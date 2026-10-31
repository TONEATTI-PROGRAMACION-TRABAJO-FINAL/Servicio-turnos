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
| `CATEDRA_GROUP_ID` | `catedra.group-id` | `integration.groupId`. Va en cada evento que el servicio publica a la cátedra |
| `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` | `catedra.kafka.bootstrap-servers` | `integration.kafkaBootstrapServers` |
| `CATEDRA_KAFKA_CONSUMER_GROUP_ID` | `catedra.kafka.consumer-group-id` | `integration.kafkaConsumerGroupId` |
| `CATEDRA_KAFKA_APPOINTMENT_ACTIONS_TOPIC` | `catedra.kafka.appointment-actions-topic` | `integration.kafkaAppointmentActionsTopic` |
| `CATEDRA_KAFKA_APPOINTMENT_PHONE_TOPIC` | `catedra.kafka.appointment-phone-topic` | `integration.kafkaAppointmentPhoneTopic` |
| `CATEDRA_KAFKA_SECURITY_PROTOCOL` | `catedra.kafka.security-protocol` | Opcional. `PLAINTEXT` por defecto: el contrato no entrega credenciales de Kafka |
| `CATEDRA_KAFKA_AUTO_OFFSET_RESET` | `catedra.kafka.auto-offset-reset` | Opcional. `earliest` (por defecto) o `latest` |
| `CATEDRA_KAFKA_RETRY_MAX_ELAPSED_TIME` | `catedra.kafka.retry-max-elapsed-time` | Opcional. Cuánto se reintenta un evento cuyo procesamiento falla, en ISO-8601; por defecto `PT2M` |
| `CATEDRA_KAFKA_SEND_TIMEOUT` | `catedra.kafka.send-timeout` | Opcional. Cuánto espera la publicación de un evento a que el broker la confirme (incluye obtener su metadata), en ISO-8601; por defecto `PT5S` |
| `CATEDRA_KAFKA_LISTENER_ENABLED` | `catedra.kafka.listener-enabled` | Opcional. `false` deja el consumidor detenido; por defecto `true` |
| `CATALOGO_BASE_URL` | `catalogo.base-url` | URL base de Servicio-catalogo |
| `USER_JWT_SECRET` | `user.jwt.secret` | Secreto propio para firmar los JWT de los usuarios (HS256, mínimo 32 bytes). Se genera, por ejemplo, con `openssl rand -base64 48` |
| `USER_JWT_EXPIRATION` | `user.jwt.expiration` | Opcional. Vigencia del JWT de usuario en formato ISO-8601; por defecto `PT24H` |

Los campos `integration.*` los devuelven `POST /api/student/register`, `POST /api/authenticate` y `GET /api/student/integration`.

Todas son obligatorias salvo las marcadas como opcionales. Si falta alguna (o `USER_JWT_SECRET` tiene menos de 32 bytes), la aplicación no arranca y muestra `APPLICATION FAILED TO START` con la lista de propiedades faltantes.

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

Los tests no necesitan las variables reales. `src/test/resources/application-test.properties` define valores ficticios y el test de contexto activa el perfil `test`, así que `./mvnw clean verify` funciona igual en local y en GitHub Actions. El consumidor Kafka queda detenido en los tests, salvo en su test de integración, que lo prueba contra un broker embebido (`@EmbeddedKafka`): no hace falta Docker ni el entorno de la cátedra.

## Contraseñas

