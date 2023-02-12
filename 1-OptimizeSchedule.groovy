/** 
@Grapes([   
    @Grab(group='org.jacop', module='jacop', version='4.9.0'),
    @Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2'),
    @GrabConfig(systemClassLoader=true)
])
*/
/**
 * Affichage du planning des workcenters :
 Reviewing operations by workcenter :
        Workcenter # INSTALL_WC1 with a capacity of 8 per day and a setupTime of 2 per operation :
                Operations list :
                        Oper.   Status  PN      Qty     Charge  Start Date                      Due Date                        Planed_date                     Delta   Estimated charge per Day
                        2       Open    A2      5       27      2023-01-01                      2023-01-07                      2023-01-07                      0       5
                        1       Open    A1      5       52      2023-01-01                      2023-01-09                      2023-01-09                      0       7
                Total charge per day : 12       Status (<= capa of 8?): NOT POSSIBLE
        Workcenter # INSTALL_WC2 with a capacity of 12 per day and a setupTime of 3 per operation :
                Operations list :
                        Oper.   Status  PN      Qty     Charge  Start Date                      Due Date                        Planed_date                     Delta   Estimated charge per Day
                Total charge per day : 0        Status (<= capa of 12?): OK
        Workcenter # MAKE_WC1 with a capacity of 15 per day and a setupTime of 2 per operation :
                Operations list :
                        Oper.   Status  PN      Qty     Charge  Start Date                      Due Date                        Planed_date                     Delta   Estimated charge per Day
                        5       Open    M2      5       27      2023-01-01                      2023-01-05                      2023-01-05                      0       7
                        3       Open    M1      7       37      2023-01-01                      2023-01-07                      2023-01-07                      0       7
                        4       Open    M1      5       27      2023-01-01                      2023-01-09                      2023-01-09                      0       4
                Total charge per day : 18       Status (<= capa of 15?): NOT POSSIBLE
 * La planification affichee n est pas realisable. Il faut regrouper les operations et afficher une planed_date realiste afin de minimiser le retard et de l estimer de facon realiste toujours :
 */
import org.jacop.core.*
import org.jacop.floats.core.*
import org.jacop.constraints.*
import org.jacop.floats.constraints.*
import org.jacop.floats.search.*
import org.jacop.search.*
import groovy.sql.Sql
import java.text.SimpleDateFormat
import java.util.Date

def sqldb = new File("database.sqlite")
def sql = Sql.newInstance("jdbc:sqlite:"+sqldb.getAbsolutePath(), "org.sqlite.JDBC")

class CSP{
	static Store store=new Store()
	static def impose(Constraint con){
		store.impose(con)
	}
}
// Classe Variable
class Variable extends IntVar{
	public Variable(String name,int min=0,int max=Integer.MAX_VALUE){
		super(CSP.store,name,min,max)
	}
}
// Classe VariableF
class VariableF extends FloatVar{
	public VariableF(String name,double min=0,double max=Double.MAX_VALUE){
		super(CSP.store,name,min,max)
	}
}

def impose(Constraint con){CSP.impose(con)}

def pattern = "yyyy-MM-dd"
def string2Date = {d->new SimpleDateFormat(pattern).parse(d)}
def date_sub ={f,d->string2Date(f)-string2Date(d)}

def startHorizonDate = "2023-01-01"
def endHorizonDate = "2023-01-30"
def days =  date_sub(endHorizonDate,startHorizonDate)

def parts = sql.dataSet("PART")
def pns = parts.rows()*.part_number


/**
 * Creation du planning de demandes
 */
def demands = pns.collect{p->(0..days).collect{0}}
def operations = sql.dataSet("OPERATION")
operations.rows().each{r->
    pns.eachWithIndex{p,i->
        if(r.part_number == p){
            demands[i][date_sub(r.due_date,startHorizonDate)]+=r.quantity
        }
    }
}

def wks = sql.dataSet("WORKCENTER").rows()

