# Imagen de un servicio. Se construye desde la raiz del repo indicando cual:
#   docker build --build-arg SERVICE=servicio-clientes -t servicio-clientes .
FROM eclipse-temurin:17-jdk AS build
ARG SERVICE
WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
COPY servicio-clientes servicio-clientes
COPY servicio-inventario servicio-inventario
COPY servicio-pedidos servicio-pedidos
# El checkout en Windows puede dejar fines de linea CRLF en gradlew.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew \
    && ./gradlew :${SERVICE}:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
ARG SERVICE
WORKDIR /app
COPY --from=build /workspace/${SERVICE}/build/libs/*-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
