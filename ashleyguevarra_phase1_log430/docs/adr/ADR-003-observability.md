# ADR-003 — Observabilité (logs, métriques et monitoring)

## Statut
Accepté

## Date
2026-03-08

## Contexte

Dans un système bancaire exposé via une API REST, il est important de pouvoir observer le comportement du système afin de détecter les erreurs, mesurer les performances et comprendre l’utilisation du service.

Le projet BrokerX doit permettre de mesurer certains indicateurs importants lors des tests de charge, notamment les **4 Golden Signals** :

- la latence
- le trafic
- les erreurs
- la saturation

Pour cela, il est nécessaire de mettre en place des mécanismes de monitoring et de collecte de métriques.

## Décision

J’ai choisi d’implémenter une stratégie d’observabilité basée sur :

- **logs applicatifs structurés**
- **exposition de métriques via Spring Boot Actuator**
- **collecte des métriques avec Prometheus**
- **visualisation via Grafana**

Les métriques sont exposées par l’application via un endpoint :

/actuator/prometheus

Prometheus collecte ces métriques et Grafana permet de visualiser les données à l’aide de dashboards.

## 4 Golden Signals

Les métriques suivantes sont surveillées :

### Latence
Temps de réponse des endpoints REST, notamment :

- `GET /accounts/{id}/balance`
- `POST /transfers`

### Trafic
Nombre de requêtes par seconde (RPS) reçues par l’API.

### Erreurs
Taux de réponses HTTP en erreur, notamment :

- erreurs `4xx`
- erreurs `5xx`

### Saturation
Utilisation des ressources du système, par exemple :

- CPU
- mémoire
- pool de connexions à la base de données

## Conséquences

### Avantages

- meilleure visibilité sur le comportement du système
- possibilité de détecter rapidement les erreurs
- permet d’analyser les performances pendant les tests de charge
- facilite l’identification des points de saturation

### Inconvénients

- ajoute des composants supplémentaires à l’architecture
- nécessite la configuration de Prometheus et Grafana
- augmente légèrement la complexité de l’environnement d’exécution

## Alternatives considérées

### Logs simples sans monitoring

Une solution plus simple aurait été d’utiliser uniquement des logs applicatifs.

Cette option a été rejetée car elle ne permet pas de mesurer facilement les performances ou de produire des dashboards.

### Outils propriétaires de monitoring

Des solutions commerciales existent pour le monitoring des applications.

Elles n’ont pas été retenues car l’objectif du projet est d’utiliser des outils open source et facilement intégrables dans un environnement Docker.