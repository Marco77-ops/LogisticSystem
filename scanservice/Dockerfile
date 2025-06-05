# ------------ build stage ------------
FROM maven:3.9.0-eclipse-temurin-17 AS build
WORKDIR /workspace

# Zuerst nur die pom.xml für optimalen Cache
COPY pom.xml .
COPY scanservice/pom.xml scanservice/
COPY shared/pom.xml shared/
COPY shipmentservice/pom.xml shipmentservice/
COPY deliveryservice/pom.xml deliveryservice/
COPY analyticservice/pom.xml analyticservice/
COPY notificationviewservice/pom.xml notificationviewservice/

# Dependencies für alle Module vorab auflösen (maximaler Cache!)
RUN mvn de.qaware.maven:go-offline-maven-plugin:1.2.8:resolve-dependencies

# Jetzt den kompletten Quellcode kopieren (erst ab hier invalidiert jede Codeänderung den Layer)
COPY . .

# Jetzt das gewünschte Service-Modul bauen
RUN mvn clean package -pl scanservice -am -DskipTests

# ------------ runtime stage -----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# NUR das fertige JAR ins Runtime-Image kopieren (kleines, sauberes Image!)
COPY --from=build /workspace/scanservice/target/scanservice-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
