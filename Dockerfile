# Stage 1: Build and package
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight OCI runtime image
FROM eclipse-temurin:21.0.13_9-jre-alpine
WORKDIR /app

# Importar certificado raíz de StyleRadar para conexión TLS a RabbitMQ (GJ-92)
RUN echo "-----BEGIN CERTIFICATE-----" > /tmp/ca.crt \
    && echo "MIIDRTCCAi2gAwIBAgIURd4+NHDLF76vdTDdgXHaQ42zyn0wDQYJKoZIhvcNAQEL" >> /tmp/ca.crt \
    && echo "BQAwMjEbMBkGA1UEAwwSU3R5bGVSYWRhci1Sb290LUNBMRMwEQYDVQQKDApTdHls" >> /tmp/ca.crt \
    && echo "ZVJhZGFyMB4XDTI2MTEwNzIxMzA0OFoXDTM2MTEwNDIxMzA0OFowMjEbMBkGA1UE" >> /tmp/ca.crt \
    && echo "AwwSU3R5bGVSYWRhci1Sb290LUNBMRMwEQYDVQQKDApTdHlsZVJhZGFyMIIBIjAN" >> /tmp/ca.crt \
    && echo "BgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAt+Qdgd4jxpTJyCLASAwj8UAPoo2a" >> /tmp/ca.crt \
    && echo "STZsoKITg1UJrJ+nXfhBhOFTzlZvr64J1/vDBBaMVN295m43nYea417uZK9jpfWG" >> /tmp/ca.crt \
    && echo "FTY1BwD8lwcRrEmwpiVySedVYBlQrejI48Lh4u/sZp92JYPOueHwsamRcOHD3Q6v" >> /tmp/ca.crt \
    && echo "wgliaxNAqJahytizCXzLamujDr0BAaRtiUUKz53fbqhIS7kYZwvL/5xEOuc2z54R" >> /tmp/ca.crt \
    && echo "+zWWiD/XLlSGMW794W+YZxYEpEncgvVuD3+ulS9WH9b1NfmPQd+ipX67I7IoGVE/" >> /tmp/ca.crt \
    && echo "6SbK5jbTMrAcN23Sf3MrmRkKaBdArwbZnOA2FrgZAeT9+HM31N/MtRlsTQIDAQAB" >> /tmp/ca.crt \
    && echo "o1MwUTAdBgNVHQ4EFgQUAr94nTXaj5eGsaSrXxsInMmsD50wHwYDVR0jBBgwFoAU" >> /tmp/ca.crt \
    && echo "Ar94nTXaj5eGsaSrXxsInMmsD50wDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0B" >> /tmp/ca.crt \
    && echo "AQsFAAOCAQEAH2GB/KMNm4906IRgkpNNapuBkNdLbL0OywwHsJJAadksKR/CMb3k" >> /tmp/ca.crt \
    && echo "l9Cv3K2z5VANAcMGzbTJG26hSDIxHorCJb5k4dj0p7S5G5ZQKumtcdLedxY8FNw1" >> /tmp/ca.crt \
    && echo "0vhj3nWQFluqLfnbodnVkQOLImLHVrnRB1uh5qGNTCVBJIwoCAAYRnviOOgQu2LV" >> /tmp/ca.crt \
    && echo "DPVHu9s/7sLaCX7EVbE7H3cot4lS8jPlcU2OsvOsuzTBP2VlJJfZ9YcsBzfK0Xtc" >> /tmp/ca.crt \
    && echo "O7RQPgQH1zfclr2QJytczAD3+69ALfvt6KG3Ym+QJkqvZz83tYbfpBUrhxZv/4IL" >> /tmp/ca.crt \
    && echo "pL2k1SIyZTwOPF6ki4qnTBuhM/KEnBAilQ==" >> /tmp/ca.crt \
    && echo "-----END CERTIFICATE-----" >> /tmp/ca.crt \
    && keytool -importcert -trustcacerts -file /tmp/ca.crt -alias styleradar-ca -keystore "$JAVA_HOME/lib/security/cacerts" -storepass changeit -noprompt \
    && rm -f /tmp/ca.crt

RUN addgroup -S styleradar && adduser -S appuser -G styleradar
USER appuser
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8300
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