// exploit des plans pour les liens parents enfants et le leadtime
def conditionToStart = pns.collect{prime->pns.collect{p->(0..days).collect{0}}}
def leadtime = [:]
sql.dataSet("PLAN").rows().each{
    leadtime[it.part_number]=it.leadtime
    if(it.component_part_number!=null){
        demands[pns.findIndexOf(v->v==it.part_number)].eachWithIndex{dem,d->
            if(dem>0){
                conditionToStart[pns.findIndexOf(v->v==it.part_number)][pns.findIndexOf(v->v==it.component_part_number)][d-1]=it.component_part_quantity*dem
            }        
        }
    }
}


pns.eachWithIndex{p,i->
    (1..days).each{d->
        demands[i][d]+=demands[i][d-1]
    }
    pns.eachWithIndex{p2,i2->
        (1..days).each{d->
            conditionToStart[i][i2][d]+=conditionToStart[i][i2][d-1]
        }
    }
}

def planningByWorkCenter = wks.collect{w->
    pns.collect{p->(0..days).collect{d->new FloatVar(CSP.store,"${w.workcenter} ToProduce ${p}_${d}", 0d,100d)}}
}
def planningInTimeByWorkCenter = wks.collect{w->
    pns.collect{p->(0..days).collect{d->new FloatVar(CSP.store,"${w.workcenter}_Charge_${p}_${d}", 0d,100d)}}
}
def prodByWorkCenter = wks.collect{w->
    pns.collect{p->(0..days).collect{d->new FloatVar(CSP.store,"${w.workcenter}_Produced_${p}_${d}", 0d,100d)}}//
}

wks.eachWithIndex{w,i->
    // impose une charge a 0 pour tous les PN imcompatible avec le WK
    if(w.workcenter[0]=='I'){
        pns.eachWithIndex{p,j->
            if(p[0]=='M'){
                planningByWorkCenter[i][j].each{
                    impose(new PeqC(it,0d)) // impose une charge a 0 pour tous les PN imcompatible avec le WK
                }
            }
        }
    }
    else{
        pns.eachWithIndex{p,j->
            if(p[0]=='A'){
                planningByWorkCenter[i][j].each{
                    impose(new PeqC(it,0d)) // impose une charge a 0 pour tous les PN imcompatible avec le WK
                }
            }
        }
    }

    //impose les liens parents enfants : Bug pour le moment
    pns.eachWithIndex{p,j->
        pns.eachWithIndex{p2,j2->
            (0..days).each{d->
                if(conditionToStart[j][j2][d]>0){
                    // si prodByWorkCenter[i][j2][d] < conditionToStart[j][j2][d] alors planningByWorkCenter[i][j][0..d] = 0
                    def cond1 = new PltC(prodByWorkCenter[i][j2][d],conditionToStart[j][j2][d] as double)
                    (0..d).each{d2->
                        def cond2 =  new PeqC(planningByWorkCenter[i][j][d2],0d)
                        //impose(new IfThen(cond1,cond2))
                    }
                }
            }
        }
    }

    //multiply par le leadtime
    pns.eachWithIndex{p,j->
        (0..days).each{d->
            impose(new PmulCeqR(planningByWorkCenter[i][j][d], leadtime[p], planningInTimeByWorkCenter[i][j][d]))
        }
    }
    //impose que chaque wk n excede pas sa capacity par jour
    def chargeByDayByPn = []
    (0..days).each{d->
        chargeByDayByPn[d]=[]
        pns.eachWithIndex{p,j->
            chargeByDayByPn[d].add(planningInTimeByWorkCenter[i][j][d])
        }
        def sum = new FloatVar(CSP.store,"${w.workcenter}_Charge_total_${d}", 0d,100d)//
        impose(new SumFloat(chargeByDayByPn[d] as FloatVar[],"==",sum))
        impose(new PlteqC(sum,w.capacity as double))
    }
    //store la somme de pn produit par pn et par jour
    pns.eachWithIndex{p,j->
        (0..days).each{d->
            def toSum = []
            (0..d).each{d2->
                toSum.add(planningByWorkCenter[i][j][d2])
            }
            impose(new SumFloat(toSum as FloatVar[],"==",prodByWorkCenter[i][j][d]))
        }
    }
}

