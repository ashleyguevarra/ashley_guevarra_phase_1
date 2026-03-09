# BrokerX Banking API — Implémentation des cas d'utilisation

Ce projet implémente les premières fonctionnalités du système bancaire BrokerX sous forme d'une API REST développée avec **Java, Spring Boot, PostgreSQL et Docker**.

L’objectif de cette phase est de mettre en place le processus d’onboarding d’un client ainsi que la création d’un compte bancaire.

Cas d’utilisation implémentés :

- UC-01 — Enregistrement d’un client
- UC-02 — Vérification KYC
- UC-03 — Ouverture d’un compte bancaire
- UC-04 — Consultation du solde et de l’historique
- UC-05 — Virement entre comptes (idempotent)

La base de données est exécutée dans un conteneur **PostgreSQL Docker (`brokerx_db`)**.

---

# UC-01 — Enregistrement d’un client

### Acteur principal
Client

### Acteurs secondaires
Service Clients

### But
Créer un nouveau client dans le système.

### Préconditions
Aucune.

### Déclencheur
Un client soumet ses informations personnelles.

### Scénario nominal

1. Le client envoie une requête **POST /customers** avec son nom complet et son email.
2. Le système valide les informations.
3. Le système crée un client avec un **UUID unique**.
4. Le statut **KYC est initialisé à PENDING**.
5. Le système retourne l'identifiant du client.

### Commande de test

```bash
curl -s -u admin:admin -X POST http://localhost:8081/customers \
-H "Content-Type: application/json" \
-d '{"fullName":"John Doe","email":"john.doe@email.com"}'
```

### Réponse

```json
{
  "id": "UUID",
  "status": "PENDING"
}
```

### Postconditions

Un client est créé dans la base de données avec :

```
kyc_status = PENDING
```

---

# UC-02 — Vérification KYC

### Acteur principal
Service de conformité

### Acteurs secondaires
Service Clients, Service d’audit

### But
Vérifier l’identité du client afin de lui permettre d’utiliser le système bancaire.

### Préconditions

- Le client doit exister.
- Le client doit avoir un statut **KYC = PENDING**.

### Déclencheur
Le service de conformité approuve le client.

### Scénario nominal

1. Le système reçoit une requête **PATCH /customers/{id}/kyc/approve**.
2. Le système vérifie que le client existe.
3. Le système met à jour le statut KYC vers **APPROVED**.
4. Une entrée est ajoutée dans la table **audit_log** avec l’action **KYC_APPROVED**.
5. Le système retourne le nouveau statut.

### Commande de test

```bash
curl -i -X PATCH -u admin:admin \
"http://localhost:8081/customers/{id}/kyc/approve"
```

### Réponse

```json
{
  "id": "UUID",
  "status": "APPROVED"
}
```

### Postconditions

Le client possède maintenant :

```
kyc_status = APPROVED
```

et peut utiliser les fonctionnalités bancaires.

---

# UC-03 — Ouverture d’un compte bancaire

### Acteur principal
Client

### Acteurs secondaires
Service Comptes, Service d’audit

### But
Créer un compte bancaire afin de permettre les dépôts et les virements.

### Préconditions

- Le client doit être authentifié.
- Le client doit être **KYC VERIFIED (APPROVED)**.

### Déclencheur
Le client demande l’ouverture d’un compte.

### Scénario nominal

1. Le client envoie une requête **POST /customers/{customerId}/accounts**.
2. Le système valide :
   - le type de compte (CHECKING ou SAVINGS)
   - la devise (CAD ou USD).
3. Le système crée un nouveau compte avec :
   - statut **ACTIVE**
   - solde initial **0**.
4. Une entrée d’audit **ACCOUNT_OPENED** est créée dans **audit_log**.
5. Le système retourne l’identifiant et les informations du compte.

### Commande de test

```bash
curl -i -u admin:admin -X POST \
"http://localhost:8081/customers/{customerId}/accounts" \
-H "Content-Type: application/json" \
-d '{"type":"CHECKING","currency":"CAD"}'
```

### Réponse

```json
{
  "id": "ACCOUNT_ID",
  "customerId": "CUSTOMER_ID",
  "type": "CHECKING",
  "currency": "CAD",
  "status": "ACTIVE",
  "balanceCents": 0
}
```

### Postconditions

Un compte actif est créé dans la base de données :

```
status = ACTIVE
balance_cents = 0
```

Le compte est maintenant prêt pour effectuer des transactions.

