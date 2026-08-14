# Техническая реализация и Архитектура

**Документ:** `docs/architecture/document-ocr-service-tech-spec.md`
**Статус:** Approved for Development
**Сервис:** document-ocr-service (Поддомен: Strategic Routing & AI Intelligence)

## 1. Технологический Стек и Зависимости

| Компонент | Технология | Назначение / Обоснование |
| :--- | :--- | :--- |
| **Runtime** | Java 21 (Virtual Threads) | Неблокирующая обработка фоновых I/O задач загрузки/выгрузки файлов. |
| **Framework** | Spring Boot 3.3+ | Интеграция со Spring AI, Spring Data, Spring Kafka. |
| **AI Integration** | Spring AI | Унифицированная абстракция для работы с LLM (Ollama / vLLM для локальных моделей или OpenAI / Claude API). |
| **OCR Engines** | Apache Tika + Tesseract OCR | Извлечение сырого текста, структуры слоев PDF и метаданных изображений. |
| **Object Storage** | MinIO (S3-compatible) | Хранилище бинарных файлов (сканы, PDF) и распознанных JSON-слоев. |
| **Database** | PostgreSQL 16 | Реляционная БД для хранения метаданных документов, атрибутов и правил валидации. |
| **Messaging** | Apache Kafka (Avro) | Передача результатов парсинга в другие сервисы системы. |
| **Cache & Queue** | Redis | Повторное использование промптов и ограничение частоты запросов к AI-моделям (Rate Limiting). |

## 2. Архитектура Конвейера Обработки (Pipeline Architecture)

Обработка документа строится по паттерну Pipes and Filters:

```text
[Загрузка файла] ──► [1. File Storage Filter] ──► [2. OCR & Text Extraction] ──► [3. Spring AI / LLM Structured Extraction] ──► [4. Rule Validation Filter] ──► [5. Outbox Event Publisher]
                           (Save to MinIO)              (Tika / Tesseract)                     (Format to JSON DTO)                 (Apply Business Rules)              (Send to Kafka)
```

*   **File Storage Filter:** Валидация формата, проверка на вирусы/вредоносные скрипты, загрузка бинарника в MinIO Bucket `transport-documents`.
*   **OCR Filter:** Извлечение сырого текстового слоя из PDF или запуск Tesseract для растровых картинок (PNG/JPG).
*   **LLM Extraction Filter:** Передача текста и промпта со строгой JSON-схемой (через `BeanOutputConverter` в Spring AI). LLM возвращает заполненный DTO.
*   **Rule Validation Filter:** Выполнение Java-правил бизнес-валидации над полученным DTO.
*   **Outbox Event Publisher:** Сохранение результата в PostgreSQL и публикация события в Kafka через Outbox Pattern.

## 3. Межсервисное Взаимодействие и Интеграции

### 3.1 Схема интеграционных связей

```text
                                         ┌──────────────────────────┐
                                         │   MinIO Storage (S3)     │
                                         └────────────▲─────────────┘
                                                      │ S3 SDK Upload/Download
                                                      │
┌──────────────────────┐    REST Multipart    ┌───────┴──────────────────┐    Kafka: DocumentParsedEvent     ┌──────────────────────────┐
│   React Frontend /   ├─────────────────────►│   document-ocr-service   ├─────────────────────────────────►│     routing-service      │
│   API Gateway        │                      └───────────┬──────────────┘                                  └──────────────────────────┘
└──────────────────────┘                                  │                                                              ▲
                                                          │                                                              │
                                                          │ Spring AI (gRPC / HTTP)                                      │
                                                          ▼                                                              │
                                         ┌──────────────────────────┐                                                    │
                                         │  Ollama / vLLM / OpenAI  │                                                    │
                                         └──────────────────────────┘                                                    │
                                                                                                                         │
                                         Kafka: DocumentValidationFailedEvent                                            │
                                         ────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Описание контрактов взаимодействия

**Входящие интерфейсы (REST API):**
*   `POST /api/v1/documents/upload` — загрузка файла (Multipart) с передачей `orderId` и предполагаемого типа документа.
*   `PATCH /api/v1/documents/{id}/attributes` — корректировка атрибутов диспетчером при ручной проверке.

**Исходящие события (Kafka Producers):**
*   `DocumentParsedEvent`: Содержит метаданные документа и полный структурированный JSON извлеченных атрибутов (коды ТН ВЭД, массы, адреса).
*   `DocumentValidationFailedEvent`: Генерируется, если критические атрибуты не найдены или не прошли проверку правил (содержит список ошибок для подстветки в UI).

## 4. Архитектурные Паттерны и Безопасность

### 4.1 Извлечение данных через Spring AI (Structured Outputs)
Для гарантии того, что LLM-модель отдаст строгий JSON без "галлюцинаций", используется паттерн Structured Output Formatting:
1.  Создается Java Record/DTO, представляющий структуру документа (`CmrDocumentDto`).
2.  Spring AI автоматически внедряет JSON-схему в системный промпт для LLM.
3.  Ответ от модели автоматически десериализуется в Java-объект с валидацией Hibernate Validator (`@NotNull`, `@Positive`).

### 4.2 Надежность интеграций и Fallback-стратегия
*   При недоступности основного LLM-сервиса используется паттерн Circuit Breaker (Resilience4j).
*   Документы ставятся в очередь на повторную обработку (Retry Queue в Redis/Kafka).
*   Если модель не может распознать текст из-за низкого качества, документ не "падет", а корректно переводится в статус NEEDS_MANUAL_REVIEW с уведомлением в UI.

### 4.3 Безопасность персональных и коммерческих данных (PII & Data Protection)
*   Прямой публичный доступ к файлам в S3 ЗАПРЕЩЕН. Доступ фронтенду предоставляется исключительно по Presigned URLs со сроком жизни 15 минут.
*   Все файлы при сохранении в MinIO шифруются на стороне сервера (SSE-S3).

## 5. Требования к Хранению Данных (Database Guidelines)

### 5.1 Структура хранения в PostgreSQL
*   Метаданные документов и списки извлеченных атрибутов хранятся в реляционной схеме.
*   Извлеченная JSON-структура дублируется в колонку с типом JSONB для возможности быстрого полнотекстового поиска и индексации произвольных атрибутов без изменения схемы БД.

### 5.2 Оптимизация
*   Для колонки JSONB создаются GIN-индексы (`CREATE INDEX idx_docs_payload ON transport_documents USING GIN (extracted_payload)`).
*   Настраивается автоматический TTL-скрипт очистки временных OCR-файлов из MinIO через 30 дней после закрытия рейса.

## 6. Observability и Эксплуатация

*   **Metrics (Micrometer + Prometheus):**
    *   `document_processing_duration_seconds` (histogram) — время выполнения полного конвейера обработки.
    *   `document_ai_confidence_score` (summary) — показатель уверенности распознавания.
    *   `document_manual_review_total` (counter) — количество документов, ушедших на ручную проверку.
*   **Logging:** Расширенное структурированное логирование с фиксацией количества потраченных токенов LLM (`prompt_tokens`, `completion_tokens`) для отслеживания стоимости инфраструктуры AI.