Las contraseñas de los usuarios se guardan hasheadas con **Argon2id** (`Argon2PasswordEncoder` de Spring Security, que delega el cálculo en BouncyCastle `bcprov-jdk18on`). Los parámetros son la configuración mínima que recomienda el [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#argon2id), con los largos de salt y de hash de la [RFC 9106](https://www.rfc-editor.org/rfc/rfc9106):

| Parámetro | Valor |
| --- | --- |
| Memoria | 19 MiB (19456 KiB) |
| Iteraciones | 2 |
| Paralelismo | 1 |
| Salt | 16 bytes, aleatoria por usuario |
| Hash | 32 bytes |

Cada hash tarda unos 30 ms. El hash guardado incluye sus parámetros (`$argon2id$v=19$m=19456,t=2,p=1$...`).

Al registrarse, la contraseña debe tener entre 8 y 128 caracteres; el login también rechaza más de 128. El máximo solo evita que contraseñas enormes encarezcan cada hash. Si no se cumple, la respuesta es un `400` en `application/problem+json`.

La base es H2 en memoria, así que no hay hashes BCrypt viejos que migrar: al reiniciar, todos los usuarios se registran de nuevo con Argon2id.

## Catálogo

La app habla solo con este servicio, así que turnos le expone las categorías y los profesionales que lee de Servicio-catalogo (`CATALOGO_BASE_URL`). No guarda copia ni caché: cada request consulta el catálogo, para que una baja que llega por la sincronización con la cátedra se vea enseguida.

### Endpoints

Todos requieren `Authorization: Bearer <jwt-de-usuario>`; sin un token válido la respuesta es `401`.

`GET /professional-categories` lista las categorías habilitadas. Las deshabilitadas las excluye Servicio-catalogo.

```json
[
  { "id": 1, "name": "Clinica medica", "description": "Atencion clinica general" }
]
```

Si no hay categorías, `200 OK` con `[]`.

`GET /professionals` lista los profesionales habilitados. Con `categoryId` (query, entero, opcional), solo los de esa categoría; sin él, los de todas.

```json
[
  { "id": 15, "categoryId": 1, "firstName": "Ana", "lastName": "Gomez", "enabled": true }
]
```

Una categoría sin profesionales habilitados, o inexistente, devuelve `200 OK` con `[]`. Un `categoryId` no numérico responde `400`.

Turnos descarta los profesionales con `enabled: false` aunque el catálogo ya no los lista. Para excluir a los profesionales de una **categoría deshabilitada** confía en Servicio-catalogo: el listado hace una sola llamada y no cruza los profesionales con las categorías.

`GET /professionals/{id}` devuelve un profesional, **habilitado o no**, con el mismo cuerpo que el listado.

```json
{ "id": 18, "categoryId": 1, "firstName": "Luis", "lastName": "Diaz", "enabled": false }
```

Un profesional deshabilitado responde `200 OK` con `enabled: false`: la app lo necesita para mostrar el nombre en reservas anteriores, y no le ofrece horarios (si los pide, `GET /available-slots` responde `400 PROFESSIONAL_DISABLED`). Un id inexistente responde `404`; uno no numérico, `400`.

### Errores

En `application/problem+json` con un `code` estable, la misma convención de la cátedra.

| HTTP | `code` | Cuándo |
| --- | --- | --- |
| `404` | `PROFESSIONAL_NOT_FOUND` | `GET /professionals/{id}` de un id que Servicio-catalogo no conoce |
| `503` | `SERVICE_UNAVAILABLE` | Servicio-catalogo no respondió, respondió un error o una respuesta ilegible. El detalle queda solo en el log |

## Reservas

Un usuario autenticado inicia una reserva eligiendo un slot. El servicio pide el hold a la cátedra (`POST /api/appointment-holds`) y lo confirma enseguida (`POST /api/appointment-holds/{holdId}/confirm`), porque entre los dos pasos la app no tiene nada que agregar. El paso del teléfono (Kafka) llega después.

El paciente es siempre el usuario del JWT: el `externalPatientId` es su `externalId` (el `sub` del token) y el nombre y el apellido salen de su registro. Lo que mande la app en esos campos se ignora.

### Endpoints

Todos requieren `Authorization: Bearer <jwt-de-usuario>`; sin un token válido la respuesta es `401`.

`POST /reservations`

```json
{ "professionalId": 15, "date": "2026-09-14", "startTime": "09:30" }
```

| Respuesta | Cuándo |
| --- | --- |
| `201 Created` con `Location: /reservations/{id}` | Se creó el proceso y quedó esperando el teléfono |
| `200 OK` | El usuario ya tenía un proceso activo para ese slot y se continuó ese (un doble toque o un reintento no piden otro hold) |

```json
{
  "id": 7,
  "professionalId": 15,
  "date": "2026-09-14",
  "startTime": "09:30",
  "status": "WAITING_FOR_PHONE",
  "expiresAt": "2026-09-10T14:44:50Z",
  "failureCode": null,
  "phoneRejectionReason": null,
  "cancellationReason": null,
  "createdAt": "2026-09-10T14:30:00Z"
}
```

`GET /reservations/{id}` devuelve el mismo cuerpo. Solo encuentra los procesos del usuario: el de otro usuario responde `404` como si no existiera. No llama a la cátedra.

`POST /reservations/{id}/phone` envía a la cátedra el teléfono que pidió (`AdditionalInformationRequested`). Solo se acepta mientras el proceso está en `WAITING_FOR_PHONE` y antes de su `expiresAt`.

```json
{ "phoneNumber": "+54 9 (261) 555-1234" }
```

Se quitan espacios, guiones, paréntesis y puntos, y lo que queda tiene que cumplir `^\+?[0-9]{7,15}$`, la misma validación que hace la cátedra. Responde `202 Accepted` con el proceso, que sigue en `WAITING_FOR_PHONE`: la cátedra contesta después por Kafka, y la app ve el resultado con `GET /reservations/{id}` (`CONFIRMED`, o `phoneRejectionReason` si lo rechazó). Al enviar se borra el `phoneRejectionReason` del teléfono anterior, para que la app no lo tome como respuesta al nuevo. Después de un rechazo se puede enviar otro teléfono hasta `expiresAt`.

El teléfono **no se guarda**: es un dato personal y solo viaja en el evento a la cátedra. Tampoco aparece en los logs.

El resultado del proceso según la cátedra, `null` cuando no corresponde:

| Campo | Cuándo tiene valor |
| --- | --- |
| `failureCode` | `FAILED` o `EXPIRED`: el `code` con que la cátedra rechazó el hold o la confirmación, o el `reason` de `AppointmentProcessInvalid` (por ejemplo `REQUEST_EVENT_MISMATCH`) o de `AppointmentProcessExpired` (`HOLD_EXPIRED`) |
| `phoneRejectionReason` | La cátedra rechazó el último teléfono (`AdditionalInformationRejected`, por ejemplo `INVALID_PHONE_NUMBER`). Mientras el estado siga en `WAITING_FOR_PHONE`, la app puede pedir otro hasta `expiresAt` |
| `cancellationReason` | `CANCELLED`, si la cancelación tuvo motivo |

Los ids de la cátedra (`holdId`, `reservationProcessId`, el `requestEventId` del pedido de teléfono y el `reservationId` de la reserva confirmada) se guardan pero no se exponen.

### Estados

```
PENDING_HOLD ──▶ HELD ──▶ CONFIRMING ──▶ WAITING_FOR_PHONE ──▶ CONFIRMED ──▶ CANCELLED
     │            │           │                 │
     └──▶ FAILED ◀┴───────────┴─────────────────┤
                  └──▶ EXPIRED ◀┴───────────────┘
```

| Estado | Significado |
| --- | --- |
| `PENDING_HOLD` | Se pidió el hold y todavía no se sabe si existe |
| `HELD` | La cátedra creó el hold |
| `CONFIRMING` | Se pidió la confirmación y todavía no se sabe si llegó |
| `WAITING_FOR_PHONE` | La cátedra confirmó el hold y espera el teléfono antes de `expiresAt`. Un teléfono rechazado no cambia el estado: se puede enviar otro hasta `expiresAt` |
| `CONFIRMED` | La cátedra confirmó la reserva (`AppointmentConfirmed`) |
| `CANCELLED` | La reserva confirmada se canceló (`AppointmentCancelled`) |
| `FAILED` | La cátedra rechazó el hold o la confirmación, o informó el proceso como inválido (`AppointmentProcessInvalid`); se guarda su `code` o `reason` |
| `EXPIRED` | El hold o el proceso vencieron (`AppointmentProcessExpired`, con su `reason`) |

`FAILED`, `EXPIRED` y `CANCELLED` son finales: una respuesta o un evento tardío no los reabre, y el usuario puede iniciar otra reserva para el mismo slot. `CONFIRMED` no es final, porque todavía se puede cancelar, pero ningún evento tardío lo hace volver atrás, vencer ni fallar; iniciar de nuevo la reserva del mismo slot la devuelve tal como está. Cada estado se guarda **antes** de la llamada a la cátedra que lo deja, así que después de un timeout queda registrado qué pudo haber pasado. La vigencia del hold es la `expiresAt` de la cátedra; si ya pasó, el proceso se marca `EXPIRED` sin volver a llamarla.

Un usuario tiene como máximo un proceso activo por slot (profesional, fecha y hora). Lo garantiza una restricción única en `reservation_processes` sobre `(user_external_id, professional_id, slot_date, start_time, active)`, donde `active` vale `TRUE` mientras el proceso no es final y `NULL` después: la restricción trata los `NULL` como distintos, así que admite cualquier cantidad de procesos terminados y uno solo activo.

### Errores

Todos en `application/problem+json` con un `code` estable, la misma convención de la cátedra.

| HTTP | `code` | Cuándo |
| --- | --- | --- |
| `400` | `VALIDATION_ERROR` | Falta un campo, el id no es positivo o una fecha u hora tiene otro formato |
| `400` | `SLOT_IN_THE_PAST` | El slot ya empezó |
| `400` | `PROFESSIONAL_DISABLED` | La cátedra rechazó el hold: profesional deshabilitado |
| `400` | `INVALID_SLOT` | La cátedra rechazó el hold o la confirmación: el slot no está en la agenda |
| `404` | `PROFESSIONAL_NOT_FOUND` | La cátedra no conoce al profesional |
| `404` | `USER_NOT_FOUND` | El usuario del token ya no existe |
| `400` | `INVALID_PHONE_NUMBER` | El teléfono, sin separadores, no tiene entre 7 y 15 dígitos con un `+` opcional al principio. No se envía |
| `404` | `RESERVATION_NOT_FOUND` | `GET` o envío del teléfono de un proceso inexistente o de otro usuario |
| `409` | `SLOT_ALREADY_RESERVED` | El slot ya tiene una reserva confirmada |
| `409` | `SLOT_ALREADY_HELD` | El slot tiene un hold activo de otro proceso |
| `409` | `RESERVATION_IN_PROGRESS` | Otro request del mismo usuario está reservando ese slot en este momento. Si lo que cambió el proceso fue un evento de la cátedra que lo dejó en `WAITING_FOR_PHONE` o `CONFIRMED`, no hay conflicto: se devuelve ese proceso |
| `409` | `RESERVATION_NOT_WAITING_FOR_PHONE` | Se envió un teléfono a un proceso que no está en `WAITING_FOR_PHONE` |
| `409` | `PHONE_NOT_REQUESTED_YET` | El proceso está en `WAITING_FOR_PHONE` pero todavía no llegó el `AdditionalInformationRequested`, cuyo `eventId` hace falta para enviar el teléfono. Reintentar en unos segundos |
| `410` | `HOLD_EXPIRED` | El hold venció antes de confirmarse |
| `410` | `RESERVATION_EXPIRED` | Se envió un teléfono después del `expiresAt` del proceso. El estado lo cierra la cátedra con `AppointmentProcessExpired` |
| `410` | `HOLD_NOT_FOUND` | La cátedra ya no encuentra el hold |
| `500` | `INTERNAL_ERROR` | La cátedra rechazó algo que este servicio nunca debería mandar (`VALIDATION_ERROR`, `HOLD_OWNER_MISMATCH`, `EXTERNAL_PATIENT_MISMATCH`); queda en el log como error |
| `503` | `SERVICE_UNAVAILABLE` | La cátedra no encuentra la integración técnica (`STUDENT_INTEGRATION_NOT_FOUND`): hay que revisar `CATEDRA_JWT` |
| `503` | `PHONE_SUBMISSION_UNAVAILABLE` | Kafka no confirmó el envío del teléfono dentro de `CATEDRA_KAFKA_SEND_TIMEOUT`. El proceso sigue esperando el teléfono y se puede enviar de nuevo |
| `503` | `RESERVATION_PENDING` | La cátedra no respondió y no se sabe si la operación tuvo efecto; incluye `reservationId`. Repetir el mismo `POST` continúa ese proceso |

`RESERVATION_PROCESS_ALREADY_CONFIRMED` no llega a la app: significa que una confirmación anterior del mismo proceso sí llegó, y se trata como éxito.

### Timeouts y reintentos

Un timeout, un `5xx` o una respuesta ilegible no prueban que la operación haya fallado (sección 18.4 del contrato). Antes de volver a llamar, el servicio reconcilia con los datos que guardó, y en cada request hace como máximo **dos** llamadas de cada tipo. Cada intento fallido queda en el log con el id del proceso y el número de intento.

- **Hold sin respuesta.** El contrato no permite leer un hold, así que se consultan las ocupaciones del slot (`GET /api/appointment-occupancies`). Si el slot está libre, el hold no se creó y se pide de nuevo. Si está `CONFIRMED`, el proceso falla con `SLOT_ALREADY_RESERVED`. Si está `HELD`, **no se puede saber si el hold es nuestro o de otro**: pedirlo otra vez respondería `SLOT_ALREADY_HELD` en ambos casos. El proceso queda en `PENDING_HOLD` y la app recibe `RESERVATION_PENDING`; cuando la cátedra libere ese hold (en su `expiresAt`), un nuevo `POST` lo pide de nuevo. Esta es una limitación del contrato, no del servicio.
- **Confirmación sin respuesta.** Se buscan las reservas del paciente para ese día (`GET /api/appointments`). Si aparece el `reservationProcessId` del proceso, la confirmación llegó y no se repite. Si no aparece, se confirma una vez más; si la primera llegó entretanto, la cátedra responde `RESERVATION_PROCESS_ALREADY_CONFIRMED` y se toma como éxito.

Los timeouts del cliente REST de la cátedra son `PT2S` de conexión y `PT5S` de lectura por defecto; `CATEDRA_CONNECT_TIMEOUT` y `CATEDRA_READ_TIMEOUT` los cambian (duración ISO-8601).

### Eventos de la cátedra (Kafka)

Después de la confirmación, el proceso avanza con los eventos que la cátedra publica en `CATEDRA_KAFKA_APPOINTMENT_ACTIONS_TOPIC` (secciones 15.4 y 15.6 a 15.10 del contrato): `AdditionalInformationRequested` guarda su `eventId` como `requestEventId` (lo necesita el envío del teléfono), y los demás llevan el proceso a los estados de la tabla de arriba.

- **Al menos una vez.** El offset se confirma recién después de procesar cada mensaje, nunca automáticamente. Cada `eventId` procesado se guarda en `processed_appointment_actions` (con restricción única), en la misma transacción que el cambio del proceso: un mensaje repetido se reconoce y no repite efectos (sección 18.1).
- **Eventos tardíos.** Un evento que el estado actual no admite (por ejemplo, una confirmación sobre un proceso `EXPIRED`) se registra y se ignora: no reabre un estado final ni hace retroceder una reserva confirmada (sección 18.3).
- **Proceso desconocido.** Un evento de un `reservationProcessId` que no existe localmente se registra y se ignora.
- **Mensaje ilegible.** JSON inválido, sin `eventId`, `eventType` o `reservationProcessId`, con un `eventType` desconocido o con un `schemaVersion` distinto de `1`: se deja en el log (`WARN`, con partición y offset) y se saltea, sin reintentar. Los campos desconocidos se ignoran.
- **Error al procesar.** Si falla aplicar un evento legible (por ejemplo, la base no responde o otro request cambió el proceso), el mensaje se reintenta en el lugar con espera exponencial (1 s, 2 s, 4 s… hasta 30 s entre intentos) durante `CATEDRA_KAFKA_RETRY_MAX_ELAPSED_TIME` (2 minutos por defecto). Mientras tanto la partición espera.

**Envío del teléfono.** `AdditionalInformationSubmitted` se publica en `CATEDRA_KAFKA_APPOINTMENT_PHONE_TOPIC` (sección 15.5) con message key `reservationProcessId`, un `eventId` nuevo en cada envío, el `requestEventId` guardado, `groupId` de `CATEDRA_GROUP_ID` y `schemaVersion` `1`. El envío es síncrono: se espera que todas las réplicas lo confirmen (`acks=all`, productor idempotente) durante como máximo `CATEDRA_KAFKA_SEND_TIMEOUT` (`PT5S` por defecto) para obtener la metadata del broker y otro tanto para la confirmación. El proceso se guarda antes de publicar; si un evento de la cátedra lo cambió entretanto, no se publica nada y la app recibe `409 RESERVATION_IN_PROGRESS`. Un timeout no prueba que el teléfono no haya llegado (sección 18.4): si la app lo reenvía y la cátedra ya había confirmado con el primero, el evento que responda al segundo llega tarde y se ignora.

**Limitación.** Si el evento sigue fallando al agotar los reintentos, se registra en el log como `ERROR` con su contenido completo y **se saltea**, para no bloquear la partición indefinidamente. Ese evento no se aplica: el contrato no define un topic de errores (DLT) donde dejarlo. El estado persistido en la cátedra se puede reconciliar por REST (`GET /api/appointments`, sección 11); esa reconciliación todavía no está implementada.
