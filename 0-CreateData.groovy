/**
 * Script Groovy ayant pour but de poser une problematique de planification des operations en fonction du stock et de la demande et des plans.
 * 
 * Un article peut etre soit Assemble (Assembly) ou Fabrique (Manufactured) dans les deux cas pour chaque article il y a une gamme. Dans la gamme d un assembly il peut y avoir d autre assembly.
 * Les demandes client portent sur un top assembly. En faisant tourner le MRP qui croisera les demandes client avec les gammes ainsi que le stocks, d autre demandes seront crees ainsi que des operations.
 * Les operations sont liees a des workcenter ayant une capacite limite. Le MRP cree des operations commencant le premier jour de l annee 2023 par defaut, la due date est systematique fixe a la veille du besoin reel afin d anticiper les flux logistiques.
 * 
 * La planification resultante n est pas realisable. Il faut regrouper les operations et afficher une planed_date realiste afin de minimiser le retard et de l estimer de facon realiste toujours : Probleme d'optimisisation sous contrainte.
 */

// Import des librairies SQL
@Grapes([   
    @Grab(group='org.xerial', module='sqlite-jdbc', version='3.7.2'),
    @GrabConfig(systemClassLoader=true)
])
import groovy.sql.Sql
import java.text.SimpleDateFormat
import java.util.Date

/**
 * 1 ere etape, creer les tables
 */
def sqldb = new File("database.sqlite")
sqldb.delete()
def sql = Sql.newInstance("jdbc:sqlite:"+sqldb.getAbsolutePath(), "org.sqlite.JDBC")
sql.connection.autoCommit =false

print "Creating data ... "


def queries_createTables = new File("0-CreateTables.sql").text.replaceAll(/--.+/,'').split(';').collect{it.trim()} - ''
queries_createTables.each{query->
    sql.execute(query)
}
sql.commit()

println "done."
/**
 * Remplir les tables avec les donnees de base :
 */
print "Populating with Groovy : "

def parts = sql.dataSet("PART")
parts.add(part_number:'A1', part_type:'Assembly')
parts.add(part_number:'A2', part_type:'Assembly')
parts.add(part_number:'A3', part_type:'Assembly')
parts.add(part_number:'A4', part_type:'Assembly')
parts.add(part_number:'M1', part_type:'Manufactured')
parts.add(part_number:'M2', part_type:'Manufactured')
parts.commit()

/**
 * Les GAMMES :
 * A1
 * - M1 2
 * - A2 1
 * - - M2 3
 * A3
 * - M1 1
 * - A4 2
 * - - M1 1
 * - - M1 1
 */
def plans = sql.dataSet("PLAN")
plans.add(part_number:'A1',leadtime:10, component_part_number:'M1',component_part_quantity: 2)
plans.add(part_number:'A1',leadtime:10, component_part_number:'A2',component_part_quantity: 1)
plans.add(part_number:'A2',leadtime:5, component_part_number:'M2',component_part_quantity: 3)
plans.add(part_number:'A3',leadtime:5, component_part_number:'M1',component_part_quantity: 1)
plans.add(part_number:'A3',leadtime:5, component_part_number:'A4',component_part_quantity: 2)
plans.add(part_number:'A4',leadtime:10, component_part_number:'M1',component_part_quantity: 1)
plans.add(part_number:'A4',leadtime:10, component_part_number:'M2',component_part_quantity: 1)
plans.add(part_number:'M1',leadtime:5, component_part_number:'',component_part_quantity: 0)
plans.add(part_number:'M2',leadtime:5, component_part_number:'',component_part_quantity: 0)
plans.commit()

/**
 * Chaque WK dispose d une capacite limite ainsi qu un setupTime. entre chaque operation, ce setupTime sera comptabilise dans la planification.
 */
def wks = sql.dataSet("WORKCENTER")
wks.add(workcenter:'INSTALL_WC1',capacity: 8,setupTime :2)
wks.add(workcenter:'INSTALL_WC2',capacity: 12,setupTime :3)
wks.add(workcenter:'MAKE_WC1',capacity: 4,setupTime :1)
wks.commit()


def demands = sql.dataSet("DEMAND")
demands.add(part_number:'A1',quantity: 5,status:'Open',due_date: '2023-01-10')
demands.add(part_number:'M1',quantity: 5,status:'Open',due_date: '2023-01-10')
demands.add(part_number:'M1',quantity: 5,status:'Open',due_date: '2023-01-05')
demands.commit()

def wh = sql.dataSet("WAREHOUSE")
wh.add(part_number:'M1',quantity: 8)
wh.commit()

def operations = sql.dataSet("OPERATION")
println "done."


/**
 * Lancement du calcul de besoin net.
 */

println "Doing Material Requirement Plan : "

/**
 * Methode recursive pour calculer les besoins, et creer des operations a chaque fois que la quantite disponible devient negative.
 */
def ArrayList mrp(String pn,Sql sql,boolean newOperationAdded=false){
    Map dateQuantities = [:]
    
    sql.eachRow("SELECT * FROM DEMAND WHERE part_number == '$pn' AND status == 'Open'".toString()){
        if(dateQuantities[it.due_date]==null)dateQuantities[it.due_date]=[0,0]
        dateQuantities[it.due_date][0]-=it.quantity
    }
    sql.eachRow("SELECT * FROM OPERATION WHERE part_number == '$pn' AND status == 'Open'".toString()){
        if(dateQuantities[it.due_date]==null)dateQuantities[it.due_date]=[0,0]
        dateQuantities[it.due_date][0]+=it.quantity
    }
    def tmp = 0
    sql.eachRow("SELECT * FROM WAREHOUSE WHERE part_number == '$pn'".toString()){ r->
        tmp+=r.quantity       
    }
    
    dateQuantities = dateQuantities.sort()
    for(int i = 0; i<dateQuantities.keySet().size(); i++){
        def d = dateQuantities.keySet()[i]
        def pattern = "yyyy-MM-dd"
        def date = new SimpleDateFormat(pattern).parse(d)
        
        def v = dateQuantities[d]
        tmp += v[1]+v[0]
        v[1] = tmp
        if(v[1]<0){
            addOperation(sql,pn,-v[1],(date - 1).format(pattern))
            return mrp(pn,sql,true)
        }
        else{
            //println d + '  ' + v
        }
    }
    return [dateQuantities,newOperationAdded]
}

