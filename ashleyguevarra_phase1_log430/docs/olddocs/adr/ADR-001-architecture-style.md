# ADR-001 — Choix du style architectural

## Statut
Accepté

## Date
2026-03-08

## Contexte

Dans le cadre du projet BrokerX (Phase 1), je dois implémenter plusieurs cas d’utilisation d’un système bancaire accessible via une API REST. Les fonctionnalités principales incluent :

- l’enregistrement d’un client
- la validation KYC
- l’ouverture d’un compte
- la consultation du solde et de l’historique
- le virement entre comptes avec idempotence

Le projet doit rester simple pour cette première phase, mais aussi permettre une évolution vers une architecture plus avancée (observabilité, microservices, API Gateway, etc.).

Je dois donc choisir une structure de projet qui sépare clairement les responsabilités et qui facilite la maintenance du code.

## Décision

J’ai choisi d’utiliser une architecture en couches inspirée de l’architecture hexagonale.

Le projet est organisé autour des packages suivants :

- `api`
- `application`
- `domain`
- `infrastructure`

Chaque couche a un rôle spécifique.

### api

Cette couche contient les contrôleurs REST.  
Elle expose les endpoints HTTP et gère les requêtes et réponses de l’API.

### application

Cette couche contient les services applicatifs.  
Elle orchestre les cas d’utilisation en appelant les entités du domaine et les composants de persistance.

### domain

Cette couche contient les concepts métier du système (par exemple les entités comme `Customer`, `Account`, `Transfer`, etc.).  
Elle représente la logique métier principale.

### infrastructure

Cette couche contient les aspects techniques comme la persistance avec JPA ou l’accès à la base de données.

## Conséquences

### Avantages

Cette structure me permet :

- de séparer la logique métier des détails techniques
- de garder un code plus lisible
- de faciliter les tests
- de préparer le projet pour les prochaines phases (observabilité, microservices, etc.)

Elle rend aussi le projet plus facile à faire évoluer.

### Inconvénients

Cette organisation demande un peu plus de structure au départ et peut sembler plus complexe qu’un projet simple avec tout dans les contrôleurs.

Cependant, pour un projet d’architecture logicielle, cette séparation est utile.

## Alternatives considérées

### Architecture monolithique simple

J’aurais pu mettre toute la logique directement dans les contrôleurs ou dans quelques services.

Je n’ai pas choisi cette option car cela aurait créé un couplage trop fort entre l’API, la logique métier et la persistance.

### Architecture hexagonale complète

Une architecture hexagonale complète avec ports et adaptateurs aurait aussi été possible.

Je n’ai pas choisi cette approche complète car elle aurait ajouté beaucoup de complexité pour cette première phase du projet.

La solution retenue reste inspirée de ces principes, mais de manière plus simple.

## Structure actuelle du projet

La structure principale du projet est la suivante :

src/main/java/com/ashleyguevarra/phase1
├── api
├── application
├── config
├── domain
├── infrastructure
├── customer
├── account
├── transfer
├── ledger
└── audit

Cette organisation permet de garder une séparation claire entre la logique métier et les aspects techniques.

## Impact sur les phases suivantes

Cette architecture facilite les prochaines étapes du projet, notamment :

- l’ajout de monitoring et d’observabilité
- l’ajout d’un cache
- l’utilisation d’un load balancer
- l’introduction d’une API Gateway
- la séparation éventuelle en microservices.