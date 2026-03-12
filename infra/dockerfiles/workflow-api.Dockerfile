FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY java-api/pom.xml ./java-api/pom.xml
RUN mvn -f java-api/pom.xml -DskipTests dependency:go-offline

COPY java-api ./java-api
RUN mvn -f java-api/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/java-api/target/workflow-api-*.jar /app/workflow-api.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/workflow-api.jar", "--spring.profiles.active=compose"]
