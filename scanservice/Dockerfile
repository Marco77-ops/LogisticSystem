# ------------ build stage ------------
FROM maven:3.9.0-eclipse-temurin-17 AS build
WORKDIR /workspace


COPY pom.xml .
COPY scanservice/pom.xml scanservice/
COPY shared/pom.xml shared/
COPY shipmentservice/pom.xml shipmentservice/
COPY deliveryservice/pom.xml deliveryservice/
COPY analyticservice/pom.xml analyticservice/
COPY notificationviewservice/pom.xml notificationviewservice/


RUN mvn de.qaware.maven:go-offline-maven-plugin:1.2.8:resolve-dependencies


COPY . .


RUN mvn clean package -pl scanservice -am -DskipTests

# ------------ runtime stage -----------
FROM eclipse-temurin:17-jre
WORKDIR /app


COPY --from=build /workspace/scanservice/target/scanservice-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