---

# UC-04 — Consultation du solde et de l’historique

### Acteur principal
Client

### Acteurs secondaires
Service Comptes, Observabilité

### But
Permettre au client de consulter le **solde actuel** et **l’historique des transactions** d’un compte.

### Préconditions

- Le client doit être authentifié.
- Le compte doit appartenir au client.

Dans cette phase du projet, l’authentification est réalisée via **Basic Auth**.  
L’identifiant du client est transmis via le header :

```
X-Customer-Id
```

Ce mécanisme simule l’identité du client avant l’introduction d’un **JWT** dans les prochaines phases.

### Déclencheur

Le client souhaite consulter le solde ou l’historique de son compte.

### Scénario nominal

1. Le client envoie une requête **GET /accounts/{accountId}/balance** ou **GET /accounts/{accountId}/ledger**.
2. Le système valide l’authentification.
3. Le système récupère le **customerId** via le header `X-Customer-Id`.
4. Le système vérifie que le compte existe.
5. Le système vérifie que le compte appartient au client.
6. Le système retourne le solde ou l’historique paginé des transactions.

---

### Commande de test — Consulter le solde

```bash
curl -u admin:admin \
-H "X-Customer-Id: {customerId}" \
http://localhost:8081/accounts/{accountId}/balance
```

### Réponse

```json
{
  "accountId": "ACCOUNT_ID",
  "balanceCents": 0
}
```

---

### Commande de test — Consulter l’historique

```bash
curl -u admin:admin \
-H "X-Customer-Id: {customerId}" \
"http://localhost:8081/accounts/{accountId}/ledger?page=0&size=10"
```

### Réponse

```json
{
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0,
  "items": []
}
```
---

# UC-05 — Virement entre comptes (Idempotent + Audit)

### Acteur principal
Client

### Acteurs secondaires
Service Transferts, Base de données (transactions), Audit / Compliance

### But
Effectuer un virement entre deux comptes tout en garantissant l’absence de double débit grâce à une **Idempotency-Key**.

---

# Préconditions

- Client authentifié
- Compte source actif
- Solde suffisant
- Idempotency-Key fournie dans la requête

---

# Déclencheur

Le client envoie une requête **POST /transfers** pour transférer de l’argent d’un compte source vers un compte destination.

---

# Scénario nominal

1. Le client envoie une requête **POST /transfers** contenant :
   - `fromAccountId`
   - `toAccountId`
   - `amountCents`
   - `Idempotency-Key` dans le header.

2. Le système vérifie si un transfert avec cette **Idempotency-Key** existe déjà.

3. Si un transfert existe déjà :
   - Le système retourne le transfert existant (même `transfer_id`).

4. Sinon :
   - Le système valide :
     - que le montant est supérieur à 0
     - que les comptes existent
     - que le compte source appartient au client
     - que le solde est suffisant

5. Le système exécute une **transaction DB** :
   - crée un `transfer` avec statut **COMPLETED**
   - écrit deux `ledger_entries` :
     - **DEBIT** sur le compte source
     - **CREDIT** sur le compte destination
   - met à jour les soldes des comptes
   - écrit une entrée `audit_log` **TRANSFER_CREATED**

6. Le système retourne la confirmation du virement.

---

# Commandes de test — Création des clients

```bash
CID_A=$(curl -s -u admin:admin -X POST http://localhost:8081/customers \
  -H "Content-Type: application/json" \
  -d '{"fullName":"UC05 Client A","email":"uc05a@test.com"}' | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

curl -s -u admin:admin -X PATCH "http://localhost:8081/customers/$CID_A/kyc/approve"

CID_B=$(curl -s -u admin:admin -X POST http://localhost:8081/customers \
  -H "Content-Type: application/json" \
  -d '{"fullName":"UC05 Client B","email":"uc05b@test.com"}' | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

curl -s -u admin:admin -X PATCH "http://localhost:8081/customers/$CID_B/kyc/approve"
```

---

# Commandes de test — Création des comptes

```bash
AID_SRC=$(curl -s -u admin:admin -X POST "http://localhost:8081/customers/$CID_A/accounts" \
  -H "Content-Type: application/json" \
  -d '{"currency":"CAD","type":"CHECKING"}' | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')

AID_DST=$(curl -s -u admin:admin -X POST "http://localhost:8081/customers/$CID_B/accounts" \
  -H "Content-Type: application/json" \
  -d '{"currency":"CAD","type":"CHECKING"}' | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
```

