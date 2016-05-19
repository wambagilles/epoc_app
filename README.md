

#Cadre

L'objectif est de faire du placement spatio-temporel d'applications sur des serveurs en optimisant une fontion de bénéfice.
Les applications sont réparties en deux groupes, les web service à bénéfice variable et les HPC à bénéfice acquis à terme de travail.
Cet objectif est cepedant contrarié par des contraintes de différents types : limitations en ressources ou en énergie. En particulier, le centre est alimenté en énergie grise, son alimentation énergétique varie avec le temps.


##intervalles

Nous considèrons une durée de n (typiquement 24) intervalles (typiquement d'une heure).
Durant cette durée totale, l'arrivée de nouvelles applications, et/ou d'arrêt de certaines applications, peuvent invalider la plannification que nous aurons établie et donc demander une nouvelle plannification.


##Centre physique

Le centre est constitué de plusieurs serveurs, chacun pouvant à priori héberger un nombre infini d'application. Chaque serveur a des capacités propres en ressources (CPU, RAM) qui vont limiter en pratique le nombre d'applications présentes sur ce serveur.

<a name="centreenegie"></a>Outre les ressources des serveurs, le centre possède une notion de capacité énergétique totale. À un intervalle donné, l'utilisation en énergie de tous les serveurs du centre ne doit pas dépasser une valeur donnée.

Nous considérons la ressource CPU comme corrélée linéairement avec la consommation énergétique. Ces deux notions sont considérées équivalentes pour la modélisation du problème. Cette équivalence n'a bien sûr de sens que si les architecture physiques des serveurs sont les même, on parle d'**homogénéité des serveurs** du centre.
Étant donné que les intervalles considérés sont tous de même longueur, la notion de puissance est aussi équivalente à une multiplication près à la notion d'énergie. Nous utilisons par la suite uniquement la notion de puisance (en W).

Sur ces serveurs sont placées des applications confiées par les clients.


##Applications

Nous préférons dans cette partie le terme "application" au terme "tâche", car bien que souvent utilisé dans la litérature ce dernier appartient déjà à la sémantique des contraintes de placement de type *cumulatif* que nous utilisons.

###Applications Web

Une application web (ou Active) fonctionne de manière continue et met à disposition plusieurs modes de fonctionnement, chacun consommant une valeur différente d'énergie mais assurant un bénéfice différent.

Chaque application web ne sera arrétée qu'au bon vouloir de son possesseur. Il faut donc prévoir, pour chaque application et à chaque intervalle, le mode de fonctionnement de cette application et lui réserver un serveur d'exécution, même si dans la pratique cette application pourrait être arrétée.

###Applications HPC

Une application HPC (ou Bench) propose un bénéfice, une durée (en intervalles), une consommation énergétique et une échéance. Le bénéfice n'est assuré que si cette application a bien été exécutée autant de temps que le demande sa durée, avant son échéance.

Lorsque cette application est exécutée, elle consomme des ressources (cpu, ram) du serveur qui l'héberge. Lorsque cette application n'est pas exécutée, elle est mise en veille sur un disque dur considéré comme de taille infinie, sa consommation est nulle.

##Limitations énergétiques

Comme indiqué [plus haut](#centreenegie), le centre est limité en consommation totale.
À chaque intervalle correspond une puissance maximale, et donc une énergie si on multiplie par la durée constante des intervalles.

De plus, sur chaque intervalle chaque serveur est limité en puissance. Chaque application exécutée sur un serveur consomme une quantité non nulle de puissance, selon le type d'application :

 - Les applications de type Web ont une valeur de puissance dépendant de leur mode de fonctionnement. 
 - Les applications de type HPC ont une puissance fixée si exécutée sur un serveur, nulle sinon.
 
Ces différents 

##Situation initiale

Le centre est dans une situation initiale dans laquelle certaines applications sont déjà exécutées.

#Travaux existants

##Pika

L'article [Opportunistic Scheduling in Clouds Partially Powered by Green Energy]( https://hal.inria.fr/hal-01205911v1 ) propose un framework d'aide à la décision évènementiel. Ce framework déplace et exécute des applications afin d'améliorer le taux d'utilisation d'énergie grise dans un centre.

De manière périodique, ce framework tente d'approcher la consommation du centre de la disponibilité en "énergie grise", variant par exemple avec l'ensoleillement des panneaux solaires.
 - Si la consommation actuelle du centre est supérieur à cette quantité d'énergie grise, le framework tente de regrouper les VM sur un nombre réduit de serveurs.
 - Si cette consommation est inférieure, le framework allume des serveurs et avance l'exécution de taches de type HPC.

#Implémentation

L'objectif est d'une part de permettre de modéliser un tel centre, d'autre part d'obtenir une plannification de ce centre sur un nombre d'intervalles donné. La modélisation du centre étant triviale, la recherche d'une plannification est le cœur de ce problème. 

L'implémentation est faite en Java, plus particulièrement avec [le solveur de contraintes Choco](https://github.com/chocoteam/choco-solver). En effet, une fois le centre actuel défini, notre implémentation utilise Choco pour définir un model formel du problème de plannification, et parcourir ce problème.

Les notion de variable ou de contrainte font par la suite référence au sens de Choco.

## Indices des éléments

Étant donné que tout est mathématique dans Choco, il faut tout d'abord faire correspondre les éléments du centre avec des nombres. La première étape de modélisation dans Choco consiste à indexer chaque serveur, chaque application, chaque intervalle.

## Placement spatio-temporelle.

Les applications web doivent êre exécutées à chaque intervalle. Le problème de plannification est donc tout d'abord défini par une matrice de positions P, où Pij est le serveur qui exécute l'application j à l'intervalle i.

Si l'application j est de type web, ce serveur existe forcément, si elle est de type HPC, cette application peut être mise en veille. Dans ce cas, l'indice du serveur d'exécution Pij et mis à *-1*

## Mode d'éxécution des applications web

Pour chaque application web, outre le serveur d'éxécution il faut choisir le mode d'excution.

## Bénéfice des applications HPC

variable booléenne d'exécution des HPC


## Consommation des ressources sur un intervalle

Bin-packing par intervalle pour les ressources statiques

Fonction somme par intervalle pour chaque serveur pour la puissance

## Limitation totale énergétique

limitation par intervalle

besoin d'utiliser une contrainte de type cumulatif pour accélérer la recherche

## Planification des HPC hors-plan

Si une application de type HPC est trop longue pour tenir sur le nombre d'intervalles, le solveur va considérer impossible la satisfaction de cette application, qui sera donc tout simplement écartée du placement au profit d'applications plus rapides. Cette non-planification peut induire une impossibilité logique d'exécution de cette application en respectant son échéance par la suite.

### Problématique

Par exemple, supposons un placement sur 2 intervalles, et une application HPC de durée 3 et d'échéance 3. Cette application ne pourra pas être planifiée sur trois intervalles dans la plage des deux intervalles considérés. Cependant en exécutant cette application sur les deux intervalles considérés et sur l'intervalle suivant, elle serait terminée à la prochaine planification et apporterait donc son bénéfice.

Il convient donc d'executer une application HPC si possible, même lorsque le bénéfice de celle-ci n'est pas assuré de par sa durée supérieure à celle de planification. Pour ce faire il faut dissocier trois cas possible.

Dans un premier cas, nous considérons une application *non terminable* si sa durée est supérieure à son échéance. Quelle que soit la planification faite, cette application ne sera pas terminée à son échéance aussi cette application est tout simplement enlevée de la planification. Reste donc les applications de durée inférieure à l'échéance.

Dans le deuxième cas, une application dont l'échéance est inférieure à la durée de la plannification est dite *dans le plan*. Une telle application doit forcément être exécutée avant la durée de plannification. Son bénéfice n'est donc récolté que si cette application a bien été plannifiée un nombre de fois suffisant avant son échéance.

Dans le dernier cas, une application dont l'échéance est supérieure à la durée de plannification, mais évidemment supérieure à sa durée, est dite *hors-plan*. Il est impossible d'affecter un bénéfice à une telle application car celui-ci ne pourrait être récolté que dans une plannification ultérieure, si les conditions le permettent. Il faut cependant prioriser les plannifications permettant de terminer une application.

C'est cette problématique de placement des applications hors-plan que nous considérons ici.

### Résolution

À faire.

## Centre non homogène

Dans le cas où le centre n'est plus homogène en CPU/énergie, c'est à dire que différents sereurs ont différentes capacité CPU ou différntes capacité énergétiques, le modèle proposé précédemment n'est plus suffisant.

Il faut alors considérer deux ressources distinctes : la resource CPU statique et la ressource énergétique dynamique. 

#Performances

