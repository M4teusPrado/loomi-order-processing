# Order Processing System — Design Spec
**Data**: 2026-05-13  
**Prazo de entrega**: 2026-05-15 às 12:00  
**Desafio**: Loomi Java Technical Challenge

---

## Stack Tecnológica

| Componente | Escolha | Motivo |
|---|---|---|
| Linguagem | Java 21 | Familiaridade, requisito principal |
| Framework | Spring Boot 3.x | Obrigatório/recomendado |
| Banco (prod/test) | PostgreSQL | Obrigatório |
| Banco (dev local) | H2 in-memory | Evita Docker no dia-a-dia |
| Mensageria | Apache Kafka + Zookeeper | Requisito |
| Migrations | Flyway | Versionamento de schema + seed |
| Testes integração | Testcontainers | Recomendado pelo desafio |
| Build | Maven (mvnw) | Padrão Spring Initializr |
| Containerização | Docker + Docker Compose | Obrigatório |
| Automação | Makefile | Altamente recomendado |

---

## Arquitetura

**Padrão**: MVC em camadas (Controller → Service → Repository) com Strategy pattern para processamento por tipo de pedido.

### Fluxo principal

```
Cliente
  │
  ▼
POST /api/orders
  │
  ▼
OrderController
  │  valida payload
  ▼
OrderService
  │  busca produtos no DB (preço + tipo + status)
  │  calcula total
  │  salva pedido com status PENDING
  │
  ▼
OrderEventProducer ──▶ Kafka (order-events) ──▶ OrderEventConsumer
                                                      │
                                              Para cada item do pedido:
                                              ├── PHYSICAL      → PhysicalOrderProcessor
                                              ├── SUBSCRIPTION  → SubscriptionOrderProcessor
                                              ├── DIGITAL       → DigitalOrderProcessor
                                              ├── PRE_ORDER     → PreOrderProcessor
                                              └── CORPORATE     → CorporateOrderProcessor
                                                      │
                                              Todos passaram?    → PROCESSED
                                              Algum falhou?      → FAILED
                                              Requer aprovação?  → PENDING_APPROVAL
                                                      │
                                              atualiza DB + publica evento de resultado
```

---

## Estrutura de Pacotes

```
src/main/java/com/loomi/orderprocessing/
├── controller/
│   └── OrderController.java
├── service/
│   ├── OrderService.java
│   ├── OrderProcessingService.java
│   └── processor/
│       ├── OrderProcessor.java
│       ├── ProcessingResult.java
│       ├── PhysicalOrderProcessor.java
│       ├── SubscriptionOrderProcessor.java
│       ├── DigitalOrderProcessor.java
│       ├── PreOrderProcessor.java
│       └── CorporateOrderProcessor.java
├── repository/
│   ├── OrderRepository.java
│   └── ProductRepository.java
├── model/
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Product.java
│   ├── MetadataConverter.java
│   └── enums/
│       ├── OrderStatus.java     (PENDING, PROCESSED, FAILED, PENDING_APPROVAL)
│       ├── ProductType.java     (PHYSICAL, SUBSCRIPTION, DIGITAL, PRE_ORDER, CORPORATE)
│       └── FailureReason.java
├── dto/
│   ├── CreateOrderRequest.java
│   ├── OrderItemRequest.java
│   ├── OrderResponse.java
│   ├── OrderItemResponse.java
│   ├── OrderSummaryResponse.java
│   └── OrderEvent.java
├── kafka/
│   ├── OrderEventProducer.java
│   └── OrderEventConsumer.java
├── exception/
│   ├── ProductNotFoundException.java
│   ├── ProductNotAvailableException.java
│   ├── OrderNotFoundException.java
│   └── GlobalExceptionHandler.java
└── config/
    └── KafkaConfig.java
```

---

## Modelo de Dados

### Tabela `products`
```sql
CREATE TABLE products (
  product_id   VARCHAR(50)   PRIMARY KEY,
  name         VARCHAR(255)  NOT NULL,
  product_type VARCHAR(20)   NOT NULL,
  price        DECIMAL(10,2) NOT NULL,
  stock_qty    INTEGER,
  active       BOOLEAN       DEFAULT true,
  metadata     TEXT
);
```

