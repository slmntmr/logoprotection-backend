# 🏗️ Maven tabanlı bir Java 17 görüntüsü kullan
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app

# 📂 Tüm dosyaları konteynere kopyala
COPY . .

# 🏗️ Projeyi derle
RUN mvn clean package -DskipTests

# 🚀 Java çalıştırmak için OpenJDK kullan
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 📦 Derlenmiş JAR dosyasını al
COPY --from=builder /app/target/*.jar app.jar

# 🔥 Konteyner çalıştırıldığında JAR dosyasını çalıştır
ENTRYPOINT ["java", "-jar", "app.jar"]
