FROM node:24-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npx ng build

# nginx-Container für die Web-UI
FROM nginx:alpine AS webui
COPY --from=frontend /frontend/dist/frontend/browser /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

FROM eclipse-temurin:25-jdk AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
# Spring Boot serves the Angular build from classpath:/static
COPY --from=frontend /frontend/dist/frontend/browser/ src/main/resources/static/
RUN ./mvnw -B -DskipTests package

# Backend-Container
FROM eclipse-temurin:25-jre AS app
WORKDIR /app
COPY --from=build /build/target/swendoc-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