---

# Initialisation du solde du compte source

```bash
docker exec -i brokerx_db psql -U brokerx -d brokerx -c \
"UPDATE accounts SET balance_cents = 10000 WHERE id = '$AID_SRC';"
```

---

# Commande de test — Virement

```bash
IDEM_KEY="uc05-001"

curl -s -u admin:admin \
  -H "X-Customer-Id: $CID_A" \
  -H "Idempotency-Key: $IDEM_KEY" \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8081/transfers \
  -d "{\"fromAccountId\":\"$AID_SRC\",\"toAccountId\":\"$AID_DST\",\"amountCents\":2500}"
```

---

# Réponse

```json
{
  "id": "fe84c117-799c-40ef-996a-e524a2583a47",
  "fromAccountId": "2eb490dc-64b1-4379-8d9e-cddb61b363b4",
  "toAccountId": "4cbef0f3-45fd-434b-a587-2d2546861d5a",
  "amountCents": 2500,
  "status": "COMPLETED",
  "createdAt": "2026-03-05T22:10:34.707530Z"
}
```

---

# Test d’idempotence

La même requête avec la même **Idempotency-Key** retourne le **même transfer_id**.

```bash
curl -s -u admin:admin \
  -H "X-Customer-Id: $CID_A" \
  -H "Idempotency-Key: uc05-001" \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8081/transfers \
  -d "{\"fromAccountId\":\"$AID_SRC\",\"toAccountId\":\"$AID_DST\",\"amountCents\":2500}"
```

---

# Vérification des soldes

```bash
curl -s -u admin:admin -H "X-Customer-Id: $CID_A" \
"http://localhost:8081/accounts/$AID_SRC/balance"

curl -s -u admin:admin -H "X-Customer-Id: $CID_B" \
"http://localhost:8081/accounts/$AID_DST/balance"
```

### Réponse

```json
{
  "accountId": "2eb490dc-64b1-4379-8d9e-cddb61b363b4",
  "balanceCents": 7500
}
```

```json
{
  "accountId": "4cbef0f3-45fd-434b-a587-2d2546861d5a",
  "balanceCents": 2500
}
```

---

# Vérification du ledger

```bash
curl -s -u admin:admin -H "X-Customer-Id: $CID_A" \
"http://localhost:8081/accounts/$AID_SRC/ledger?page=0&size=10"

curl -s -u admin:admin -H "X-Customer-Id: $CID_B" \
"http://localhost:8081/accounts/$AID_DST/ledger?page=0&size=10"
```

### Réponse

```json
{
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "items": [
    {
      "direction": "DEBIT",
      "amountCents": 2500,
      "description": "TRANSFER fe84c117-799c-40ef-996a-e524a2583a47"
    }
  ]
}
```

```json
{
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "items": [
    {
      "direction": "CREDIT",
      "amountCents": 2500,
      "description": "TRANSFER fe84c117-799c-40ef-996a-e524a2583a47"
    }
  ]
}
```

---

# Vérification de l’audit

```bash
docker exec -i brokerx_db psql -U brokerx -d brokerx -c \
"SELECT action, entity_type, entity_id FROM audit_log ORDER BY created_at DESC LIMIT 5;"
```

### Résultat attendu

```
TRANSFER_CREATED | TRANSFER | fe84c117-799c-40ef-996a-e524a2583a47
```

---

# Extensions / Variantes — UC-01 (Enregistrement d’un client)

### E1 — Données invalides

Si les informations fournies ne respectent pas les contraintes de validation  
(ex. `fullName` vide ou trop court, email invalide) :

```
HTTP 400
VALIDATION_ERROR
```

Exemple :

```bash
curl -i -u admin:admin -X POST http://localhost:8081/customers \
-H "Content-Type: application/json" \
-d '{"fullName":"","email":"invalid-email"}'
```

---

### E2 — Client déjà existant

Si un client existe déjà avec le même email :

```
HTTP 409
CUSTOMER_ALREADY_EXISTS
```

Exemple :

```bash
curl -i -u admin:admin -X POST http://localhost:8081/customers \
-H "Content-Type: application/json" \
-d '{"fullName":"John Doe","email":"john.doe@email.com"}'
```

---

# Extensions / Variantes — UC-02 (Vérification KYC)

### E1 — Client introuvable

Si l’identifiant `{id}` ne correspond à aucun client dans le système :

```
HTTP 404
CUSTOMER_NOT_FOUND
```

Exemple :

