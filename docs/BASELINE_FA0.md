# Baseline FASE 0
- Branch: main
- HEAD: $(git -C /home/opencode/civilservant rev-parse --short HEAD)
- Remoti: origin -> tanadelgigante/civil-servant.git, upstream -> alessio/civil-servant.git
- Working tree pulito (churn CRLF/LF committato)
- File rimossi: .classpath, .project, .settings/, .mvn/, mvnw, mvnw.cmd, bin/
- .gitignore espanso
- Dockerfile corretto (builder con Maven 3.9.11-eclipse-temurin-25, mvn package, runtime eclipse-temurin:25-jre)
- Difetti bloccanti verificati (riferimenti su working tree): D1-D5 in CivilServantApplication/ServiceConfigHelper
