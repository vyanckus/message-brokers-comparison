# Сравнение брокеров сообщений (Message Brokers Comparison)

![Java](https://img.shields.io/badge/Java-19-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-red)

Демонстрационное приложение для сравнения производительности и возможностей различных брокеров сообщений: **ActiveMQ, RabbitMQ, Kafka и WebSocket**.

## 🚀 Возможности

- **Унифицированный API** для работы с разными брокерами сообщений
- **Тестирование производительности** - benchmark сравнение пропускной способности
- **Мониторинг в реальном времени** - статус брокеров, метрики, графики
- **WebSocket демо** - интерактивные графики с живыми данными
- **Русскоязычный интерфейс** - удобный веб-интерфейс на русском языке

🏗️ Архитектура
---------------

### Модульная структура проекта


```
message-brokers-comparison/
├── 📁 broker-core/           # Основные интерфейсы и абстракции
├── 📁 shared-dto/            # Общие DTO для всех модулей
├── 📁 activemq-module/       # Реализация ActiveMQ брокера
├── 📁 rabbitmq-module/       # Реализация RabbitMQ брокера  
├── 📁 kafka-module/          # Реализация Kafka брокера
├── 📁 websocket-module/      # Реализация WebSocket брокера
├── 📁 demo-app/              # Основное Spring Boot приложение
└── 📁 infrastructure/        # Docker конфигурации
```

### Архитектурные слои


```
┌─────────────────────────────────────────────────────────────────┐
│                    УРОВЕНЬ ПРЕДСТАВЛЕНИЯ                        │
├─────────────────────────────────────────────────────────────────┤
│  ┌───────────────┐  ┌──────────────┐  ┌─────────────────────┐   │
│  │  Веб-страницы │  │  REST API    │  │  WebSocket Client   │   │
│  │ (Thymeleaf)   │  │  Endpoints   │  │   (SockJS/STOMP)    │   │
│  └───────────────┘  └──────────────┘  └─────────────────────┘   │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                    БИЗНЕС-УРОВЕНЬ                               │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌─────────────────┐                     │
│  │MessageController│    │ BenchmarkControl│                     │
│  │  (REST API)     │    │   ler (REST API)│                     │
│  └─────────────────┘    └─────────────────┘                     │
│           │                            │                        │
│  ┌────────▼────────┐         ┌─────────▼────────┐               │
│  │ MessageBroker   │         │ BenchmarkService │               │
│  │     Service     │         │                  │               │
│  └─────────────────┘         └──────────────────┘               │
└───────────────────────────────┬─────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                    УРОВЕНЬ ИНТЕГРАЦИИ                           │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │ MessageBroker   │  │ MessageBroker   │  │ Health Check    │  │
│  │   Factory       │  │   Interface     │  │   (Actuator)    │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
│           │                                                     │
└───────────┬─────────────────────────────────────────────────────┘
            │
 ┌──────────▼──────────┬───────────┬──────────┬──────────────────┐
 │    ActiveMQBroker   │ RabbitMQ  │ Kafka    │ WebSocketBroker  │
 │    (JMS API)        │ Broker    │ Broker   │ (STOMP/WS)       │
 └─────────────────────┴───────────┴──────────┴──────────────────┘
```

### Поток данных

text

```
Клиент (Браузер)
       │
       ▼
Spring MVC Controllers
       │
       ▼
Business Services
       │
       ▼
MessageBrokerFactory ───┐
       │                │
       ▼                │
MessageBroker Interface │
       │                │
       ├────────────────┘
       │
       ▼
┌──────────────┬──────────────┬──────────────┬──────────────┐
│ ActiveMQ     │ RabbitMQ     │   Kafka      │ WebSocket    │
│Implementation│Implementation│Implementation│Implementation│
└──────────────┴──────────────┴──────────────┴──────────────┘
       │                │                │                │
       ▼                ▼                ▼                ▼
 ActiveMQ Server  RabbitMQ Server  Kafka Cluster  WebSocket Client
```

### Ключевые компоненты

#### 🎯 Core Components

*   `MessageBroker` \- унифицированный интерфейс для всех брокеров

*   `MessageBrokerFactory` \- фабрика для создания экземпляров брокеров

*   `BrokerProperties` \- централизованная конфигурация


#### 🔌 Broker Implementations

*   `ActiveMQBroker` \- JMS-based реализация для Apache ActiveMQ

*   `RabbitMQBroker` \- AMQP-based реализация для RabbitMQ

*   `KafkaBroker` \- High-throughput реализация для Apache Kafka

*   `WebSocketBroker` \- Real-time реализация для WebSocket


#### 🌐 Web Layer

*   `WebController` \- обработка HTML страниц (Thymeleaf)

*   `MessageController` \- REST API для работы с сообщениями

*   `BenchmarkController` \- REST API для тестирования производительности

*   `WebSocketConfig` \- конфигурация WebSocket соединений


#### ⚙️ Services

*   `MessageBrokerService` \- основной сервис для управления брокерами

*   `BenchmarkService` \- сервис для тестирования производительности

*   `BrokerHealthIndicator` \- health checks для мониторинга


### Принципы проектирования

1.  Инверсия зависимостей \- зависимости направлены к абстракциям

2.  Единый интерфейс \- все брокеры реализуют `MessageBroker`

3.  Фабричный паттерн \- централизованное создание брокеров

4.  Модульность \- каждый брокер в отдельном модуле

5.  Graceful degradation \- приложение работает даже если брокеры недоступны


Эта архитектура позволяет легко добавлять новые брокеры сообщений и обеспечивает согласованное поведение across different messaging technologies.

## 📋 Поддерживаемые брокеры

| Брокер | Протокол | Сценарии использования | Производительность |
|--------|----------|------------------------|-------------------|
| **ActiveMQ** | JMS, AMQP, MQTT | Корпоративная передача сообщений | Средняя |
| **RabbitMQ** | AMQP 0-9-1 | Сложная маршрутизация, рабочие очереди | Высокая |
| **Kafka** | Протокол Kafka | Потоковая обработка, Event Sourcing | Очень высокая |
| **WebSocket** | WebSocket, STOMP | Веб-приложения реального времени | Низкая задержка |

## 🛠️ Технологический стек

### Backend
- **Java 19**
- **Spring Boot 3.5.7**
- **Spring Web MVC**
- **Spring WebSocket**
- **Spring Actuator** (мониторинг)
- **Maven** (сборка)

### Брокеры сообщений
- **Apache ActiveMQ** (JMS)
- **RabbitMQ** (AMQP)
- **Apache Kafka** (распределенный streaming)
- **WebSocket/STOMP** (real-time)

### Frontend
- **Thymeleaf** (шаблоны)
- **Bootstrap 5** (UI компоненты)
- **Chart.js** (графики)
- **JavaScript** (интерактивность)

## 📥 Установка и запуск

### Предварительные требования
- Java 19 или выше
- Maven 3.8+
- Docker и Docker Compose

### 1. Клонирование репозитория
```bash
git clone https://github.com/vyanckus/message-brokers-comparison.git
cd message-brokers-comparison
```
### 2\. Запуск брокеров сообщений (через Docker)

bash

```
cd infrastructure
docker-compose up -d
```

Проверьте что брокеры запустились:

*   ActiveMQ Console: [http://localhost:8161](http://localhost:8161/) (admin/admin)

*   RabbitMQ Console: [http://localhost:15672](http://localhost:15672/) (guest/guest)

*   Kafka: localhost:9092


### 3\. Сборка и запуск приложения

bash

```
# Сборка всех модулей
mvn clean install

# Запуск приложения
cd demo-app
mvn spring-boot:run
```

### 4\. Открытие приложения

Откройте в браузере: [http://localhost:8080](http://localhost:8080/)

🎯 Использование приложения
---------------------------

### Главная страница (`/`)

*   Обзор статуса всех брокеров

*   Быстрый доступ к основным функциям

*   Кнопки управления брокерами


### Тестирование сообщений (`/messages`)

*   Отправка сообщений через разные брокеры

*   Подписка на получение сообщений

*   Просмотр истории сообщений


### Benchmark тесты (`/benchmark`)

*   Сравнение производительности брокеров

*   Настройка параметров тестирования

*   Графики результатов


### Мониторинг (`/monitoring`)

*   Статус брокеров в реальном времени

*   Системные метрики

*   Логи сообщений


### WebSocket демо (`/websocket-demo`)

*   Интерактивные графики

*   Генератор тестовых данных

*   Статистика в реальном времени


### Состояние системы (`/health`)

*   Health checks всех компонентов

*   Детальная диагностика

*   История состояния

🔧 Конфигурация
---------------

Основные настройки в `demo-app/src/main/resources/application.yml`:

yaml

```
spring:
    activemq:
        broker-url: tcp://localhost:61616
    rabbitmq:
        host: localhost
        port: 5672
    kafka:
        bootstrap-servers: localhost:9092
 
message:
    broker:
        websocket:
            endpoint: localhost      
            path: /websocket
```

📊 API Endpoints
----------------

### Управление сообщениями

*   `POST /api/messages/initialize` \- инициализация брокеров

*   `POST /api/messages/send` \- отправка сообщения

*   `POST /api/messages/subscribe/{brokerType}` \- подписка на сообщения

*   `GET /api/messages/history` \- история сообщений


### Benchmark тесты

*   `POST /api/benchmark/run` \- запуск синхронного теста

*   `POST /api/benchmark/start-async` \- запуск асинхронного теста

*   `GET /api/benchmark/status` \- статус активных тестов


### Мониторинг

*   `GET /api/messages/status` \- статус брокеров

*   `GET /api/messages/metrics` \- метрики производительности

*   `GET /actuator/health` \- health checks (Spring Actuator)


🐛 Поиск и устранение неисправностей
------------------------------------

### Проблемы с подключением к брокерам

1.  Убедитесь что Docker контейнеры запущены: `docker ps`

2.  Проверьте порты: 61616 (ActiveMQ), 5672 (RabbitMQ), 9092 (Kafka)

3.  Проверьте логи приложения на предмет ошибок подключения


### Проблемы с WebSocket

1.  Убедитесь что браузер поддерживает WebSocket

2.  Проверьте настройки CORS в конфигурации

3.  Посмотрите логи браузера (F12 → Console)


### Проблемы со сборкой

1.  Очистите проект: `mvn clean`

2.  Обновите зависимости: `mvn dependency:resolve`

3.  Проверьте версию Java: `java -version`


🤝 Вклад в проект
-----------------

Приветствуются contributions! Для внесения изменений:

1.  Форкните репозиторий

2.  Создайте feature branch: `git checkout -b feature/amazing-feature`

3.  Сделайте commit изменений: `git commit -m 'Add amazing feature'`

4.  Push в branch: `git push origin feature/amazing-feature`

5.  Откройте Pull Request



👨‍💻 Автор
-----------

Фёдор Вянцкус

*   GitHub: [@vyanckus](https://github.com/vyanckus)

*   Email: vyanckus@mail.ru


### ⭐ Если этот проект был полезен, поставьте звезду на GitHub!
