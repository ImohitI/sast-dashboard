FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests && cp target/dashboard-api-*.jar /app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
