# Contratto plugin v2 (service-config.json)

Questo documento descrive lo schema `schemaVersion: 2` introdotto per il gateway **Civil Servant** (FASE 3). È un'**estensione in-place** del contratto v1: i 4 campi legacy restano supportati senza modifica da parte dei plugin esistenti (R10).

## 1. Struttura

```jsonc
{
  "schemaVersion": 2,
  "name": "calibre-api",                // obbligatorio
  "version": "2.0.0",
  "description": "...",
  "language": "python",                 // legacy (v1); in v2 preferire runtime.type
  "runtime": { "type": "python", "version": "3.14" },
  "route":   { "prefix": "/calibre-api", "stripPrefix": 1 },  // oppure stringa legacy
  "backend": { "host": "127.0.0.1", "port": 8000, "portMode": "fixed" },
  "health":  { "type": "http", "path": "/healthz", "intervalSeconds": 10, "timeoutSeconds": 3, "startupTimeoutSeconds": 90 },
  "setup":   { "script": "setup.sh", "timeoutSeconds": 300 },
  "start":   { "command": "...", "env": {}, "envFile": "plugin.env" },
  "stop":    { "timeoutSeconds": 20 }
}
```

## 2. Campi di avvio (precedenza e discordanza)

Il comando di avvio viene risolto con la seguente **precedenza obbligatoria** (R105):

1. `start.command` (strutturato, v2)
2. `startCommand` (piatto, moderno)
3. `start_command` (piatto, legacy v1)

- **Il comando vincente non può essere vuoto.** Se nessuno dei tre campi è presente o valorizzato, il file viene rifiutato con errore esplicito (R4).
- **In caso di discordanza tra campi presenti**, il gateway **non fallisce mai**: emette un `WARN` con il nome del file e tutti i valori presenti, applica il comando con precedenza più alta e registra la divergenza in `StartBlock.divergentFields`. La divergenza è resa visibile nella dashboard (R107).
- **Warning di deprecazione**: l'uso di `start_command` produce un `WARN` che invita a migrare verso `start.command`/`startCommand`.

Questa regola sblocca l'allineamento tra i due plugin in revisione (`calibre-api` usa `startCommand`, `baikal-api` usa `start_command`).

## 3. Default e derivazione

| Blocco | Default quando assente | Note |
| --- | --- | --- |
| `schemaVersion` | `1` | Assente ⇒ contratto legacy v1; viene emesso un `WARN`. |
| `name` | **obbligatorio** | Unico caso che invalida il file (R4). |
| `version` | `0.0.0-unknown` + `WARN` | Mai bloccante. |
| `description` | uguale a `name` |  |
| `language` | derivato da `runtime.type`, altrimenti `unknown` | In v1 era memorizzato ma mai usato; in v2 preferire `runtime`. |
| `runtime` | `{type: language, version: <ultima Tier 1>}` | Se assente, nessun PATH forzato. |
| `route` | stringa legacy `" /foo "` ⇒ `{prefix:"/foo", stripPrefix:1}` oppure assente ⇒ `"/{name}"` | `stripPrefix: 1` coerente con le route statiche. |
| `backend` | `{host:"127.0.0.1", port:null, portMode:"auto"}` | Se `port` dichiarato e libero ⇒ `fixed`, altrimenti `auto` (FASE 5). |
| `health` | `{type:"process", path:null, intervalSeconds:10, timeoutSeconds:3, startupTimeoutSeconds:90}` | **Non** HTTP di default (nessun endpoint health nei plugin esistenti). |
| `setup` | `{script:null, timeoutSeconds:300}` | `script: null` ⇒ nessuno setup (scioglie difetto 5). |
| `start` | derivato dai campi di avvio con precedenza R105 | `command` non vuoto obbligatorio. |
| `stop` | `{timeoutSeconds:20}` |  |

## 4. Retrocompatibilità

- I 4 campi legacy v1 (`name`, `language`, `route`, `start_command`) **continuano a funzionare** senza modifiche.
- `route` come stringa viene convertita in oggetto con `stripPrefix: 1` (con warning).
- `language` viene migrato in `runtime.type` quando `runtime` assente.
- Un plugin v1 valido viene caricato correttamente; i warning di deprecazione sono loggati.

## 5. Errori e isolamento

- `name` mancante o vuoto ⇒ `ServiceConfigException` (plugin rifiutato, gateway non si arresta).
- Comando di avvio mancante o vuoto ⇒ `ServiceConfigException` (plugin rifiutato, gateway non si arresta).
- Errori di parsing/I/O per un singolo plugin ⇒ loggati come `ERROR`, il plugin viene skippato e gli altri restano operativi.

## 6. Note implementative

- Il caricamento avviene tramite `ServiceConfigHelper.load(File)` che restituisce un'istanza per-plugin (nessuno stato condiviso).
- Discovery e registrazione avvengono **dentro** la costruzione di `RouteLocator` (prima della prima interrogazione delle route), così le route dinamiche sono pubblicate all'avvio.
- I conflitti di route producono un suffisso progressivo (`/route-1`, `/route-2`, ...) con `WARN` esplicito e visibilità in `ServiceDescriptor`.
- I path dei servizi sono ancorati alla property `civilservant.services-dir` (default `/app/services`).
- L'URI di backend deriva da `backend.host`/`backend.port` e **non punta mai al gateway stesso**.
