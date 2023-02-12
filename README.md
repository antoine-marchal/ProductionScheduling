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
2. Exécutez-le en utilisant un interpréteur Groovy ou un environnement de développement Groovy.
3. Modifier directement le script pour entrer les données nécessaires, telles que les demandes client, les stocks disponibles, les gammes d'articles, les workcenters et les capacités.
4. Le script produira la planification des opérations, regroupées pour minimiser les retards et une date planifiée réaliste.

## Note

Veillez à disposer des connaissances nécessaires en matière de MRP et de programmation Groovy avant de tenter d'utiliser ce script. Si vous rencontrez des problèmes ou avez besoin d'aide, n'hésitez pas à consulter la documentation ou à demander de l'aide en ligne.