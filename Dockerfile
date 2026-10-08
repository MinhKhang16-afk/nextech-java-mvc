FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /work
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd -r shop && useradd -r -g shop shop && mkdir -p /data/uploads && chown -R shop:shop /data
COPY --from=builder /work/target/nextech-java-mvc-1.0.0.jar app.jar
USER shop
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
