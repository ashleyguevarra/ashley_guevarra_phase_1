# Archive — sources Mermaid pour `arc42.md`

Fichier de référence uniquement. Les figures utilisées dans le document sont les PNG dans `docs/Images/` (`fig01_…` à `fig07_…`).

Pour régénérer un PNG : copier le contenu d’un bloc `mermaid` (sans la ligne ` ```mermaid `) dans un fichier `.mmd`, ou coller sur [mermaid.live](https://mermaid.live), puis exporter. Exemple avec `mmdc` :

```bash
mmdc -i mon_diagramme.mmd -o ../Images/fig01_3_1_contexte_metier.png
```

---

## `fig01_3_1_contexte_metier.png` — §3.1 Contexte métier

```mermaid
flowchart LR
    Client([Client])
    KYC([Service KYC simulé])
    Ext([Système bancaire externe futur])
    CanBankX[CanBankX Banking API]

    Client -->|Informations personnelles, demande compte, virement| CanBankX
    CanBankX -->|Confirmation, infos compte, résultat virement| Client
    KYC -->|Données personnelles| CanBankX
    CanBankX -->|Résultat validation KYC| KYC
    Ext -.->|Demande transaction futur| CanBankX
    CanBankX -.->|Confirmation/rejet futur| Ext
```

---

## `fig02_3_2_contexte_technique.png` — §3.2 Contexte technique

Version **simplifiée** (réplicas regroupés) — fichier source : `diagrams/arc42-gen/fig02_3_2_contexte_technique.mmd`.

```mermaid
flowchart TB
    subgraph Clients["Clients et tests de charge"]
        direction LR
        FE["Frontend (Nginx:80)"]
        K6[k6]
    end

    GW["KrakenD :8080 — API Gateway"]

    FE -->|HTTPS| GW
    K6 -->|HTTP| GW

    subgraph Monopath["Mode monolithe"]
        MO[canbankx_monolith] --> DBM[(PostgreSQL)]
    end

    GW -->|api/v1/*| MO

    subgraph Micropath["Mode microservices — docker-compose.lb.yml"]
        direction TB
        NGX["NGINX — load balancer :80"]
        ACC["account-service — 2 réplicas a, b\nPostgreSQL account"]
        TRF["transfer-service — 2 réplicas a, b\nPostgreSQL transfer + Redis"]
        NGX -->|least_conn| ACC
        NGX -->|least_conn| TRF
        TRF -->|saga → account-a| ACC
    end

    GW -->|KrakenD → NGINX| NGX

    subgraph Obs["Observabilité"]
        direction TB
        GRAF[Grafana] --> PROM[Prometheus]
    end

    PROM -.->|/actuator/prometheus| MO
    PROM -.->|/actuator/prometheus| ACC
    PROM -.->|/actuator/prometheus| TRF
```

---

## `fig03_5_niveau1.png` — §5 Niveau 1

```mermaid
flowchart TB
    subgraph CanBankX["CanBankX Banking API"]
        subgraph AccountService["account-service (2 instances derrière NGINX)"]
            Kyc[KycController]
            AccCtrl[AccountController]
            Consult[AccountConsultController]
            SagaInternal[InternalAccountSagaController]
        end
        subgraph TransferService["transfer-service (2 instances derrière NGINX)"]
            TransCtrl[TransferController]
            TransSvc[TransferService]
        end
    end

    subgraph Domaine
        Customer[Customer]
        Account[Account]
        Transfer[Transfer]
        Ledger[LedgerEntry]
        Audit[AuditLog]
    end

    Kyc --> Customer
    AccCtrl --> Account
    Consult --> Account
    Consult --> Ledger
    SagaInternal --> Account
    TransCtrl --> TransSvc
    TransSvc --> Transfer
    TransSvc --> SagaInternal

    subgraph Infra
        DB[(PostgreSQL)]
    end

    Account --> DB
    Customer --> DB
    Transfer --> DB
    Ledger --> DB
    Audit --> DB
```

---

## `fig04_5_n2_domaine.png` — §5 Niveau 2 domaine

```mermaid
flowchart TB
    subgraph Domaine["Couche Domaine — Identique en Monolith et Microservices"]
        Customer[Customer]
        Account[Account]
        Transfer[Transfer]
        Ledger[LedgerEntry]
        Audit[AuditLog]
    end

    Customer --> Account
    Account --> Ledger
    Transfer --> Ledger
    Transfer --> Audit
```

---

## `fig05_5_n2_application.png` — §5 Niveau 2 application

```mermaid
flowchart LR
    subgraph Monolith["Mode Monolith — Tous les services dans un processus"]
        RegM[RegisterCustomerService]
        ApproveM[ApproveKycService]
        OpenM[OpenAccountService]
        ConsultM[ConsultAccountService]
        TransM[TransferService]
    end

    subgraph Micro["Mode Microservices — Services répartis"]
        subgraph AccountSvc["account-service"]
            Reg[RegisterCustomerService]
            Approve[ApproveKycService]
            Open[OpenAccountService]
            Consult[ConsultAccountService]
        end
        subgraph TransferSvc["transfer-service"]
            Trans[TransferService]
        end
    end
```

---

## `fig06_5_n2_infrastructure.png` — §5 Niveau 2 infrastructure

```mermaid
flowchart TB
    subgraph Monolith["Mode Monolith"]
        subgraph ControllersM["Contrôleurs (1 processus)"]
            KycM[KycController]
            AccM[AccountController]
            ConsultM[AccountConsultController]
            TransM[TransferController]
        end
        subgraph ReposM["Repositories"]
            CustM[CustomerRepository]
            AccRepoM[AccountRepository]
            TransRepoM[TransferRepository]
            LedgerM[LedgerEntryRepository]
            AuditM[AuditLogRepository]
        end
        DB_Mono[("PostgreSQL unique")]
    end

    subgraph Micro["Mode Microservices — 2 JVM par type (LB NGINX)"]
        subgraph AccInfra["account-service-a / account-service-b"]
            Kyc[KycController]
            Acc[AccountController]
            Consult[AccountConsultController]
            Saga[InternalAccountSagaController]
            Cust[CustomerRepository]
            AccRepo[AccountRepository]
            Ledger[LedgerEntryRepository]
            Audit[AuditLogRepository]
            DB_Acc[("PostgreSQL account")]
        end
        subgraph TransInfra["transfer-service-a / transfer-service-b"]
            Trans[TransferController]
            TransRepo[TransferRepository]
            Redis[Redis]
            DB_Trans[("PostgreSQL transfer")]
        end
    end
```

---

## `fig07_7_deploiement.png` — §7 Déploiement

Version **simplifiée** — fichier source : `diagrams/arc42-gen/fig07_7_deploiement.mmd`.

```mermaid
flowchart TB
    subgraph User["<<Device>> User"]
        FE["Frontend (Nginx:80)"]
    end

    GW["API Gateway — KrakenD :8080"]

    subgraph Stress["Stress test"]
        direction LR
        K6[k6]
        LOC[locust]
    end

    subgraph Obs["Observabilité"]
        direction TB
        GF[Grafana] -->|PromQL| PR[Prometheus]
    end

    FE -->|HTTPS| GW
    K6 -->|HTTP| GW
    LOC -->|HTTP| GW

    subgraph OptA["Option A — monolithe (docker-compose.monolith.yml)"]
        direction TB
        NM["NGINX :8083"]
        MONO[canbankx_monolith :8080]
        PGM[("PostgreSQL :5432")]
        NM --> MONO
        MONO --> PGM
    end

    subgraph OptB["Option B — microservices (docker-compose.lb.yml)"]
        direction TB
        NL["NGINX :8082 — load balancer"]
        ACC["account-service — ×2 a,b :8080"]
        TRF["transfer-service — ×2 a,b :8080"]
        PGA[("PostgreSQL account :5435")]
        PGT[("PostgreSQL transfer :5436")]
        RDS["Redis :6379"]
        NL -->|customers / accounts| ACC
        NL -->|transfers| TRF
        ACC --> PGA
        TRF --> PGT
        TRF --> RDS
        TRF -->|/internal/saga/* → account-a| ACC
    end

    GW --> NM
    GW --> NL

    PR -.->|scrape| MONO
    PR -.->|scrape| ACC
    PR -.->|scrape| TRF
```
