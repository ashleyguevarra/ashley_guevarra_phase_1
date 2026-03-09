# Arc42 — BrokerX Phase 1

## 1. Introduction et objectifs

Le projet BrokerX Phase 1 consiste à concevoir et implémenter une API REST pour un système bancaire destiné à des investisseurs particuliers.

Les objectifs principaux de cette phase sont :

- implémenter au moins cinq cas d’utilisation bancaires
- exposer ces fonctionnalités via une API REST sécurisée
- assurer la persistance des données avec PostgreSQL
- garantir la traçabilité des actions avec un journal d’audit
- préparer l’application pour les prochaines étapes du projet (observabilité, optimisation, API Gateway, microservices)

Les cas d’utilisation principaux implémentés sont :

- enregistrement d’un client
- approbation KYC
- ouverture d’un compte bancaire
- consultation du solde et de l’historique
- virement entre comptes avec idempotence

---

## 2. Contraintes

Le projet doit respecter les contraintes suivantes :

- utilisation de Java et Spring Boot
- persistance des données avec PostgreSQL
- exposition d’une API REST
- sécurisation minimale par Basic Auth
- documentation de l’API avec Swagger / OpenAPI
- instrumentation applicative avec Spring Actuator et Prometheus
- exécution reproductible avec Docker

Le projet doit aussi rester suffisamment structuré pour permettre une évolution future vers une architecture microservices.

---

## 3. Contexte du système

### Acteurs externes

- **Client** : utilise l’API pour gérer ses comptes et effectuer des virements
- **Administrateur / conformité** : approuve le statut KYC
- **Base de données PostgreSQL** : stocke les clients, comptes, virements et journaux
- **Prometheus / Grafana** : collectent et visualisent les métriques

### Vue de contexte simplifiée

Le client interagit avec l’API REST BrokerX.  
L’API applique les règles métier, persiste les données dans PostgreSQL et expose des métriques d’observabilité.

---

## 4. Stratégie de solution

La solution retenue repose sur une application Spring Boot structurée de manière modulaire, avec séparation entre :

- l’exposition de l’API (`api`)
- l’orchestration des cas d’utilisation (`application`)
- la logique métier (`domain`)
- les aspects techniques et la persistance (`infrastructure`)

Le système utilise :

- Spring Web pour l’API REST
- Spring Security pour l’authentification Basic
- Spring Data JPA pour l’accès aux données
- PostgreSQL comme base de données
- Flyway pour les migrations
- Spring Actuator + Micrometer + Prometheus pour les métriques
- Grafana pour les dashboards

---

## 5. Vue des blocs de construction

Les principaux blocs du système sont :

### Customer / KYC
Responsable de l’enregistrement des clients et de la gestion du statut KYC.

### Accounts
Responsable de l’ouverture des comptes, du solde et de l’historique.

### Transfers
Responsable de l’exécution des virements entre comptes.

### Ledger
Responsable de l’enregistrement des écritures comptables.

### Audit
Responsable de la traçabilité des actions critiques.

---

## 6. Vue d’exécution

Scénario typique : virement entre comptes

1. Le client envoie une requête `POST /transfers`
2. L’API valide l’authentification et les données reçues
3. Le service applicatif vérifie les comptes et le solde disponible
4. Le système vérifie l’`Idempotency-Key`
5. Le transfert est créé
6. Les soldes sont mis à jour
7. Les écritures `ledger_entries` sont créées
8. Une entrée est ajoutée dans `audit_log`
9. La réponse HTTP est retournée au client

---

## 7. Vue de déploiement

Le déploiement local du projet repose sur les éléments suivants :

- application Spring Boot
- base PostgreSQL
- Prometheus
- Grafana

L’application écoute sur le port `8081`.  
PostgreSQL est exécuté dans un conteneur Docker.  
Prometheus collecte les métriques exposées par `/actuator/prometheus`.  
Grafana permet de visualiser les Golden Signals.

---

## 8. Concepts transversaux

### Sécurité
Les endpoints métier sont protégés par Basic Auth.

### Validation
Les entrées sont validées via les annotations de validation Spring.

### Gestion des erreurs
Les erreurs sont retournées au format JSON avec un message, un code d’erreur et des détails.

### Idempotence
Les virements utilisent une `Idempotency-Key` afin d’éviter le double traitement d’une même requête.

### Audit
Les opérations critiques produisent des entrées append-only dans `audit_log`.

### Observabilité
L’application expose des métriques Prometheus et un dashboard Grafana a été mis en place pour suivre les 4 Golden Signals.

# Vue architecturale 4+1 — BrokerX Phase 1

## 1. Vue logique

La vue logique décrit les principaux éléments fonctionnels du système.

Les principaux modules sont :

- **Customers / KYC** : gestion des clients et du statut KYC
- **Accounts** : gestion des comptes bancaires
- **Transfers** : gestion des virements
- **Ledger** : historique des écritures comptables
- **Audit** : journalisation des actions critiques

Cette décomposition correspond aux principaux concepts métier du système BrokerX.

---

## 2. Vue de développement

La vue de développement décrit l’organisation du code source.

Le projet est structuré en plusieurs packages principaux :

- `api`
- `application`
- `domain`
- `infrastructure`
- `customer`
- `account`
- `transfer`
- `ledger`
- `audit`
- `config`

Cette structure permet de séparer la logique métier, l’orchestration applicative, l’exposition REST et les aspects techniques.

---

## 3. Vue des processus

La vue des processus décrit le comportement dynamique du système pendant l’exécution.

Exemple de scénario dynamique : **virement entre comptes**

1. Le client appelle `POST /transfers`
2. Le contrôleur REST reçoit la requête
3. Le service applicatif valide les données
4. Le système vérifie l’idempotence
5. Le système vérifie le solde du compte source
6. Le transfert est persisté
7. Les soldes sont mis à jour
8. Les écritures ledger sont créées
9. Une entrée d’audit est ajoutée
10. La réponse est retournée au client

---

## 4. Vue physique (déploiement)

Le système est déployé localement avec les composants suivants :

- **Application Spring Boot** sur le port `8081`
- **PostgreSQL** pour la persistance
- **Prometheus** pour la collecte des métriques
- **Grafana** pour la visualisation des métriques

Cette vue montre les principaux composants déployés et leurs interactions.

---

## 5. Scénarios

Les scénarios servent à illustrer l’architecture à travers les cas d’utilisation principaux :

- enregistrement d’un client
- approbation KYC
- ouverture d’un compte
- consultation du solde
- consultation du ledger
- virement entre comptes avec idempotence

Ces scénarios permettent de valider la cohérence entre les vues logique, développement, processus et déploiement.