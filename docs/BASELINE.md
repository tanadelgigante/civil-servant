# Baseline tecnica FASE 0

Data: 2026-10-11
Piano di riferimento: `docs/WORKPLAN.md` Rev. 8
Baseline committata: commit `f696f6f` (Task 0.1) sopra HEAD `d491a3c` "first release"

Questo documento registra la baseline tecnica richiesta da **Task 0.5** e l'esito
della **verifica di non regressione** di **Task 0.6**. La FASE 0 non modifica il
codice applicativo: le tre classi Java, `pom.xml`, `config/application.yml` e i
file di `services/example-service/` restano intatti.

## 1. Repository e versioni (baseline)

| Voce | Valore |
| --- | --- |
| Percorso | `/mnt/v/devel/civilservant` |
| Branch | `main` |
| HEAD iniziale | `d491a3c` "first release" |
| Remoto `origin` | `https://repo.kenshiro.lab.tana/alessio/civil-servant.git` (non canonico, R108) |
| Versione applicazione (`pom.xml`) | `1.0-SNAPSHOT` |
| `version.ini` | creato in FASE 0 con `2.0.0-SNAPSHOT` |
| Stack | Java 21, Spring Boot 3.4.0, Spring Cloud 2024.0.0 |

## 2. Dimensione del codice

3 classi Java (piu' il marker `package-info.java`), 257 righe totali di file:

| File | Righe |
| --- | --- |
| `src/main/java/com/tanadelgigante/civilservant/CivilServantApplication.java` | 187 |
| `src/main/java/com/tanadelgigante/civilservant/ServiceConfigHelper.java` | 48 |
| `src/main/java/com/tanadelgigante/civilservant/EndpointController.java` | 22 |
| `src/main/java/com/tanadelgigante/civilservant/package-info.java` | 0 |

## 3. I 5 difetti bloccanti (WORKPLAN §2.5)

| # | Difetto | Evidenza | Risolta in |
| --- | --- | --- | --- |
| 1 | Route dinamiche in loop: destinazione hardcoded a `http://localhost:8187`, la porta del gateway | `CivilServantApplication.java:154` | FASE 5 |
| 2 | Route dinamiche mai pubblicate: discovery dopo `SpringApplication.run()`, registry vuoto alla costruzione del `RouteLocator` | `CivilServantApplication.java:34-39` contro `:147-159` | FASE 3 |
| 3 | `ServiceConfigHelper` singleton con stato: route associate al JSON sbagliato; campi assenti letti come `""` senza errore | `ServiceConfigHelper.java:12, :17-22, :24-27`; `CivilServantApplication.java:161-162, :124` | FASE 3 |
| 4 | Nessun lifecycle e stderr scartato: `Process` mai `destroy()`-ato, stderr non rediretto | `CivilServantApplication.java:128, :114, :135` | FASE 5 + FASE 6 |
| 5 | `setup.sh` obbligatorio ma exit code non controllato | `CivilServantApplication.java:102-105, :119` | FASE 4 |

I riferimenti `file:righe` restano validi: la FASE 0 non sposta ne' modifica i file
sorgente Java.

## 4. Verifica di non regressione (Task 0.6)

Compilazione eseguita con l'immagine builder `maven:3.9.11-eclipse-temurin-21`
(la stessa introdotta in Task 0.7), montando il repository in `/app`:

| Momento | Comando | Esito |
| --- | --- | --- |
| Prima della pulizia | `mvn -B compile` | BUILD SUCCESS |
| Dopo la pulizia | `mvn -B compile` | BUILD SUCCESS |

Il criterio e' "compila e avvia", **non** "funziona": i difetti 1-3 restano
bloccanti per costruzione (WORKPLAN §2.5).

## 5. File rimossi (Task 0.2 e 0.3)

Tracciati in git (rimossi con `git rm`):

- `.classpath`
- `.project`
- `.settings/org.eclipse.jdt.core.prefs`
- `.settings/org.eclipse.m2e.core.prefs`
- `.mvn/wrapper/MavenWrapperDownloader.java`
- `.mvn/wrapper/maven-wrapper.jar`
- `.mvn/wrapper/maven-wrapper.properties`
- `mvnw`
- `mvnw.cmd`

Non tracciato in git (rimosso dal disco):

- `bin/` — copia obsoleta dell'intero progetto (coperta da `/bin/` in `.gitignore`)

`.mvn/`, `mvnw` e `mvnw.cmd` non sono piu' presenti ne' tracciati (A11, R97).

## 6. Dockerfile (Task 0.7)

Prima:

```
FROM eclipse-temurin:21-jdk as build
COPY .mvn/ .mvn/
COPY mvnw .
RUN ./mvnw package -DskipTests
RUN ls -l /app/target
```

Dopo:

```
FROM maven:3.9.11-eclipse-temurin-21 AS build
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests
```

Il builder e' un'immagine dedicata che contiene Maven, con tag bloccato (mai
`latest`): la rimozione del wrapper non lascia il progetto non costruibile
(R98, R99, RSK50). Il runtime resta `eclipse-temurin:21-jre`.
