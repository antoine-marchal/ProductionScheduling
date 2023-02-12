# MRP Groovy Script

Ce script Groovy a pour but de planifier les opérations en fonction du stock, de la demande et des plans. Il utilise un algorithme MRP (Material Requirements Planning) pour croiser les demandes client avec les gammes des articles et les stocks disponibles. Cela peut entraîner la création de nouvelles demandes et d'opérations liées à des workcenters ayant une capacité limitée.

## Fonctionnement

- Les articles peuvent être soit Assemblés (Assembly) ou Fabriqués (Manufactured) et chaque article appartient à une gamme. Il peut y avoir d'autres assemblies dans la gamme d'un assembly.
- Les demandes client portent sur un top assembly.
- Les opérations sont liées à des workcenters ayant une capacité limitée.
- Le MRP crée les opérations à partir du premier jour de l'année 2023 par défaut, la date d'échéance étant fixée systématiquement la veille du besoin réel pour anticiper les flux logistiques.
- La planification résultante n'est pas réalisable, il est nécessaire de regrouper les opérations et d'afficher une date planifiée réaliste pour minimiser les retards et les estimer de manière réaliste. Ce problème est un problème d'optimisation sous contrainte.

## Utilisation

1. Téléchargez le script MRP Groovy.
2. Exécutez les scripts en utilisant la librairie GroovyScriptExecutor avec la commande suivante :
```
java -server -Xss15500k -Xmx1G -jar .\GroovyScriptExecutor.jar .\1-OptimizeSchedule.groovy
```
3. Modifier directement le script pour entrer les données nécessaires, telles que les demandes client, les stocks disponibles, les gammes d'articles, les workcenters et les capacités.
4. Le script 1-OptimizeSchedule produira la planification des opérations, regroupées pour minimiser les retards et une date planifiée réaliste.

## Note

Veillez à disposer des connaissances nécessaires en matière de MRP et de programmation Groovy avant de tenter d'utiliser ce script. Si vous rencontrez des problèmes ou avez besoin d'aide, n'hésitez pas à consulter la documentation ou à demander de l'aide en ligne.

### Fichier 0-CreateData

Un article peut etre soit Assemble (Assembly) ou Fabrique (Manufactured) dans les deux cas pour chaque article il y a une gamme. Dans la gamme d un assembly il peut y avoir d autre assembly.
Les demandes client portent sur un top assembly. En faisant tourner le MRP qui croisera les demandes client avec les gammes ainsi que le stocks, d autre demandes seront crees ainsi que des operations.
Les operations sont liees a des workcenter ayant une capacite limite. Le MRP cree des operations commencant le premier jour de l annee 2023 par defaut, la due date est systematique fixe a la veille du besoin reel afin d anticiper les flux logistiques.
La planification resultante n est pas realisable. Il faut regrouper les operations et afficher une planed_date realiste afin de minimiser le retard et de l estimer de facon realiste toujours : Probleme d'optimisisation sous contrainte.
Le script se sert d'un fichier SQL pour construire les tables.
Après remplissage des tables, le script propose un algorithme simple de MRP pour créer les opérations nécessaires :
```
Doing Material Requirement Plan : 
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
                2023-01-05              5               5
                2023-01-06              -5              0
				
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
```

### Fichier 1-OptimizeSchedule

Sur l'exemple construit. On se propose de créer un modèle d'optimisation sous contraintes permettant de, a partir des operations planifiees ainsi que des gammes, recreer un scheduling permettant de réduire le retard et d'occuper au mieux les workcenters en prenant en compte leur limite de capacité.
Pour l'heure les liens parents enfants de la gamme ne fonctionnent pas et le calcul est trop long lorseque l on permet a librairie de chercher des solutions permettant du retard dans le scheduling.
En forcant donc un retard souhaite egal à zéro nous obtenons le résultat suivant :

