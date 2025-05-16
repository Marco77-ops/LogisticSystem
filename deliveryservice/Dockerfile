# ------------ build stage ------------
FROM maven:3.9.0-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn clean package -pl deliveryservice -am -DskipTests

# ------------ runtime stage -----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/deliveryservice/target/deliveryservice-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
