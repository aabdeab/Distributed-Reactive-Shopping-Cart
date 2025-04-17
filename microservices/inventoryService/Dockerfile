FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /extracted
COPY build/libs/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /extracted/dependencies/ ./
COPY --from=builder /extracted/spring-boot-loader/ ./
COPY --from=builder /extracted/snapshot-dependencies/ ./
COPY --from=builder /extracted/application/ ./
ENV SERVER_PORT=8082
ENV SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
EXPOSE $SERVER_PORT
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]