```

1 solutions found
PN number : A1
Time :                          1       2       3       4       5       6       7       8       9       10      11      12      13      14      15      16      17      18      19      20      21      22      23      24      25      26      27      28      29      30
=============================================================================================================
Demand :                        0       0       0       0       0       0       0       0       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5
=============================================================================================================
INSTALL_WC1(capa 8) :           2.0     2.0     2.0     2.0     2.0     4.0     4.0     2.5     4.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
INSTALL_WC2(capa 12) :          3.0     2.53    2.91    2.25    3.0     2.81    3.0     3.0     3.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
MAKE_WC1(capa 15) :             0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
Produit :                       0.5     0.95    1.44    1.87    2.37    3.05    3.75    4.3     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0
=============================================================================================================
ProdMinusD :                    0.5     0.95    1.44    1.87    2.37    3.05    3.75    4.3     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
Late :                          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
PN number : A2
Time :                          1       2       3       4       5       6       7       8       9       10      11      12      13      14      15      16      17      18      19      20      21      22      23      24      25      26      27      28      29      30
=============================================================================================================
Demand :                        0       0       0       0       0       0       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5
=============================================================================================================
INSTALL_WC1(capa 8) :           1.36    2.0     2.0     2.0     2.0     2.0     2.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
INSTALL_WC2(capa 12) :          1.03    1.42    1.5     1.5     1.5     1.75    2.94    0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
MAKE_WC1(capa 15) :             0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
Produit :                       0.48    1.16    1.86    2.56    3.26    4.01    5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0
=============================================================================================================
ProdMinusD :                    0.48    1.16    1.86    2.56    3.26    4.01    0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
Late :                          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
PN number : A3
Time :                          1       2       3       4       5       6       7       8       9       10      11      12      13      14      15      16      17      18      19      20      21      22      23      24      25      26      27      28      29      30
=============================================================================================================
Demand :                        0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0
=============================================================================================================
INSTALL_WC1(capa 8) :           0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
INSTALL_WC2(capa 12) :          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
MAKE_WC1(capa 15) :             0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
Produit :                       0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
ProdMinusD :                    0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
Late :                          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
PN number : A4
Time :                          1       2       3       4       5       6       7       8       9       10      11      12      13      14      15      16      17      18      19      20      21      22      23      24      25      26      27      28      29      30
=============================================================================================================
Demand :                        0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0       0
=============================================================================================================
INSTALL_WC1(capa 8) :           0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
INSTALL_WC2(capa 12) :          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
MAKE_WC1(capa 15) :             0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
Produit :                       0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
ProdMinusD :                    0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
Late :                          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
PN number : M1
Time :                          1       2       3       4       5       6       7       8       9       10      11      12      13      14      15      16      17      18      19      20      21      22      23      24      25      26      27      28      29      30
=============================================================================================================
Demand :                        0       0       0       0       0       0       7       7       12      12      12      12      12      12      12      12      12      12      12      12      12      12      12      12      12      12      12      12      12      12
=============================================================================================================
INSTALL_WC1(capa 8) :           0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
INSTALL_WC2(capa 12) :          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
MAKE_WC1(capa 15) :             1.87    7.5     7.5     5.62    7.5     7.5     7.5     7.5     7.5     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
Produit :                       0.37    1.87    3.37    4.5     6.0     7.5     9.0     10.5    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0    12.0
=============================================================================================================
ProdMinusD :                    0.37    1.87    3.37    4.5     6.0     7.5     2.0     3.5     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
Late :                          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
PN number : M2
Time :                          1       2       3       4       5       6       7       8       9       10      11      12      13      14      15      16      17      18      19      20      21      22      23      24      25      26      27      28      29      30
=============================================================================================================
Demand :                        0       0       0       0       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5       5
=============================================================================================================
INSTALL_WC1(capa 8) :           0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
INSTALL_WC2(capa 12) :          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
MAKE_WC1(capa 15) :             3.59    2.5     5.0     6.41    7.5     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
Produit :                       0.72    1.22    2.22    3.5     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0     5.0
=============================================================================================================
ProdMinusD :                    0.72    1.22    2.22    3.5     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
Late :                          0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0     0.0
=============================================================================================================
=============================================================================================================
```