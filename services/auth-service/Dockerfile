# Multi-stage Dockerfile — same shape as services/gateway/Dockerfile.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY common/pom.xml common/
COPY services/gateway/pom.xml services/gateway/
COPY services/auth-service/pom.xml services/auth-service/
COPY services/show-service/pom.xml services/show-service/
COPY services/booking-service/pom.xml services/booking-service/
COPY services/payment-service/pom.xml services/payment-service/
COPY services/notification-worker/pom.xml services/notification-worker/
RUN chmod +x mvnw && ./mvnw -pl services/auth-service -am -DskipTests dependency:go-offline -B || true
COPY common/ common/
COPY services/auth-service/ services/auth-service/
RUN ./mvnw -pl services/auth-service -am -DskipTests package -B \
 && cd services/auth-service/target \
 && java -Djarmode=tools -jar auth-service.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S showkart && adduser -S showkart -G showkart
COPY --from=build --chown=showkart:showkart /workspace/services/auth-service/target/extracted/dependencies/ ./
COPY --from=build --chown=showkart:showkart /workspace/services/auth-service/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=showkart:showkart /workspace/services/auth-service/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=showkart:showkart /workspace/services/auth-service/target/extracted/application/ ./
USER showkart
EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
