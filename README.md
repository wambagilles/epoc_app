

#Cadre

l'objectif est de faire du scheduling d'applications avec différentes dimensions


##Intervales

On prévoit un scheduling sur un enemble de n (typiquement 24) intervalles (typiquement d'une heure).
Ce scheduling devra être remanié en cas d'arrivée de nouvelle application, et/ou d'arrêt de certaines applications.


##Centre physique

Le centre est constitué de plusieurs serveurs. Chaque serveur a des capacités propres en ressources (CPU, RAM). La ressource CPU étant corrélée linéairement avec la consommation énergétique, qui est le sujet de ce travail, ces deux notions sont considérées non distinctes.


##Applications

###Applications Web

Une application web fonctionne de manière continueet met à disposition plusieurs modes de fonctionnement, chacun consommant une valeur différente d'énergie mais assurant un bénéfice différent.
Chaque application web peut être arrétée par son possesseur son bon vouloir. Il faut donc prévoir pour chaque application et à chaque intervale, le mode de fonctionnement de cette application ainsi que son serveur d'exécution.

###Applications HPC

Une aplication HPC propose un bénéfice, une durée (en intervales), une consommation énergétique et une échéance. Le bénéfice n'est assuré que si cette application a bien été exécutée autant de fois que le demande sa durée, avant son échéance.
Lorsque cette application est schédulée, elle consomme des ressources (cpu, ram) du serveur qui l'héberge. Lorsque cette application n'est pas schédulée, elle est mise en veille sur un disque dur de taille infinie, sa consommation est nulle.

##Limitations énergétiques

##Situation initiale

Le centre est dans une situation initiale dans laquelle certaines applications sont déjà exécutées.
