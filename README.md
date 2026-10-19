<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-50%2F100-yellow) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey) ![Build](https://img.shields.io/badge/Build-OK-green) ![Duplicación](https://img.shields.io/badge/Duplicaci%C3%B3n-0.0%25-green) ![%POO](https://img.shields.io/badge/%25POO-56%25-orange) ![Sueltas](https://img.shields.io/badge/Sueltas-0-green) ![D1](https://img.shields.io/badge/D1-58-orange) ![D2](https://img.shields.io/badge/D2-36-red) ![D4](https://img.shields.io/badge/D4-0-red) ![D5](https://img.shields.io/badge/D5-95-green) ![D6](https://img.shields.io/badge/D6-48-orange) ![D7](https://img.shields.io/badge/D7-62-yellow)

**Calidad (heurístico, 7 dimensiones):** índice **50/100** · build **OK** · aprobar **no (7/11)** · destacar **no**
Detalle completo en [`docs/EVALUACION.md`](docs/EVALUACION.md).
<!-- calidad:fin -->

# Servicio-turnos

## Configuración del entorno local

El servicio se integra con el servicio central de la cátedra (REST y Kafka) y consume Servicio-catalogo por REST. Los datos de conexión se leen **solo de variables de entorno**: no hay valores reales en el código ni en el repositorio.

### Variables

| Variable | Propiedad | De dónde sale |
| --- | --- | --- |
| `CATEDRA_API_BASE_URL` | `catedra.api-base-url` | URL base de la API REST, la envía la cátedra |
| `CATEDRA_JWT` | `catedra.jwt` | `id_token` de la cuenta técnica (registro o login), el mismo que usa Servicio-catalogo |
| `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` | `catedra.kafka.bootstrap-servers` | `integration.kafkaBootstrapServers` |
| `CATEDRA_KAFKA_CONSUMER_GROUP_ID` | `catedra.kafka.consumer-group-id` | `integration.kafkaConsumerGroupId` |
| `CATEDRA_KAFKA_APPOINTMENT_ACTIONS_TOPIC` | `catedra.kafka.appointment-actions-topic` | `integration.kafkaAppointmentActionsTopic` |
| `CATEDRA_KAFKA_APPOINTMENT_PHONE_TOPIC` | `catedra.kafka.appointment-phone-topic` | `integration.kafkaAppointmentPhoneTopic` |
| `CATALOGO_BASE_URL` | `catalogo.base-url` | URL base de Servicio-catalogo |
| `USER_JWT_SECRET` | `user.jwt.secret` | Secreto propio para firmar los JWT de los usuarios (HS256, mínimo 32 bytes). Se genera, por ejemplo, con `openssl rand -base64 48` |
| `USER_JWT_EXPIRATION` | `user.jwt.expiration` | Opcional. Vigencia del JWT de usuario en formato ISO-8601; por defecto `PT24H` |

Los campos `integration.*` los devuelven `POST /api/student/register`, `POST /api/authenticate` y `GET /api/student/integration`.

Todas son obligatorias salvo `USER_JWT_EXPIRATION`. Si falta alguna (o `USER_JWT_SECRET` tiene menos de 32 bytes), la aplicación no arranca y muestra `APPLICATION FAILED TO START` con la lista de propiedades faltantes.

### Pasos

1. Copiar el archivo de ejemplo y completar los valores reales:
   ```bash
   cp .env.example .env
   ```
2. Cargar las variables en la terminal y levantar el servicio:
   ```bash
   set -a; source .env; set +a
   ./mvnw spring-boot:run
   ```
   En IntelliJ también se pueden cargar desde *Run Configuration → Environment variables*.

El archivo `.env` contiene secretos (JWT técnico y secreto del JWT de usuario): **nunca se commitea**, ni se pega en capturas, issues o logs.

### Tests

Los tests no necesitan las variables reales. `src/test/resources/application-test.properties` define valores ficticios y el test de contexto activa el perfil `test`, así que `./mvnw clean verify` funciona igual en local y en GitHub Actions.
