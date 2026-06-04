FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY backend/pom.xml backend/pom.xml
RUN mvn -pl backend -am dependency:go-offline

COPY backend backend
RUN mvn -pl backend -am package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/backend/target/fake-tcp-server-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 18080 14400
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
