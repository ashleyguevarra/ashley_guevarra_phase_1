# Diagrammes CanBankX (Mermaid)

Les diagrammes sont au format Mermaid (.mmd). Pour générer des PNG :

```bash
# Installer mermaid-cli
npm install -g @mermaid-js/mermaid-cli

# Générer les PNG dans docs/Images/
cd docs/diagrams
mmdc -i 3.1_contexte_metier.mmd -o ../Images/3.1_Diagram.png
mmdc -i 3.2_contexte_technique.mmd -o ../Images/3.2_contexte_technique.png
mmdc -i 5_niveau1_microservices.mmd -o ../Images/5_Niveau1.png
mmdc -i 6_UC01_register.mmd -o ../Images/6_UC01.png
mmdc -i 6_UC02_kyc.mmd -o ../Images/6_UC02.png
mmdc -i 6_UC03_open_account.mmd -o ../Images/6_UC03.png
mmdc -i 6_UC04_balance_ledger.mmd -o ../Images/6_UC04.png
mmdc -i 6_UC05_saga.mmd -o ../Images/6_UC05.png
mmdc -i 7_deploiement_monolith_et_microservices.mmd -o ../Images/7_DiagrammeDeploiement.png
```

Ou utiliser [mermaid.live](https://mermaid.live) pour éditer et exporter manuellement.
