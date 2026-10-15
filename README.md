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

Los campos `integration.*` los devuelven `POST /api/student/register`, `POST /api/authenticate` y `GET /api/student/integration`.

Todas son obligatorias. Si falta alguna, la aplicación no arranca y muestra `APPLICATION FAILED TO START` con la lista de propiedades faltantes.

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

El archivo `.env` contiene secretos (JWT técnico): **nunca se commitea**, ni se pega en capturas, issues o logs.

### Tests

Los tests no necesitan las variables reales. `src/test/resources/application-test.properties` define valores ficticios y el test de contexto activa el perfil `test`, así que `./mvnw clean verify` funciona igual en local y en GitHub Actions.
