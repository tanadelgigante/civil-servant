# Note di upgrade stack 2.0.0 (FASE 1)

Documento dei breaking change introdotti dalla **FASE 1** (WORKPLAN Rev. 8, Task 1.5).
Coppia di versioni verificata e fissata in `pom.xml`:

| Componente | Prima (baseline) | Dopo (FASE 1) |
| --- | --- | --- |
| Java | 21 | **25** (LTS) |
| Spring Boot | 3.4.0 | **4.0.7** |
| Spring Cloud | 2024.0.0 | **2025.1.2** (train Oakwood) |
| Immagine builder | `maven:3.9.11-eclipse-temurin-21` | **`maven:3.9.11-eclipse-temurin-25`** |
| Immagine runtime | `eclipse-temurin:21-jre` | **`eclipse-temurin:25-jre`** |

La coppia **Spring Boot 4.0.7 / Spring Cloud 2025.1.2** è quella dichiarata compatibile
dalla documentazione ufficiale del train 2025.1 (Oakwood), basato su Spring Framework 7
e Spring Boot 4.0.x.

## Breaking change impattanti sul codice e sulla configurazione

1. **Artifact di Spring Cloud Gateway rinominato.** Nel train 2025.1 l'artifact
   deprecato `spring-cloud-starter-gateway` è stato **rimosso**; il sostituto è
   **`spring-cloud-starter-gateway-server-webflux`**. Aggiornato in `pom.xml`.

2. **Prefisso delle proprietà del gateway.** Le proprietà del gateway WebFlux sono
   migrate da `spring.cloud.gateway.*` a **`spring.cloud.gateway.server.webflux.*`**.
   Aggiornato in `config/application.yml`; senza la migrazione le route statiche
   non vengono più pubblicate (regressione verificata: `404` invece di `5xx`).

3. **Jackson 2 → Jackson 3.** Spring Boot 4 usa Jackson 3 come libreria JSON
   predefinita: le classi si spostano da `com.fasterxml.jackson.databind.*` a
   **`tools.jackson.databind.*`**. Migrati gli import in `ServiceConfigHelper`.
   Rimosse le dipendenze Jackson 2 e SLF4J dichiarate inline (gestite dai BOM).

4. **Pulizia strutturale del `pom.xml`** (Task 1.1): `spring-boot-maven-plugin`
   spostato da `<dependencies>` a `<build><plugins>`; `spring-boot-dependencies`
   importato in `<dependencyManagement>`; nessuna versione inline sulle dipendenze;
   aggiunto `spring-boot-starter-test` in ambito `test`.

## Note di compatibilità

- **Warning di runtime (non deprecazioni).** Su Java 25 Netty emette il warning
  `A restricted method in java.lang.System has been called` /
  `--enable-native-access=ALL-UNNAMED`. Non è una deprecazione di Spring e non
  impedisce l'avvio. Da valutare in FASE 9 (hardening): si può silenziare con
  `--enable-native-access=ALL-UNNAMED` all'ENTRYPOINT.
- **Configurazione in `config/application.yml`**: il container la carica via
  `--spring.config.location`; non è impacchettata nel JAR.
