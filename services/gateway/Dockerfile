# Multi-stage Dockerfile — pattern for every ShowKart service.
# Build stage compiles the specific module; runtime stage runs the layered JAR on Alpine JRE.

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
RUN chmod +x mvnw && ./mvnw -pl services/gateway -am -DskipTests dependency:go-offline -B || true
COPY common/ common/
COPY services/gateway/ services/gateway/
RUN ./mvnw -pl services/gateway -am -DskipTests package -B \
 && cd services/gateway/target \
 && java -Djarmode=tools -jar gateway.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S showkart && adduser -S showkart -G showkart
COPY --from=build --chown=showkart:showkart /workspace/services/gateway/target/extracted/dependencies/ ./
COPY --from=build --chown=showkart:showkart /workspace/services/gateway/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=showkart:showkart /workspace/services/gateway/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=showkart:showkart /workspace/services/gateway/target/extracted/application/ ./
USER showkart
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
