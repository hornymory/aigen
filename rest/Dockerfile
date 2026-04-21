FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY mvnw ./mvnw
COPY .mvn ./.mvn
COPY pom.xml ./pom.xml

RUN chmod +x ./mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
