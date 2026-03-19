# CanBankX Banking API — LOG430 Phase 1

## Description

Ce projet implémente une première version du système bancaire **CanBankX** sous forme d'une **API REST** développée avec **Java et Spring Boot**.

L'objectif du projet est de concevoir une architecture logicielle capable de supporter les fonctionnalités bancaires de base tout en intégrant des mécanismes d'observabilité, de performance et de traçabilité.

Le système permet notamment :

- l'enregistrement d'un client
- la validation KYC
- l'ouverture d'un compte bancaire
- la consultation du solde et de l'historique
- les virements entre comptes avec idempotence

L'application est instrumentée avec **Prometheus** et **Grafana** afin d'observer les **4 Golden Signals** (latence, trafic, erreurs et saturation).

---

# Technologies utilisées

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway (migrations)
- Docker
- Prometheus
- Grafana
- k6 (tests de charge)
- Swagger / OpenAPI

---

# Architecture du projet

Le projet est structuré en plusieurs modules correspondant aux principaux domaines métier :

```
customer/
account/
transfer/
ledger/
audit/
config/
```

Les principaux composants du système sont :

- **Customer / KYC** : gestion des clients et validation d'identité
- **Accounts** : gestion des comptes et des soldes
- **Transfers** : gestion des virements entre comptes
- **Ledger** : enregistrement des transactions comptables
- **Audit** : journalisation des actions critiques

---

# Cas d'utilisation implémentés

Le système implémente les cas d'utilisation suivants :

1. **UC-01 — Enregistrement d’un client**
2. **UC-02 — Vérification KYC**
3. **UC-03 — Ouverture d’un compte bancaire**
4. **UC-04 — Consultation du solde et de l’historique**
5. **UC-05 — Virement entre comptes (Idempotent + Audit)**

---

# Démarrage du projet

## 1. Lancer la base de données

Le projet utilise PostgreSQL dans un conteneur Docker.

```
docker compose up -d
```

---

## 2. Lancer l'application Spring Boot

Depuis la racine du projet :

```
./mvnw spring-boot:run
```

L'application démarre sur :

```
http://localhost:8081
```

---

# Documentation de l'API

Swagger est disponible à l'adresse :

```
http://localhost:8081/swagger-ui/index.html
```

L'API utilise **Basic Auth**.

```
username: admin
password: admin
```

---

# Exemple de requêtes

## Création d'un client

```
POST /customers
```

Body :

```json
{
  "fullName": "John Doe",
  "email": "john@example.com"
}
```

---

## Approver le KYC

```
PATCH /customers/{id}/kyc/approve
```

---

## Créer un compte

```
POST /customers/{customerId}/accounts
```

---

## Consulter le solde

```
GET /accounts/{accountId}/balance
```

---

## Effectuer un virement

```
POST /transfers
```

Header requis :

```
X-Customer-Id
Idempotency-Key
```

Body :

```json
{
  "fromAccountId": "...",
  "toAccountId": "...",
  "amountCents": 500
}
```

---

# Observabilité

L'application expose des métriques Prometheus via :

```
/actuator/prometheus
```

Prometheus collecte ces métriques et Grafana permet de visualiser les **4 Golden Signals** :

- latence
- trafic
- erreurs
- saturation

Grafana est accessible via :

```
http://localhost:3001
```

---

# Tests de charge

Des tests de charge ont été réalisés avec **k6**.

Le scénario simule :

- consultation du solde
- consultation du ledger
- virements entre comptes

Résultats du test :

- Requêtes totales : **1708**
- Débit moyen : **15.43 requêtes/sec**
- Taux d'erreur : **0 %**
- Latence moyenne : **350 ms**
- P95 : **972 ms**

Ces résultats montrent que l'application reste stable sous charge.

---

# Load Balancing

Une architecture avec **NGINX** peut être utilisée pour distribuer les requêtes entre plusieurs instances de l'application.

Architecture :

```
Client → NGINX → Instances CanBankX → PostgreSQL
```

Cela permet :

- d'améliorer le débit
- de réduire la latence
- d'améliorer la tolérance aux pannes

---

# Caching

Une stratégie de cache peut être implémentée avec **Redis** pour les endpoints fréquemment consultés :

- `/accounts/{id}/balance`
- `/accounts/{id}/ledger`

Un TTL court (10–30 secondes) permet d'améliorer les performances tout en conservant une cohérence acceptable.

---

# Observabilité et performance

Les performances du système sont mesurées via :

- Prometheus
- Grafana
- k6

Ces outils permettent d'observer le comportement du système sous charge et d'identifier les optimisations possibles.

---

# Structure du dépôt

```
docs/
postman/
loadtests/
monitoring/
src/
README.md
```

Les documents d'architecture incluent :

- Arc42
- vue 4+1
- ADR
- modèle de domaine
- diagramme ER

---

# Conclusion

Cette première phase du projet CanBankX permet de démontrer :

- une API REST fonctionnelle
- une persistance fiable des données
- un système de journalisation et d'audit
- une observabilité complète avec Prometheus et Grafana
- une première analyse des performances avec k6

L'architecture actuelle peut évoluer vers une architecture **microservices avec API Gateway** dans les phases suivantes.