/**
 * Methode pour creer une operation et donc les demandes en component part en fonction des gammes.
 */
def addOperation(Sql sql,String pn,int qte,String ddate){
    def pattern = "yyyy-MM-dd"
    def date = new SimpleDateFormat(pattern).parse(ddate)
    def ldt = 0
    sql.eachRow("SELECT * FROM PLAN WHERE part_number='$pn'".toString()){ r->
        ldt = r.leadtime as int
        if(r.component_part_number != null && r.component_part_number != ""){
          sql.execute("INSERT INTO DEMAND(part_number,quantity,status,due_date) VALUES('${r.component_part_number}',${(r.component_part_quantity as int) * qte},'Open','${(date - 1).format(pattern)}') ".toString())
        }
    }
    sql.execute("INSERT INTO OPERATION(part_number,quantity,status,due_date,planed_date,start_date,charge,workcenter) VALUES('${pn}',${qte},'Open','${ddate}','${ddate}','2023-01-01',${(qte*ldt)},'${(pn[0]=='A'?"INSTALL_WC1":"MAKE_WC1")}') ".toString())
    sql.commit()
}

def pns = parts.rows()*.part_number 
def mrps = [:]
int i = 0
boolean redo = false
while(i<pns.size()-1){
    def res = null
    (res,redo) = mrp(pns[i],sql)
    mrps[pns[i]] = res
    if(redo) i=0
    else i++
}

/**
 * Execution du mrp et affichage du resultat final :
 * Doing Material Requirement Plan : 
        MRP results for PN # A1 : 
                Date :                  Qty :           Total qty :
                2023-01-09              5               5
                2023-01-10              -5              0
        MRP results for PN # A2 :
                Date :                  Qty :           Total qty :
                2023-01-07              5               5
                2023-01-08              -5              0
        MRP results for PN # A3 :
                Date :                  Qty :           Total qty :
        MRP results for PN # A4 :
                Date :                  Qty :           Total qty :
        MRP results for PN # M1 :
                Date :                  Qty :           Total qty :
                2023-01-05              -5              3
                2023-01-07              7               10
                2023-01-08              -10             0
                2023-01-09              5               5
                2023-01-10              -5              0
        MRP results for PN # M2 :
                Date :                  Qty :           Total qty :
    done.
 */
pns.each{
    println "\tMRP results for PN # $it : "
    println "\t\tDate :\t\t\tQty :\t\tTotal qty :"
    mrps[it].each{k,v->
        println "\t\t$k\t\t${v[0]}\t\t${v[1]}"
    }
}

println "done."

/**
 * Affichage du planning des workcenters :
 * Reviewing operations by workcenter : 
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
        Workcenter # MAKE_WC1 with a capacity of 4 per day and a setupTime of 1 per operation :
                Operations list :
                        Oper.   Status  PN      Qty     Charge  Start Date                      Due Date                        Planed_date                     Delta   Estimated charge per Day
                        3       Open    M1      7       36      2023-01-01                      2023-01-07                      2023-01-07                      0       6
                        4       Open    M1      5       26      2023-01-01                      2023-01-09                      2023-01-09                      0       4
                Total charge per day : 10       Status (<= capa of 4?): NOT POSSIBLE
                
 * La planification affichee n est pas realisable. Il faut regrouper les operations et afficher une planed_date realiste afin de minimiser le retard et de l estimer de facon realiste toujours :
 */
println "Reviewing operations by workcenter : "

def a_wks = wks.rows()
def pattern = "yyyy-MM-dd"
a_wks.each{
    def wk = it.workcenter
    def capa = it.capacity
    def setupTime = it.setupTime
    println "\tWorkcenter # $wk with a capacity of $capa per day and a setupTime of $setupTime per operation : "
    println "\t\tOperations list :"
    println "\t\t\tOper.\tStatus\tPN\tQty\tCharge\tStart Date\t\t\tDue Date \t\t\tPlaned_date\t\t\tDelta\tEstimated charge per Day"
    def total_charge_perDay = 0 
    sql.eachRow("SELECT * FROM OPERATION WHERE workcenter == '$wk' AND status == 'Open' ORDER BY due_date".toString()){
        def endDate = new SimpleDateFormat(pattern).parse(it.planed_date)
        def startDate = new SimpleDateFormat(pattern).parse(it.start_date)
        def estimated_charge_perDay =Math.ceil((it.charge+setupTime)/(endDate-startDate)) as int 
        total_charge_perDay+=estimated_charge_perDay
        println "\t\t\t${it.operation_id}\t${it.status}\t${it.part_number}\t${it.quantity}\t${it.charge+setupTime}\t${it.start_date}\t\t\t${it.due_date}\t\t\t${it.planed_date}\t\t\t${new SimpleDateFormat(pattern).parse(it.due_date)-endDate}\t${estimated_charge_perDay}"
    }
    println "\t\tTotal charge per day : ${total_charge_perDay}\tStatus (<= capa of $capa?): ${total_charge_perDay<=capa?'OK':'NOT POSSIBLE'}"
}

sql.close()