```bash
curl -i -X PATCH -u admin:admin \
"http://localhost:8081/customers/00000000-0000-0000-0000-000000000000/kyc/approve"
```

---

### E2 — Statut KYC invalide

Si le client n’est pas dans l’état **PENDING** (ex. déjà APPROVED) :

```
HTTP 409
INVALID_KYC_STATE
```

Exemple :

```bash
curl -i -X PATCH -u admin:admin \
"http://localhost:8081/customers/{id}/kyc/approve"
```

---

### E3 — Client non autorisé

Si l’utilisateur n’est pas authentifié correctement :

```
HTTP 401
UNAUTHORIZED
```

Exemple :

```bash
curl -i -X PATCH \
"http://localhost:8081/customers/{id}/kyc/approve"
```

---

# Extensions / Variantes — UC-03 (Ouverture d’un compte bancaire)

## E1 — Devise non supportée

Si la devise demandée n’est pas supportée par le système :

```
HTTP 400
UNSUPPORTED_CURRENCY
```

Exemple :

```bash
curl -i -u admin:admin -X POST \
"http://localhost:8081/customers/{customerId}/accounts" \
-H "Content-Type: application/json" \
-d '{"type":"CHECKING","currency":"EUR"}'
```

---

## E2 — Client non autorisé / KYC manquant

Si le client n’a pas encore passé la vérification **KYC** :

```
HTTP 403
KYC_REQUIRED
```

Exemple :

```bash
curl -i -u admin:admin -X POST \
"http://localhost:8081/customers/{customerId}/accounts" \
-H "Content-Type: application/json" \
-d '{"type":"CHECKING","currency":"CAD"}'
```

---

# Extensions / Variantes — UC-04 (Consultation du solde et de l’historique)

## E1 — Compte introuvable

Si le compte demandé n’existe pas dans le système :

```
HTTP 404
ACCOUNT_NOT_FOUND
```

---

## E2 — Accès interdit (ownership)

Si le compte n’appartient pas au client :

```
HTTP 403
FORBIDDEN_RESOURCE
```

Exemple :

```bash
curl -i -u admin:admin \
-H "X-Customer-Id: 11111111-1111-1111-1111-111111111111" \
"http://localhost:8081/accounts/{accountId}/balance"
```

---

# Postconditions — UC-04

Aucune donnée n’est modifiée.

Le système retourne simplement :

- le **solde du compte**
- l’**historique des transactions**

Les requêtes sont également observables via les **métriques HTTP exposées par Spring Actuator**.

---

# Extensions / Variantes — UC-05 (Virement entre comptes)

## E1 — Idempotency-Key absente

Si la requête ne contient pas de **Idempotency-Key**, le système rejette la requête.

```
HTTP 400
IDEMPOTENCY_KEY_REQUIRED
```

Exemple :

```bash
curl -i -u admin:admin \
  -H "X-Customer-Id: $CID_A" \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8081/transfers \
  -d "{\"fromAccountId\":\"$AID_SRC\",\"toAccountId\":\"$AID_DST\",\"amountCents\":100}"
```

---

## E2 — Solde insuffisant

Si le compte source ne possède pas un solde suffisant pour effectuer le virement :

```
HTTP 409
INSUFFICIENT_FUNDS
```

Exemple :

```bash
curl -i -u admin:admin \
  -H "X-Customer-Id: $CID_A" \
  -H "Idempotency-Key: uc05-no-money" \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8081/transfers \
  -d "{\"fromAccountId\":\"$AID_SRC\",\"toAccountId\":\"$AID_DST\",\"amountCents\":99999999}"
```

---

## E3 — Compte destination invalide

Si le compte destination n'existe pas :

```
HTTP 404
ACCOUNT_NOT_FOUND
```

Exemple :

```bash
curl -i -u admin:admin \
  -H "X-Customer-Id: $CID_A" \
  -H "Idempotency-Key: uc05-bad-dst" \
  -H "Content-Type: application/json" \
  -X POST http://localhost:8081/transfers \
  -d "{\"fromAccountId\":\"$AID_SRC\",\"toAccountId\":\"00000000-0000-0000-0000-000000000000\",\"amountCents\":100}"
```

---

# Postconditions — UC-05

- Le virement est enregistré **une seule fois** grâce à l’**Idempotency-Key**.
- Deux écritures sont ajoutées dans `ledger_entries` :
  - **DEBIT** sur le compte source
  - **CREDIT** sur le compte destination
