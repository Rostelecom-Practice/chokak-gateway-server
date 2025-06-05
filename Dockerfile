# Build stage с Maven
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Только зависимости — кешируется
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходники и собираем
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage — только JRE
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app
COPY --from=builder /app/target/*.jar ./app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
