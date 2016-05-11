

#Cadre

l'objectif est de faire du scheduling d'applications avec différentes dimensions


##Intervales

On prévoit une plannification sur une durée de n (typiquement 24) intervalles (typiquement d'une heure).
Cette plannification devra être remaniée en cas d'arrivée de nouvelle application, et/ou d'arrêt de certaines applications.


##Centre physique

Le centre est constitué de plusieurs serveurs. Chaque serveur a des capacités propres en ressources (CPU, RAM). La ressource CPU étant corrélée linéairement avec la consommation énergétique, qui est le sujet de ce travail, ces deux notions sont considérées non distinctes.


##Applications

Nous préférons dans cette partie le terme "application" au terme "tâche", car bien que souvent utilisé dans la litérature ce dernier est déjà utilisé dans les contraintes de placement de type cumulatif que nous utilisons.

###Applications Web

Une application web fonctionne de manière continue et met à disposition plusieurs modes de fonctionnement, chacun consommant une valeur différente d'énergie mais assurant un bénéfice différent.
Chaque application web peut être arrétée par son possesseur son bon vouloir. Il faut donc prévoir pour chaque application et à chaque intervale, le mode de fonctionnement de cette application ainsi que son serveur d'exécution.

###Applications HPC

Une aplication HPC propose un bénéfice, une durée (en intervales), une consommation énergétique et une échéance. Le bénéfice n'est assuré que si cette application a bien été exécutée autant de fois que le demande sa durée, avant son échéance.
Lorsque cette application est schédulée, elle consomme des ressources (cpu, ram) du serveur qui l'héberge. Lorsque cette application n'est pas schédulée, elle est mise en veille sur un disque dur de taille infinie, sa consommation est nulle.

##Limitations énergétiques

##Situation initiale

Le centre est dans une situation initiale dans laquelle certaines applications sont déjà exécutées.

#Travaux existants

##Pika

#Implémentation

## Planification des HPC hors-plan

Si une application de type HPC est trop longue pour tenir sur le nombre d'intervalles, le solveur va considérer impossible la satisfaction de cette application, qui sera donc tout simplement écartée du placement au profit d'applications plus rapides. Cette non-planification peut induire une impossibilité logique d'exécution de cette application en respectant son échéance par la suite.

### Problématique

Par exemple, supposons un placement sur 2 intervalles, et une application HPC de durée 3 et d'échéance 3. Cette application ne pourra pas être planifiée sur trois intervalles dans la plage des deux intervalles considérés. Cependant en exécutant cette application sur les deux intervalles considérés et sur l'intervalle suivant, elle serait terminé à la prochaine planification et apporterait donc son bénéfice.

Il convient donc d'executer une application HPC si possible, même lorsque le bénéfice de celle-ci n'est pas assuré de par sa durée supérieure à celle de planification. Pour ce faire il faut dissocier trois cas possible.

Dans un premier cas, nous considérons une application *non terminable* si sa durée est supérieure à son échéance. Quelle que soit la planification faite, cette application ne sera pas terminée à son échéance aussi cette application est tout simplement enlevée de la planification. Reste donc les applications de durée inférieure à l'échéance.

Dans le deuxième cas, une application dont l'échéance est inférieure à la durée de la plannification est dite *dans le plan*. Une telle application doit forcément être exécutée avant la durée de plannification. Son bénéfice n'est donc récolté que si cette application a bien été plannifiée un nombre de fois suffisant avant son échéance.

Dans le dernier cas, une application dont l'échéance est supérieure à la durée de plannification, mais évidemment supérieure à sa durée, est dite *hors-plan*. Il est impossible d'affecter un bénéfice à une telle application car celui-ci ne pourrait être récolté que dans une plannification ultérieure, si les conditions le permettent. Il faut cependant prioriser les plannifications permettant de terminer une application.

C'est cette problématique de placement des applications hors-plan que nous considérons ici.

### Résolution

À faire.

#Performances

