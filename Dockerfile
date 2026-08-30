# Stage 1: Build the Java Spark fat jar using Maven
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml and resolve dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build final jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the built jar from Stage 1
COPY --from=builder /app/target/*-with-dependencies.jar app.jar || COPY --from=builder /app/target/*.jar app.jar

# Render supplies port via the PORT environment variable
ENV PORT=8080
EXPOSE 8080

# Execute the Spark application
ENTRYPOINT ["sh", "-c", "java -jar app.jar"]