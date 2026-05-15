# Order Processing System

Sistema de processamento de pedidos com arquitetura orientada a eventos, desenvolvido como desafio técnico Java.

---

## Stack

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Banco de dados | PostgreSQL 15 |
| Mensageria | Apache Kafka + Zookeeper |
| Migrations | Flyway |
| Testes | JUnit 5 + Mockito + Testcontainers |
| Build | Maven |
| Containerização | Docker + Docker Compose |

---

## Arquitetura

O sistema segue o padrão MVC em camadas com Strategy pattern para processamento por tipo de produto.

```
POST /api/orders
       │
       ▼
OrderController → OrderService → salva pedido (PENDING) → publica evento Kafka
                                                                    │
                                                          OrderEventConsumer
                                                                    │
                                                         OrderProcessingService
                                                                    │
                                  ┌──────────────────────┬──────────────────────┐
                                  ▼                      ▼                      ▼
                         PhysicalProcessor   SubscriptionProcessor   DigitalProcessor
                         PreOrderProcessor   CorporateProcessor
                                  │
                        PROCESSED / FAILED / PENDING_APPROVAL
```

### Tipos de produto e regras

| Tipo | Regras |
|---|---|
| **PHYSICAL** | Verifica estoque, reserva unidades, estima prazo de entrega |
| **SUBSCRIPTION** | Limite de 5 assinaturas, sem duplicatas, sem conflito Enterprise + Basic |
| **DIGITAL** | Verifica licenças disponíveis, impede recompra, gera chave de ativação |
| **PRE_ORDER** | Valida data de lançamento futura, controla slots disponíveis |
| **CORPORATE** | Valida CNPJ, limite de crédito $100k, desconto 15% acima de 100 unidades, aprovação manual acima de $50k |

---

## Pré-requisitos

- Java 21
- Maven 3.9+
- Docker + Docker Compose

---

## Rodando localmente (H2 in-memory)

Sem Docker, sem Kafka — ideal para desenvolvimento:

```bash
mvn spring-boot:run
```

A aplicação sobe na porta `8080` com banco H2 em memória.
Console H2 disponível em: `http://localhost:8080/h2-console`

---

## Rodando com Docker

```bash
make up
```

Sobe PostgreSQL, Zookeeper, Kafka e a aplicação em containers.

```bash
make down        # Para tudo
make logs        # Ver logs da aplicação
make clean       # Remove containers e volumes
make db-migrate  # Executa migrations manualmente
```

---

## Testes

```bash
make test
```

Os testes unitários rodam sem Docker. Os testes de integração requerem Docker (Testcontainers).

---

## API

### Criar pedido
```http
POST /api/orders
Content-Type: application/json

{
  "customerId": "customer-123",
  "items": [
    { "productId": "LAPTOP-PRO-2024", "quantity": 1 },
    { "productId": "EBOOK-JAVA-001", "quantity": 2 }
  ]
}
```

Retorna `201 Created` com o pedido em status `PENDING`. O processamento acontece de forma assíncrona via Kafka.

### Buscar pedido
```http
GET /api/orders/{orderId}
```

### Listar pedidos do cliente
```http
GET /api/orders?customerId={customerId}
```

---

## Documentação interativa

Com a aplicação rodando, acesse:

```
http://localhost:8080/swagger-ui.html
```

---

## Health Check

```
http://localhost:8080/actuator/health
```

---

## Produtos disponíveis (seed)

| ID | Nome | Tipo | Preço |
|---|---|---|---|
| BOOK-CC-001 | Clean Code | PHYSICAL | R$ 89,90 |
| LAPTOP-PRO-2024 | Laptop Pro | PHYSICAL | R$ 5.499,00 |
| LAPTOP-MBP-M3-001 | MacBook Pro M3 | PHYSICAL | R$ 12.999,00 |
| SUB-PREMIUM-001 | Premium Monthly | SUBSCRIPTION | R$ 49,90 |
| SUB-BASIC-001 | Basic Monthly | SUBSCRIPTION | R$ 19,90 |
| SUB-ENTERPRISE-001 | Enterprise Plan | SUBSCRIPTION | R$ 299,00 |
| SUB-ADOBE-CC-001 | Adobe Creative Cloud | SUBSCRIPTION | R$ 159,00 |
| EBOOK-JAVA-001 | Effective Java | DIGITAL | R$ 39,90 |
| EBOOK-DDD-001 | Domain-Driven Design | DIGITAL | R$ 59,90 |
| EBOOK-SWIFT-001 | Swift Programming | DIGITAL | R$ 49,90 |
| COURSE-KAFKA-001 | Kafka Mastery | DIGITAL | R$ 299,00 |
| GAME-2025-001 | Epic Game 2027 | PRE_ORDER | R$ 249,90 |
| PRE-PS6-001 | PlayStation 6 | PRE_ORDER | R$ 4.999,00 |
| PRE-IPHONE16-001 | iPhone 16 Pro | PRE_ORDER | R$ 7.999,00 |
| CORP-LICENSE-ENT | Enterprise License | CORPORATE | R$ 15.000,00 |
| CORP-CHAIR-ERG-001 | Ergonomic Chair Bulk | CORPORATE | R$ 899,00 |
