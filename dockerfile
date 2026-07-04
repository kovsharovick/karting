# Первый этап: сборка с Maven и JDK 17
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Копируем pom.xml и загружаем зависимости (кэшируем, чтобы не качать каждый раз)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код и собираем приложение
COPY src ./src
RUN mvn clean package -DskipTests

# Второй этап: лёгкий образ с JRE 17 для запуска
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Копируем собранный jar из первого этапа
COPY --from=build /app/target/karting-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]