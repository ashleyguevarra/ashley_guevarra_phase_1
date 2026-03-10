# ADR-002 — Idempotence et transactions pour les virements

## Statut
Accepté

## Date
2026-03-08

## Contexte

Dans un système bancaire, les virements entre comptes doivent être fiables et ne doivent jamais être exécutés plusieurs fois par erreur.

Dans le contexte d’une API REST, il peut arriver que le client répète une requête à cause :

- d’un timeout réseau
- d’une erreur côté client
- d’une tentative de retry automatique

Si la même requête est exécutée deux fois, cela pourrait provoquer un **double débit**, ce qui serait un problème critique pour un système financier.

Il est donc nécessaire de mettre en place un mécanisme pour garantir que la même opération ne soit pas exécutée plusieurs fois.

## Décision

Pour les opérations de virement (`POST /transfers`), j’ai choisi d’utiliser une **Idempotency-Key** fournie dans le header de la requête.

Chaque requête de transfert doit contenir :

Idempotency-Key: <clé unique>

Cette clé est enregistrée dans la table `transfers`.

Avant de créer un nouveau transfert, le système vérifie si une opération avec la même `Idempotency-Key` existe déjà.

- Si la clé existe déjà → le système retourne le transfert existant.
- Sinon → un nouveau transfert est créé.

Cela permet de garantir que la même requête ne crée pas plusieurs transactions.

## Transactions de base de données

L’opération de virement est exécutée dans une **transaction de base de données** afin de garantir la cohérence des données.

Les étapes suivantes sont exécutées de manière atomique :

1. Vérification du solde du compte source
2. Création du transfert
3. Création des écritures dans `ledger_entries`
4. Mise à jour des soldes des comptes
5. Création d’une entrée dans `audit_log`

Si une erreur survient pendant ces étapes, la transaction est annulée (rollback).

## Conséquences

### Avantages

- évite les doubles virements
- améliore la fiabilité de l’API
- permet aux clients de répéter une requête sans risque
- assure la cohérence des données grâce aux transactions

### Inconvénients

- nécessite de stocker et vérifier une clé d’idempotence
- ajoute une légère complexité dans la logique du transfert

## Alternatives considérées

### Ne pas utiliser d’idempotence

Une solution aurait été de simplement traiter chaque requête comme une nouvelle opération.

Cette approche a été rejetée car elle pourrait provoquer des **doubles transactions** si une requête est répétée.

### Utiliser un mécanisme externe de déduplication

Une autre option aurait été d’utiliser un système externe pour gérer la déduplication des requêtes.

Cette solution aurait ajouté une complexité inutile pour cette phase du projet.

## Impact sur le système

Ce mécanisme garantit que les virements sont traités de manière fiable et qu’un même appel API ne peut pas provoquer plusieurs opérations financières.
