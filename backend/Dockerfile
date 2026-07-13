# ---- Build ----
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Capa de dependencias cacheable: solo se reconstruye si cambia el pom.
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ---- Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# No corras como root.
RUN addgroup -S finsight && adduser -S finsight -G finsight
USER finsight

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Railway inyecta PORT; si no está, 8080.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar app.jar"]