- Les soldes des comptes sont mis à jour.
- Une entrée `audit_log` est créée :

```
TRANSFER_CREATED
```

---

# Architecture technique

## Technologies utilisées

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Docker

---

## Structure principale du projet

```
customer/
    Customer.java
    CustomerRepository.java
    RegisterCustomerService.java
    ApproveKycService.java
    KycController.java

account/
    Account.java
    AccountRepository.java
    OpenAccountService.java
    AccountController.java
    ConsultAccountService.java

transfer/
    Transfer.java
    TransferRepository.java
    TransferService.java
    TransferController.java

ledger/
    LedgerEntry.java
    LedgerEntryRepository.java
    LedgerService.java

audit/
    AuditLog.java
    AuditLogRepository.java
```

---

# Persistance

La persistance est gérée avec **Spring Data JPA** et les données sont stockées dans **PostgreSQL**.

Chaque opération critique crée une entrée dans **audit_log** afin d’assurer la **traçabilité des actions**.

---

# Conclusion

Les principaux cas d’utilisation du système **BrokerX** ont été implémentés :

- enregistrement d’un client
- validation KYC
- ouverture d’un compte bancaire
- consultation du solde
- consultation de l’historique des transactions
- virement entre comptes avec **idempotence**

Le système garantit :

- la cohérence des transactions grâce aux **transactions SQL**
- l’absence de double débit via les **Idempotency-Key**
- la traçabilité complète des actions grâce à **audit_log**

Cette première phase pose les bases de l’architecture bancaire et permet l’évolution vers :

- une architecture **microservices**
- une **API Gateway**
- l’utilisation de **JWT pour l’authentification**
- l’observabilité (Prometheus / Grafana)
- les tests de charge et la scalabilité.

## Documentation API

La documentation OpenAPI de l’API BrokerX est accessible via Swagger UI à l’adresse suivante :

http://localhost:8081/swagger-ui/index.html

## Tests de performance (k6)

Des tests de charge ont été réalisés à l’aide de l’outil **k6** afin de simuler un trafic concurrent sur l’API.

Le script de test simule plusieurs utilisateurs virtuels effectuant les opérations suivantes :

- consultation du solde d’un compte
- consultation de l’historique (ledger)
- exécution d’un virement entre comptes

Le scénario de charge augmente progressivement le nombre d’utilisateurs :

- 10 utilisateurs pendant 30 secondes
- 25 utilisateurs pendant 30 secondes
- 50 utilisateurs pendant 30 secondes
- retour à 0 utilisateurs

Les seuils de performance définis sont :

- moins de **5 % d’erreurs HTTP**
- **p95 < 500 ms** pour la latence des requêtes

Les tests ont été exécutés :

1. **accès direct via NGINX (load balancer)**
2. **accès via l’API Gateway KrakenD**

Les résultats montrent que l’API supporte correctement une charge concurrente modérée sans erreurs significatives.  
L’introduction de l’API Gateway ajoute une légère surcharge de latence, ce qui est attendu puisqu’une couche supplémentaire de routage est introduite. Cependant, cette architecture permet une meilleure **centralisation des accès, gestion des routes et évolutivité vers une architecture microservices complète**.

| Scénario | Endpoint | Résultat |
|--------|--------|--------|
| Accès direct (NGINX) | /accounts/{id}/balance | Réponses stables |
| Accès via Gateway (KrakenD) | /accounts/{id}/balance | Réponses stables |

# Observabilité et tests de charge

## Observabilité

Afin de surveiller le comportement de l’application BrokerX, des mécanismes d’observabilité ont été mis en place.

L’application expose des métriques via **Spring Boot Actuator** et **Micrometer**, accessibles à l’endpoint :

```
/actuator/prometheus
```

Ces métriques sont collectées par **Prometheus**, puis visualisées dans **Grafana**.

Les tableaux de bord Grafana permettent d’observer les **4 Golden Signals** :

### Latence
Temps de réponse des requêtes HTTP mesuré à partir des métriques `http_server_requests`.

### Trafic
Nombre de requêtes HTTP par seconde (Requests per second).

### Erreurs
Nombre de requêtes retournant des codes HTTP 4xx ou 5xx.

### Saturation
Utilisation des ressources applicatives, notamment la mémoire JVM.

Ces métriques permettent de comprendre le comportement du système sous différentes charges.

---

## Tests de charge

Des tests de charge ont été réalisés avec l’outil **k6** afin de simuler plusieurs utilisateurs accédant à l’API.