def prodByPn = pns.collect{p->(0..days).collect{d->new FloatVar(CSP.store,"${p}_Produced_${d}",0d,100d)}}//
pns.eachWithIndex{p,j->
    (0..days).each{d->
        def toSum = []
        wks.eachWithIndex{w,i->
                toSum.add(prodByWorkCenter[i][j][d])
        }
        impose(new SumFloat(toSum as FloatVar[],"==",prodByPn[j][d]))
    }
}

def prodMinusDemandByPn = pns.collect{p->(0..days).collect{d->new FloatVar(CSP.store,"${p}_ProducedMinusDemand_${d}",0d,100d)}}// impose aucun retard
pns.eachWithIndex{p,j->
    (0..days).each{d->
        impose(new PminusCeqR(prodByPn[j][d],demands[j][d],prodMinusDemandByPn[j][d]))
    }
}

//creer et impose par pn et par un jour un etat si on est en retard ou pas 1 si on est en retard, 0 sinon
def lateByPn = pns.collect{p->(0..days).collect{d->new FloatVar(CSP.store, "${p} Late ${d}",-100d,100d)}} //negative !//
pns.eachWithIndex{p,j->
    (0..days).each{d->
        impose(new IfThenElse(new PltC(prodMinusDemandByPn[j][d],0d),new PeqQ(prodMinusDemandByPn[j][d],lateByPn[j][d]),new PeqC(lateByPn[j][d], 0d)))
    }
}
//Comptabilise les retards par pn et les totalise ensuite
def sumbOfLateByPn = pns.collect{p-> new FloatVar(CSP.store, "${p} Late",-100d,100d)}//negative//
def sumOfLate = new FloatVar(CSP.store, "sumOfLate",-100d,100d) //negative//
def cost = new FloatVar(CSP.store, "cost = -sumofLate",-100d,100d) //positive//

pns.eachWithIndex{p,j->
    impose(new SumFloat(lateByPn[j] as FloatVar[], '==', sumbOfLateByPn[j]))
}
impose(new SumFloat(sumbOfLateByPn as FloatVar[], '==', sumOfLate))
impose(new PmulCeqR(sumOfLate, -1d, cost))



def decisionVars = planningByWorkCenter.flatten() as FloatVar[]

SplitSelectFloat<FloatVar> select = new SplitSelectFloat<FloatVar>(CSP.store, decisionVars,new LargestDomainFloat<FloatVar>()); //
DepthFirstSearch<FloatVar> search = new DepthFirstSearch<FloatVar>() 
//search.getSolutionListener().searchAll(true)
search.setTimeOut(10)
search.labeling(CSP.store,select,cost)
println (search.getSolutionListener().solutionsNo() + ' solutions found')
//Optimize min = new Optimize(CSP.store, search, select, cost); 
//boolean result = min.minimize();


pns.eachWithIndex{p,j->
    println("PN number : "+p)
    print ("Time : \t\t\t\t")
    (1..days+1).each{print(it+"\t")}
    print ("\n=============================================================================================================")
    print("\nDemand : \t\t\t")
    demands[j].each{print(it+"\t")}
    print ("\n=============================================================================================================")
    wks.eachWithIndex{w,i->
        print("\n${w.workcenter}(capa ${w.capacity}) : \t\t".toString())
        (0..days).each{print(planningInTimeByWorkCenter[i][j][it].value().round(2)+"\t")}
    }
    print ("\n=============================================================================================================")
    print("\nProduit : \t\t\t".toString())
    prodByPn[j].each{
        print(it.value().round(2)+'\t')
    }
    print ("\n=============================================================================================================")
    print("\nProdMinusD : \t\t\t".toString())
    prodMinusDemandByPn[j].each{
        print(it.value().round(2)+'\t')
    }
    print ("\n=============================================================================================================")
    
    print ("\n=============================================================================================================")
    print("\nLate : \t\t\t\t".toString())
    lateByPn[j].each{
        print(it.value().round(2)+'\t')
    }
    print ("\n=============================================================================================================")
    print ("\n=============================================================================================================\n")
}
/**
planningByWorkCenter[1][0].each{
    println([it.id(),it.value().round(2)].toString())
}
 */
return 'Done'
