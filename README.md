#  Taxi Platform

## Автор
- Прозоренко К.В ИП-312

Учебный проект — backend микросервисного приложения для заказа такси на Java 25 / Spring Boot 4. Реализованы три самостоятельных сервиса, разделённых по доменам, с асинхронной очередью уведомлений и атомарным назначением водителей при конкурентных запросах.

---

## Содержание

- [Архитектура](#архитектура)
- [Стек технологий](#стек-технологий)
- [Структура репозитория](#структура-репозитория)
- [Быстрый старт](#быстрый-старт)
- [Сервисы и их API](#сервисы-и-их-api)
- [Ключевые инженерные решения](#ключевые-инженерные-решения)
- [База данных](#база-данных)
- [Конфигурация](#конфигурация)
- [Тестирование](#тестирование)
---

## Архитектура

```
                       ┌──────────────┐
                       │    Client    │
                       └──────┬───────┘
                              │ HTTP + JWT
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
┌───────▼────────┐   ┌────────▼─────────┐   ┌───────▼──────────────┐
│  user-service  │   │   trip-service   │   │ notification-service │
│      :8081     │◄──┤      :8082       ├──►│         :8083        │
│ passenger/driver│   │ trip lifecycle  │   │  worker-pool queue   │
│ JWT issuer + DB │   │ pricing, stats   │   │  (4 потока)          │
│ Redis cache     │   │ scheduler (TTL)  │   │  retries + DLQ       │
└───────┬────────┘   └────────┬─────────┘   └──────────┬───────────┘
        │                     │                        │
        ▼                     ▼                        ▼
┌────────────────────────────────────────────────────────────────┐
│                       PostgreSQL 16                            │
│       passengers • drivers • trips • notification_tasks        │
└────────────────────────────────────────────────────────────────┘
                       │
                       ▼ (используется только user-service)
                ┌─────────────┐
                │   Redis 7   │
                │  cache TTL  │
                └─────────────┘
```

**Flow одного заказа:**

1. Клиент логинится в `user-service` → получает JWT.
2. Клиент шлёт `POST /trips` в `trip-service`.
3. `trip-service` атомарно резервирует свободного водителя через `user-service` (`SELECT FOR UPDATE SKIP LOCKED`).
4. Создаётся запись поездки со статусом `ASSIGNED`.
5. `trip-service` ставит две задачи уведомления (DRIVER + PASSENGER) в `notification-service`.
6. Пул из 4 воркеров параллельно тянет задачи из очереди и доставляет их.
7. Если водитель не подтвердил поездку за TTL (60с) — фоновый скедулер `trip-service` отменяет поездку и возвращает водителя в `AVAILABLE`.

---

## Стек технологий

### Базовый

| Слой | Технология |
|---|---|
| Язык | **Java 25** |
| Фреймворк | **Spring Boot 4.0** |
| Сборка | **Gradle 9** (Kotlin/Groovy DSL) |
| Контейнеризация | **Docker** + **Docker Compose** |
| Хранилище | **PostgreSQL 16** |
| Кэш | **Redis 7** |
| Миграции БД | **Flyway** |

### Spring-стек

- `spring-boot-starter-webmvc` — REST API
- `spring-boot-starter-data-jpa` + Hibernate
- `spring-boot-starter-security` + JWT
- `spring-boot-starter-validation` — Bean Validation
- `spring-boot-starter-actuator` — health, metrics
- `spring-boot-starter-cache` + Redis (только в `user-service`)
- `@Scheduled` — фоновые задачи (TTL отмены поездок)

### Контракты и кодогенерация

- **OpenAPI 3.1** — single source of truth для API ([openapi/](openapi/))
- `openapi-generator-gradle-plugin` 7.10 — генерирует интерфейсы контроллеров и DTO из YAML
- `springdoc-openapi-starter-webmvc-ui` 2.7 — рантайм Swagger UI

### Межсервисное взаимодействие

- **Spring HTTP Interface** (`@HttpExchange`) — декларативные HTTP-клиенты на `RestClient`
- **Resilience4j** 2.2 — Retry (3 попытки, exponential backoff) + Circuit Breaker (50% failure rate, sliding window 10)

### Аутентификация

- **JJWT** 0.12 — выпуск и парсинг JWT (HS256, общий `JWT_SECRET` между сервисами)
- Stateless — никаких сессий, токен живёт 1 час
- Системный токен для фоновых задач (роль `ADMIN`) — выпускается trip-service'ом для вызова user-service из `@Scheduled`

### Утилиты

- **Lombok** — boilerplate (геттеры, конструкторы, билдеры)
- **MapStruct** 1.6 — типобезопасный маппинг между DTO и Entity
- **Jackson** — сериализация (с `jackson-databind-nullable` для OpenAPI nullable)

### Тесты

- **JUnit 5** + Spring Boot Test
- **Testcontainers** 1.20 — реальные PostgreSQL и Redis в Docker для интеграционных тестов
- **MockMvc** + **spring-security-test** — слой контроллеров

---

## Структура репозитория

```
taxi-platform/
├── docker-compose.yml              # вся инфра + три сервиса
├── build.gradle                    # общие зависимости (Spring, OpenAPI, Lombok, MapStruct)
├── settings.gradle                 # multi-module конфиг
│
├── openapi/                        # ← API-first: источник истины
│   ├── user-service.yaml
│   ├── trip-service.yaml
│   └── notification-service.yaml
│
├── user-service/                   # :8081 — passengers, drivers, auth
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/
│       ├── main/java/com/taxi/user/
│       │   ├── controller/         # реализации сгенерированных интерфейсов
│       │   ├── service/            # бизнес-логика
│       │   ├── repository/         # Spring Data JPA + custom queries
│       │   ├── entity/             # JPA Entity
│       │   ├── mapper/             # MapStruct
│       │   ├── security/           # JWT
│       │   └── config/
│       └── main/resources/
│           ├── application.yml
│           └── db/migration/       # Flyway: V1, V2…
│
├── trip-service/                   # :8082 — trips lifecycle
│   └── src/main/java/com/taxi/trip/
│       ├── controller/
│       ├── service/                # TripService, UserGateway, NotificationGateway
│       ├── client/                 # HTTP-клиенты (Spring HTTP Interface)
│       ├── scheduler/              # StuckTripCleanupScheduler (TTL)
│       ├── repository/
│       ├── entity/
│       ├── exception/
│       └── ...
│
└── notification-service/           # :8083 — async queue + worker pool
    └── src/main/java/com/taxi/notification/
        ├── controller/
        ├── service/                # NotificationService, DeliveryGateway
        ├── worker/                 # WorkerPool, TaskClaimer
        ├── repository/
        ├── entity/
        └── ...
```

> Сгенерированный из OpenAPI код кладётся в `*/build/generated/` и **в Git не коммитится**. Генерация запускается автоматически перед `compileJava`.

---

## Быстрый старт

### Требования

- Docker + Docker Compose
- (для локальной разработки) JDK 21+ и Gradle 8+

### Запуск

```bash
# Поднять весь стек
docker compose up -d

# Логи всех сервисов
docker compose logs -f

# Логи конкретного сервиса
docker compose logs -f trip-service
```

После старта (около 30–60 секунд) доступны:

| Что | Где |
|---|---|
| User Service | http://localhost:8081 |
| Trip Service | http://localhost:8082 |
| Notification Service | http://localhost:8083 |
| Swagger UI (User) | http://localhost:8081/swagger-ui.html |
| Swagger UI (Trip) | http://localhost:8082/swagger-ui.html |
| Swagger UI (Notification) | http://localhost:8083/swagger-ui.html |
| Health-чек (любой сервис) | `:PORT/actuator/health` |

### Первый запрос

```bash
# 1. Логин (admin засеян миграцией)
TOKEN=$(curl -s -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@taxi.com","password":"secret"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")

# 2. Зарегистрировать водителя
curl -X POST http://localhost:8081/drivers \
  -H "Content-Type: application/json" \
  -d '{"name":"Пётр","email":"p@m.ru","phone":"+79001112233","license_number":"L001","password":"secret123"}'

# 3. Создать поездку
curl -X POST http://localhost:8082/trips \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"passenger_id":1,"origin":"ул. Ленина, 1","destination":"пр. Мира, 20","distance_km":12.5}'
```

### Сборка локально (без Docker)

```bash
./gradlew build                    # все сервисы
./gradlew :trip-service:test       # тесты одного сервиса
./gradlew :user-service:bootRun    # запустить локально
./gradlew openApiGenerate          # обновить сгенерированный код после правки openapi/
```

---

## Сервисы и их API

### User Service (`:8081`) — пользователи и аутентификация

| Метод | Путь | Описание |
|---|---|---|
| POST | `/auth/login` | Получить JWT |
| POST | `/passengers` | Регистрация пассажира |
| GET | `/passengers/{id}` | Профиль пассажира |
| POST | `/drivers` | Регистрация водителя |
| GET | `/drivers/{id}` | Профиль водителя |
| PATCH | `/drivers/{id}/status` | Сменить статус водителя |
| GET | `/drivers/available` | Список свободных (кэш Redis 30с) |
| POST | `/drivers/assign` | **Атомарно** зарезервировать водителя (used by trip-service) |

**Статусы водителя:** `AVAILABLE` ↔ `BUSY` / `OFFLINE`.

### Trip Service (`:8082`) — поездки

| Метод | Путь | Описание |
|---|---|---|
| POST | `/trips` | Создать поездку (поддерживает `Idempotency-Key`) |
| GET | `/trips/{id}` | Получить поездку |
| GET | `/trips` | Список с фильтрами (`?passenger_id=…&status=…`) |
| PATCH | `/trips/{id}/status` | Сменить статус |
| POST | `/trips/{id}/rating` | Оценка (1–5, только для `COMPLETED`) |
| GET | `/statistics` | Аналитика за день |

**Стейт-машина поездки:**

```
SEARCHING ─► ASSIGNED ─► ACCEPTED ─► IN_PROGRESS ─► COMPLETED
     │           │           │             │
     └───────────┴───────────┴─────────────┴────► CANCELLED
```

**Расчёт цены:** `price = distance_km × tariff_rate` (по умолчанию `tariff_rate=25.0`).

### Notification Service (`:8083`) — асинхронные уведомления

| Метод | Путь | Описание |
|---|---|---|
| POST | `/notifications` | Поставить задачу в очередь |
| GET | `/notifications` | Список (`?trip_id=…&status=…&recipient_id=…`) |
| GET | `/notifications/{id}` | Одна задача |

**Жизненный цикл задачи:**

```
PENDING ─► IN_PROGRESS ─► SENT
                       └► PENDING (retry, attempts++)
                       └► DEAD    (после 3 неудач)
```

---

## Ключевые инженерные решения

### 1. Атомарное назначение водителя

Используется PostgreSQL `SELECT … FOR UPDATE SKIP LOCKED` — Postgres блокирует строку для одного запроса и **пропускает** её для конкурирующих, не заставляя их ждать. При 10 параллельных `POST /trips` каждый получает уникального водителя или 409 — без шанса double-booking. См. `DriverRepository.lockOneAvailable()`.

### 2. Очередь сообщений в БД (без Kafka/RabbitMQ)

`notification_tasks` — обычная таблица. Пул из 4 воркеров поллит её через тот же `SELECT FOR UPDATE SKIP LOCKED`. Гарантии at-least-once обеспечены транзакциями Postgres. Захват и доставка идут в **разных** транзакциях (`REQUIRES_NEW`), чтобы не держать row-lock на время медленной доставки. См. `WorkerPool` и `TaskClaimer`.

Брокер не используется специально: один потребитель, нет потоковой обработки, нет нужды в нескольких consumer-группах. Postgres даёт те же гарантии без отдельной инфраструктуры.

### 3. Idempotency-Key и TTL на ASSIGNED

Защита от разрывов связи у клиента:

- **Idempotency-Key** (`POST /trips`): повторный запрос с тем же ключом возвращает уже созданную поездку, а не создаёт дубль. Уникальный частичный индекс `(passenger_id, idempotency_key)`.
- **TTL 60 с на ASSIGNED**: фоновый скедулер каждые 30с ищет поездки, висящие в `ASSIGNED` слишком долго (водитель не подтвердил / пассажир пропал), отменяет их и возвращает водителя в `AVAILABLE`. Порядок операций важен: сначала освобождение водителя через user-service, только потом смена статуса поездки. Если HTTP-вызов упал — поездка остаётся в `ASSIGNED`, скедулер повторит на следующем тике. См. `StuckTripCleanupScheduler` и `TripService.cancelStuckAssigned()`.

### 4. Resilience4j на межсервисных HTTP

Все вызовы между сервисами завёрнуты в `@Retry` (3 попытки, экспоненциальный backoff 500ms × 2^n) и `@CircuitBreaker` (открывается при 50% ошибок в окне из 10 запросов, restore через 10с). Уведомления — **best-effort**: ошибка enqueue не откатывает создание поездки.

### 5. JWT propagation между сервисами

`HttpClientsConfig` через `RestClient` interceptor пробрасывает `Authorization` входящего запроса в межсервисные вызовы — вышестоящий сервис видит того же пользователя. Для фоновых задач (нет HTTP-контекста) `JwtService.issueSystem()` выпускает короткоживущий (5 минут) системный токен с ролью `ADMIN`.

### 6. API-first с OpenAPI

Контракт описан в `openapi/*.yaml`. Перед каждой компиляцией `openapi-generator` создаёт интерфейсы контроллеров (`*Api.java`) и DTO. Контроллеры реализуют интерфейсы — Java-компилятор гарантирует, что код не разойдётся со спецификацией. Swagger UI доступен в браузере и подхватывает спецификацию автоматически.

---

## База данных

### Таблицы

- **`passengers`** (user-service) — `id, name, email, phone, password_hash, created_at`
- **`drivers`** (user-service) — `id, name, email, phone, license_number, password_hash, status, created_at`
- **`trips`** (trip-service) — `id, passenger_id, driver_id, status, origin, destination, distance_km, price, rating, idempotency_key, created_at, updated_at`
- **`notification_tasks`** (notification-service) — `id, trip_id, recipient_type, recipient_id, message, status, attempts, last_error, created_at, updated_at`

> Все три сервиса делят **одну** базу данных `taxi`, но каждый управляет своими таблицами и своим Flyway-history (`flyway_schema_history_user/trip/notification`). При желании каждый сервис можно вынести в отдельную БД — миграции писать переписать не придётся.

### Миграции

Flyway применяет файлы из `src/main/resources/db/migration/V*__*.sql` при старте каждого сервиса. Файлы `Vn__seed_…sql` содержат базовые данные (например, admin@taxi.com).

---

## Конфигурация

Все параметры читаются через `application.yml` с переопределением через переменные окружения. Основные:

| Переменная | Дефолт | Что |
|---|---|---|
| `POSTGRES_HOST` | `localhost` | PostgreSQL host |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` / `_USER` / `_PASSWORD` | `taxi` / `taxi_user` / `taxi_password` | Кред-ы БД |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis |
| `JWT_SECRET` | (dev-default) | **256-битный** секрет, обязан совпадать у всех сервисов |
| `JWT_EXPIRATION_MS` | `3600000` | Время жизни access-токена (1ч) |
| `USER_SERVICE_URL` | `http://localhost:8081` | base URL для trip-service |
| `NOTIFICATION_SERVICE_URL` | `http://localhost:8083` | base URL для trip-service |
| `TRIP_TARIFF_RATE` | `25.0` | Цена за км |
| `TRIP_ASSIGNED_TTL_SECONDS` | `60` | TTL зависшей `ASSIGNED` поездки |
| `TRIP_ASSIGNED_CLEANUP_INTERVAL_MS` | `30000` | Интервал работы скедулера |
| `NOTIFICATION_WORKER_POOL_SIZE` | `4` | Кол-во воркеров |
| `NOTIFICATION_WORKER_POLL_INTERVAL_MS` | `1000` | Пауза при пустой очереди |
| `NOTIFICATION_MAX_ATTEMPTS` | `3` | После — задача в `DEAD` |

---

## Тестирование

```bash
./gradlew test                           # все
./gradlew :trip-service:test             # один сервис
./gradlew test jacocoTestReport          # с покрытием
```

Интеграционные тесты используют **Testcontainers** — поднимают реальные Postgres и Redis в Docker. Никаких H2/in-memory моков, тесты гоняют те же миграции, что и прод.

---

## Лицензия

MIT