Le scénario de test génère un mélange de requêtes :

- consultation du solde
- consultation de l’historique (ledger)
- virements entre comptes

Le test augmente progressivement le nombre d’utilisateurs virtuels afin d’observer l’évolution des performances.

### Résultats obtenus

- **Requêtes totales** : 1708
- **Débit moyen** : 15.43 requêtes/seconde
- **Taux d’échec** : 0 %
- **Latence moyenne** : 350.85 ms
- **Latence médiane** : 230.03 ms
- **P95** : 972.02 ms
- **Latence maximale** : 1.26 s

Ces résultats montrent que le système reste stable sous charge et que toutes les requêtes ont été traitées correctement.

Cependant, la latence P95 dépasse l’objectif initial de 500 ms, ce qui indique que des optimisations supplémentaires pourraient être nécessaires (cache, load balancing, microservices, etc.).

# Validation du Load Balancer (NGINX)

Afin de valider l’architecture avec équilibrage de charge, nous avons configuré **NGINX** comme reverse proxy devant deux instances de l’application Spring Boot.

Architecture utilisée :

Client → NGINX (port 8082) → app1 / app2 → PostgreSQL

Les deux instances de l’application sont déployées dans des conteneurs Docker :

- brokerx_app1
- brokerx_app2

NGINX agit comme point d’entrée unique et distribue les requêtes vers les deux instances backend.

---

# Vérification de la santé du système

Commande exécutée :

```bash
curl -i -u admin:admin http://localhost:8082/actuator/health
```

Résultat :

```http
HTTP/1.1 200
Server: nginx/1.29.5
Content-Type: application/vnd.spring-boot.actuator.v3+json

{"status":"UP","groups":["liveness","readiness"]}
```

Cette réponse confirme que :

- NGINX est accessible sur le port **8082**
- la requête est correctement redirigée vers une instance backend
- l'application Spring Boot est **UP**

---

# Test fonctionnel via le Load Balancer

Nous avons ensuite testé un cas d'utilisation réel du système via NGINX.

Création d’un client :

```bash
curl -i -u admin:admin -X POST http://localhost:8082/customers \
  -H "Content-Type: application/json" \
  -d '{"fullName":"LB Test User","email":"lbtest@example.com"}'
```

Résultat :

```http
HTTP/1.1 200

{"id":"b36cff21-94cd-4661-9e72-d1e37d425377","status":"PENDING"}
```

Cette réponse confirme que :

- la requête a été acceptée par NGINX
- elle a été transmise à une instance backend
- le client a été correctement enregistré dans la base de données

---

# Validation dans les logs des instances

Les logs des conteneurs backend montrent que la requête a bien été traitée par l’application.

Commande utilisée :

```bash
docker logs brokerx_app1 --tail 50
docker logs brokerx_app2 --tail 50
```

Extrait des logs :

```text
Securing POST /customers
Authenticated user
insert into customers
```

Ces logs démontrent que :

- la requête a atteint l’application Spring Boot
- l’authentification a été effectuée
- l’insertion dans PostgreSQL a été exécutée

---

# Conclusion

Les tests démontrent que :

- NGINX agit correctement comme **reverse proxy**
- les requêtes sont **routées vers les instances backend**
- les fonctionnalités du système fonctionnent **via le load balancer**
- la base de données PostgreSQL est correctement utilisée par les deux instances

L’architecture **NGINX + plusieurs instances Spring Boot + PostgreSQL** fonctionne donc correctement.

# Non-Functional Requirements (NFR)

The following performance and reliability targets are defined for the BrokerX API during Phase 1.

| Metric | Target |
|------|------|
| P95 latency – GET /accounts/{id}/balance | < 200 ms |
| P95 latency – POST /transfers | < 400 ms |
| Throughput target | 50–200 requests/sec |
| Error rate | < 1% |
| Availability target | > 99% during load tests |

These targets will be validated during the load testing phase using tools such as k6 or JMeter and monitored through Prometheus and Grafana dashboards.

# Résultats des tests de charge

Les tests de charge ont été réalisés avec **k6** sur l’API BrokerX en simulant un mélange de requêtes :

- consultation du solde
- consultation du ledger
- virement entre comptes

## Résultats observés

- **Requêtes totales** : 1708
- **Débit moyen** : 15.43 requêtes/seconde
- **Taux d’échec** : 0 %
- **Latence moyenne** : 350.85 ms
- **Latence médiane** : 230.03 ms
- **P90** : 862.57 ms
- **P95** : 972.02 ms
- **Latence maximale** : 1.26 s

