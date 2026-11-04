FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S bank && adduser -S bank -G bank
COPY --from=build /workspace/target/teste-banco-digital-*.jar app.jar
USER bank
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
