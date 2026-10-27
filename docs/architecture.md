# Architettura

## Struttura directory
- `src/` - Sorgenti Java (main/test)
- `scripts/` - Script di build, provisioning, health
- `tests/` - Fixture e script di test
- `packaging/` - Artefatti di distribuzione/bundle plugin
- `config/` - Configurazione d'esempio
- `docs/` - Documentazione (fonte unica)
- `.gitea/workflows/` - Workflow Gitea Actions

## Note
- `docker-compose.yaml` e `.env.example` sono **esempi generici** (D21). I valori reali li definisce l'installatore.
- Volumi di produzione (`services/`, `config/`, `data/`) devono sopravvivere al riavvio, mai su tmpfs (R59).
- Il bind mount d'esempio usa il placeholder canonico `/path/to/dir` (R102).
- Il progetto non prescrive alcuna via di override (R101).