Seed: 16 produtos do REQUIREMENTS.md inseridos via `V2__seed_products.sql`.

### Tabela `orders`
```sql
CREATE TABLE orders (
  order_id       VARCHAR(36)   PRIMARY KEY,
  customer_id    VARCHAR(100)  NOT NULL,
  status         VARCHAR(30)   NOT NULL,
  failure_reason VARCHAR(100),
  total_amount   DECIMAL(10,2) NOT NULL,
  created_at     TIMESTAMP     NOT NULL,
  updated_at     TIMESTAMP     NOT NULL
);
```

### Tabela `order_items`
```sql
CREATE TABLE order_items (
  id           BIGSERIAL     PRIMARY KEY,
  order_id     VARCHAR(36)   REFERENCES orders(order_id),
  product_id   VARCHAR(50)   NOT NULL,
  product_type VARCHAR(20)   NOT NULL,
  quantity     INTEGER       NOT NULL,
  unit_price   DECIMAL(10,2) NOT NULL,
  metadata     TEXT
);
```

---

## API REST

### `POST /api/orders`
- Valida payload (itens não-vazios, quantidades > 0)
- Busca cada produto no DB: valida existência, status ativo, obtém tipo e preço
- Calcula `totalAmount = Σ(quantity * unitPrice)`
- Persiste pedido com status `PENDING`
- Publica evento `ORDER_CREATED` no Kafka
- Retorna `201 Created` imediatamente

### `GET /api/orders/{orderId}`
- Retorna pedido completo com itens
- `404` se não encontrado

### `GET /api/orders?customerId={customerId}`
- Retorna lista resumida de pedidos do cliente

---

## Lógica de Processamento por Tipo

Cada item é processado pelo seu próprio processor. Se qualquer item falhar, o pedido vai para `FAILED`.

### PHYSICAL
- Verifica `stock_qty >= quantity` → falha `OUT_OF_STOCK`
- Se `stock_qty < 5` após reserva → loga alerta `LOW_STOCK`
- Calcula prazo de entrega mockado: SP=5d, RJ=7d, outros=10d

### SUBSCRIPTION
- Verifica duplicata ativa do mesmo produto → `DUPLICATE_ACTIVE_SUBSCRIPTION`
- Máximo 5 assinaturas ativas → `SUBSCRIPTION_LIMIT_EXCEEDED`
- Enterprise + Basic simultâneos → `INCOMPATIBLE_SUBSCRIPTIONS`

### DIGITAL
- Verifica pool de licenças → `LICENSE_UNAVAILABLE`
- Verifica se cliente já possui → `ALREADY_OWNED`
- Gera chave de ativação (UUID mockado)

### PRE_ORDER
- Valida `releaseDate` futura → `RELEASE_DATE_PASSED`
- Verifica slots disponíveis → `PRE_ORDER_SOLD_OUT`

### CORPORATE
- Valida formato CNPJ → `INVALID_CORPORATE_DATA`
- Limite de crédito $100k → `CREDIT_LIMIT_EXCEEDED`
- Total > $50k → `PENDING_APPROVAL`
- Quantidade > 100 → 15% desconto por volume

---

## Idempotência

Consumer verifica se pedido já saiu de `PENDING` antes de processar. Se sim, ignora o evento.

---

## Perfis Spring

| Perfil | Banco | Kafka |
|---|---|---|
| default | H2 in-memory | desabilitado |
| dev | PostgreSQL via env vars | Kafka via env vars |
| test (Testcontainers) | PostgreSQL container | Kafka container |

---

## Docker

| Container | Imagem | Porta |
|---|---|---|
| postgres | postgres:15 | 5432 |
| zookeeper | confluentinc/cp-zookeeper:7.5.0 | 2181 |
| kafka | confluentinc/cp-kafka:7.5.0 | 9092 |
| app | build local (multistage) | 8080 |