## Analyse

Le système est demeuré stable pendant le test de charge et toutes les vérifications ont réussi.

Aucune requête n’a échoué, ce qui montre que l’API demeure fonctionnelle même sous une montée progressive de charge.

En revanche, la cible initiale de latence `P95 < 500 ms` n’a pas été respectée dans ce scénario, puisque la latence P95 observée est de **972.02 ms**.

Ce résultat montre que l’application est correcte et robuste, mais qu’elle nécessitera des optimisations supplémentaires pour respecter des objectifs de performance plus stricts, notamment via :

- l’ajout d’un cache
- le load balancing sur plusieurs instances
- l’introduction d’une API Gateway
- des optimisations au niveau de la persistance ou des endpoints les plus sollicités

# Load Balancing

Afin d'améliorer la performance et la disponibilité du système BrokerX, une stratégie de **load balancing** peut être mise en place devant l'application.

Le load balancing permet de distribuer les requêtes HTTP sur plusieurs instances de l'application afin de :

- réduire la latence moyenne
- augmenter le débit maximal (RPS)
- améliorer la tolérance aux pannes

## Architecture proposée

Dans une architecture de production, un **reverse proxy NGINX** peut être utilisé devant plusieurs instances de l'application Spring Boot.

Architecture simplifiée :

Client → NGINX Load Balancer → Instances BrokerX → PostgreSQL

Chaque instance BrokerX exécute la même application mais peut traiter une partie des requêtes.

## Exemple de configuration NGINX

Exemple simplifié de configuration :

```nginx
events {}

http {

    upstream brokerx_backend {
        server app1:8081;
        server app2:8081;
        server app3:8081;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://brokerx_backend;
        }
    }
}
```

Dans cet exemple, les requêtes sont distribuées entre trois instances de l'application.

## Stratégie de distribution

NGINX peut utiliser plusieurs stratégies :

- round robin (par défaut)
- least connections
- IP hash

Pour ce projet, **round robin** est suffisant.

## Impact attendu

L'utilisation d'un load balancer permet :

- d'augmenter le nombre de requêtes supportées
- de réduire les temps de réponse sous forte charge
- de maintenir le service disponible si une instance tombe en panne

Ces améliorations peuvent être observées via les métriques Prometheus et les dashboards Grafana.

# Format des erreurs

L’API BrokerX retourne des erreurs au format JSON afin de fournir des messages cohérents aux clients de l’API.

## Structure d’une erreur

```json
{
  "timestamp": "2026-03-08T18:28:42.854893Z",
  "message": "Invalid request",
  "error": "VALIDATION_ERROR",
  "details": [
    {
      "field": "amountCents",
      "message": "must be greater than or equal to 1"
    }
  ]
}
```

## Description des champs

| Champ | Description |
|------|-------------|
| `timestamp` | Date et heure de l’erreur |
| `message` | Message général décrivant l’erreur |
| `error` | Code d’erreur applicatif |
| `details` | Informations supplémentaires (ex. champ invalide) |

## Exemples de codes d’erreur

### Validation
- `VALIDATION_ERROR`

### Ressource introuvable
- `CUSTOMER_NOT_FOUND`
- `ACCOUNT_NOT_FOUND`

### Conflit métier
- `CUSTOMER_ALREADY_EXISTS`
- `INVALID_KYC_STATE`
- `INSUFFICIENT_FUNDS`

### Accès refusé / sécurité
- `UNAUTHORIZED`
- `FORBIDDEN_RESOURCE`

## Bounded Contexts

L’analyse du domaine BrokerX pour la phase 1 permet d’identifier quatre bounded contexts principaux :

### 1. Customers / KYC
Ce contexte est responsable de l’enregistrement des clients et de la gestion du statut KYC.  
Il couvre la création d’un client, l’unicité de l’adresse courriel et l’approbation KYC.

**Entité principale :**
- Customer

**Règles métier principales :**
- un client est créé avec un statut `PENDING`
- un email doit être unique
- un client doit être `APPROVED` avant de pouvoir ouvrir un compte

### 2. Accounts
Ce contexte gère les comptes bancaires du client.  
Il couvre l’ouverture d’un compte, le solde courant et la consultation de l’historique.

**Entité principale :**
- Account

**Règles métier principales :**
- un compte appartient à un seul client
- un compte possède un type et une devise
- un compte actif peut être consulté et utilisé dans un virement

