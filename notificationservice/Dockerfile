FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/notificationservice-*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]