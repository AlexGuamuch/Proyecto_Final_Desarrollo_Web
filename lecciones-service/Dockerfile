# Imagen del servicio de lecciones (fase 1).
# Construcción en dos etapas: Maven compila el jar y la imagen final solo lleva el JRE.

# ---------- Etapa 1: compilación ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Primero solo el pom: si no cambian las dependencias, esta capa queda en caché.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Las pruebas corren en GitHub Actions en cada push; aquí se omiten para acelerar el build en la nube.
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: ejecución ----------
FROM eclipse-temurin:21-jre

# Usuario sin privilegios: el proceso nunca corre como root.
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=build /build/target/lecciones-service.jar app.jar

USER 10001:10001

# La nube define PORT; 8080 es el valor por defecto (server.port=${PORT:8080}).
ENV PORT=8080
# Pensado para 512 MB y CPU compartida (plan gratuito): heap al 75 % del límite del contenedor,
# recolector serial y compilación JIT rápida para arrancar antes.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