### 3. Transfers
Ce contexte gère les virements entre comptes.  
Il couvre la validation métier du transfert, l’idempotence et l’exécution transactionnelle.

**Entité principale :**
- Transfer

**Règles métier principales :**
- le montant doit être supérieur à 0
- le compte source doit avoir un solde suffisant
- une même `Idempotency-Key` ne doit pas créer un double traitement
- un transfert réussi met à jour les deux comptes

### 4. Audit / Ledger
Ce contexte gère la traçabilité des événements critiques et l’historique comptable.

**Entités principales :**
- LedgerEntry
- AuditLog

**Règles métier principales :**
- chaque transfert crée une écriture de débit et une écriture de crédit
- chaque action critique crée une entrée d’audit
- les journaux sont append-only

## Domain Map

Les bounded contexts interagissent de la manière suivante :

- **Customers / KYC** fournit l’identité du client et son statut de conformité.
- **Accounts** dépend du statut KYC pour autoriser l’ouverture d’un compte.
- **Transfers** utilise les comptes comme source et destination d’un virement.
- **Audit / Ledger** enregistre les événements produits par les contextes Accounts et Transfers.

### Relations principales

- Customers / KYC -> Accounts  
  Un client approuvé peut ouvrir un ou plusieurs comptes.

- Accounts -> Transfers  
  Les comptes participent comme compte source ou compte destination d’un transfert.

- Transfers -> Ledger  
  Chaque transfert génère des écritures comptables de débit et de crédit.

- Customers / KYC, Accounts, Transfers -> Audit  
  Les actions critiques produisent des entrées d’audit append-only.

## Modèle de données (ER simplifié)

Le modèle relationnel minimal de la phase 1 est structuré autour de cinq tables principales.

### customers
Contient les informations du client et son statut KYC.

Champs principaux :
- `id`
- `full_name`
- `email`
- `kyc_status`
- `created_at`

Contraintes :
- `id` unique
- `email` unique

### accounts
Contient les comptes bancaires des clients.

Champs principaux :
- `id`
- `customer_id`
- `type`
- `currency`
- `status`
- `balance_cents`
- `created_at`

Contraintes :
- `customer_id` clé étrangère vers `customers(id)`

### transfers
Contient les virements entre comptes.

Champs principaux :
- `id`
- `from_account_id`
- `to_account_id`
- `amount_cents`
- `status`
- `idempotency_key`
- `created_at`

Contraintes :
- `from_account_id` clé étrangère vers `accounts(id)`
- `to_account_id` clé étrangère vers `accounts(id)`
- `idempotency_key` unique

### ledger_entries
Contient les écritures comptables générées par les transferts.

Champs principaux :
- `id`
- `account_id`
- `transfer_id`
- `direction`
- `amount_cents`
- `description`
- `created_at`

Contraintes :
- `account_id` clé étrangère vers `accounts(id)`
- `transfer_id` clé étrangère vers `transfers(id)`

### audit_log
Contient les événements métier critiques sous forme append-only.

Champs principaux :
- `id`
- `action`
- `entity_type`
- `entity_id`
- `payload_json`
- `created_at`

# Stratégie de mise en cache (Caching)

Afin d'améliorer les performances du système BrokerX, une **mise en cache avec Redis** a été implémentée pour certaines requêtes fréquemment consultées.

Le caching permet de réduire les accès répétés à la base de données et d'améliorer les temps de réponse pour les opérations de lecture.

## Technologies utilisées

La solution de caching repose sur les composants suivants :

- **Redis** comme cache distribué en mémoire
- **Spring Cache** pour l'abstraction de la mise en cache
- **Spring Boot Redis (`spring-boot-starter-data-redis`)** pour l'intégration avec Redis

Redis est exécuté comme **conteneur Docker** dans l'infrastructure du projet.

Architecture simplifiée :

Client → API BrokerX (Spring Boot) → Redis Cache → PostgreSQL

## Endpoint mis en cache

L'endpoint suivant a été sélectionné pour le caching :

GET /customers/{customerId}/accounts/{accountId}/balance

Cet endpoint est fréquemment utilisé par les clients pour consulter le solde de leur compte.

La mise en cache est implémentée dans la couche service :

```java
@Cacheable(value = "accountBalance",
           key = "#accountId.toString() + ':' + #customerId.toString()")
public long getBalance(UUID accountId, UUID customerId)
```

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