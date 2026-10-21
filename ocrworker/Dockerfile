FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY src/ src/
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/ocrworker-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
