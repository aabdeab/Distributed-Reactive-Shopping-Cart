
FROM openjdk:17-jdk-slim

COPY build.gradle .
COPY gradle ./gradle
COPY gradlew .

COPY src ./src

RUN ./gradlew build -x test

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "build/libs/*.jar"]