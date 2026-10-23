FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
ARG MODULE
RUN mvn -B -pl ${MODULE} -am package -DskipTests
RUN cp ${MODULE}/target/*.jar /app.jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
