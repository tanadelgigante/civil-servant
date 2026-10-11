# Architettura del repository

FASE 2, WORKPLAN Rev. 8 (R25, Task 2.1, Task 2.5). Definisce la mappa delle
directory, il ruolo di ciascuna e le invarianti (cosa **non** ci va dentro).

## Albero delle directory

```
civilservant/
├── src/main/java/          codice applicativo Java (gateway)
├── src/main/resources/     risorse impacchettate nel JAR
├── src/test/java/          test JUnit (convenzione Maven)
├── tests/                  fixture e script di test end-to-end (NON test JUnit)
│   └── fixtures/           dati di input per i test (JSON v1/v2, bundle d'esempio)
├── scripts/                script di build, provisioning, health check
├── packaging/              artefatti di packaging (file di supporto al rilascio)
├── config/                 configurazione d'esempio del gateway (application.yml)
├── services/               bundle dei plugin (d'esempio: example-service)
├── docs/                   documentazione e questo WORKPLAN
├── .gitea/                 workflow CI/CD (creato in FASE 11)
├── Dockerfile              build multi-stage del gateway
├── docker-compose.yaml     ESEMPIO generico di deployment (D21)
├── .env.example            ESEMPIO generico di variabili (D21)
├── version.ini             unica sorgente di verita' della versione (R26)
└── pom.xml                 build Maven
```

## Scelta esplicitata: `src/test/java` vs `tests/`

La convenzione Maven vuole i **test JUnit in `src/test/java`**. La directory
top-level `tests/` e' riservata a **fixture e script di test end-to-end** (es.
bundle plugin di esempio, JSON v1/v2 per i test di retrocompatibilita' della
FASE 3). Questa scelta evita di mescolare due cose diverse: test unitari che
girano con `mvn test` e materiale di supporto che non e' un test Maven.

## Invarianti

- `src/main/java` non contiene configurazione d'esempio ne' fixture.
- `tests/fixtures` non contiene codice di produzione.
- `config/` e' **configurazione d'esempio**, non la configurazione di una
  specifica installazione.
- `docker-compose.yaml` e `.env.example` sono **esempi generici** (D21):
  mostrano la forma, non i valori reali. I valori reali li definisce
  l'installatore nella propria installazione (R65). Il progetto **non
  prescrive alcuna via di override** (R101).
- `version.ini` e' l'**unica sorgente di verita'** della versione (R26); in
  sviluppo vale `X.Y.Z-SNAPSHOT`, alla release `X.Y.Z`. Il `<version>` del
  `pom.xml` e' allineato a `version.ini`.

## Compose ed `.env.example`: dove stanno le proprieta'

Scelta di collocazione (R64): le proprieta' d'esempio `PROPERTY1`/`PROPERTY2`
sono dichiarate **in entrambi** i supporti e sono quindi **coerenti fra loro**:

- `docker-compose.yaml`, blocco `environment:` — mostra il consumo con
  `${PROPERTY1:-CHANGE_ME}` e commenta significato/default lì dove la proprieta'
  e' usata (R63);
- `.env.example` — documentazione estesa per ciascuna proprieta' (significato,
  default, comportamento se manca, cosa succede con un valore errato), da
  copiare in `.env` (gitignorato).

Nessun obbligo di completezza: `.env.example` non e' una checklist
dell'installatore (R64).

## Volumi di produzione

I volumi `services/`, `config/` e la persistenza dei runtime (`data/`) **devono
sopravvivere al riavvio**: restano bind mount o volume Docker, **mai** tmpfs
(R59). Nel compose d'esempio usano il placeholder canonico `/path/to/dir`
(R102). Il tmpfs e' riservato al testbed effimero del workflow CI/CD (D20).
