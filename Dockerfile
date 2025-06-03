FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY pom.xml .  
COPY src ./src

RUN apt-get update && apt-get install -y maven \
    && mvn clean package -DskipTests \
    && apt-get remove -y maven \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

ENTRYPOINT ["java", "-jar", "target/app.jar"]
