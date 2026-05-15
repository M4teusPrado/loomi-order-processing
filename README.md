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
OrderController → OrderService → salva pedido (PENDING) → publica em [order-events]
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
                                  │
                        publica em [order-results]
                     (OrderProcessed / OrderFailed / OrderPendingApproval)
```

> **Nota:** O tópico `order-results` existe para sistemas downstream (notificações, inventário, relatórios). Neste projeto não há consumer implementado para esse tópico — em um contexto real, serviços independentes assinariam esse tópico para reagir ao resultado de cada pedido.

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

---

## Decisões de design

### Strategy pattern para processadores
Cada tipo de produto (`PHYSICAL`, `SUBSCRIPTION`, `DIGITAL`, `PRE_ORDER`, `CORPORATE`) tem seu próprio `OrderProcessor`. O `OrderProcessingService` recebe a lista de processors via injeção de dependência e delega para o correto conforme o tipo do item. Isso permite adicionar novos tipos sem alterar código existente (Open/Closed).

### Processamento por item, não por pedido
A unidade de processamento é o `OrderItem`, não o `Order`. Cada item é processado pelo processor correspondente ao seu tipo. O status final do pedido reflete o pior resultado entre os itens: se qualquer item falhar, o pedido vai para `FAILED`.

### Kafka para desacoplamento assíncrono
A criação do pedido responde imediatamente com `PENDING`. O processamento acontece em background via Kafka. Isso garante baixa latência na API e resiliência — se o consumer cair, os eventos ficam na fila e são reprocessados quando voltar.

### H2 para dev local, PostgreSQL para produção
O perfil `default` usa H2 in-memory e desabilita Kafka, permitindo rodar e testar sem Docker. O perfil `dev` usa PostgreSQL + Kafka via Docker Compose. A troca é transparente via `application-dev.yml`.

### Snapshot de preço no pedido
O preço é copiado do catálogo para o `OrderItem` no momento da criação. Isso garante que mudanças futuras no catálogo não afetam pedidos já criados — comportamento esperado em qualquer sistema de e-commerce.

### `@Autowired(required = false)` no producer
`OrderEventProducer` é um bean com `@Profile("!default")`, então não existe no perfil padrão (H2). O `OrderService` usa `required = false` para funcionar nos dois contextos sem precisar de dois serviços diferentes.

---

## O que priorizei e por quê

O foco foi na **correção da lógica de negócio** e na **confiabilidade do fluxo assíncrono**. Os cinco tipos de pedido com todas as regras do requirements foram implementados antes de qualquer item de infraestrutura ou documentação.

A segunda prioridade foi a **testabilidade**: testes unitários para cada processor e testes de integração com Testcontainers cobrindo o fluxo ponta a ponta (API → Kafka → processamento → status atualizado).

Itens como Swagger, Actuator e logging estruturado vieram depois, pois são importantes para produção mas não bloqueiam a funcionalidade core.

---

## O que melhoraria com mais tempo

- **Dead Letter Queue**: mensagens que falham repetidamente deveriam ir para um tópico separado com retry com backoff exponencial, evitando poison pills que travam o consumer.
- **Eventos de resultado**: publicar `OrderProcessed`, `OrderFailed` e `OrderPendingApproval` de volta no Kafka para que sistemas downstream (notificações, inventário) possam reagir.
- **Cobertura de testes**: adicionar testes unitários para `OrderService` e `PhysicalOrderProcessor`, e ampliar os cenários de integração para cobrir falhas de negócio (estoque zerado, assinatura duplicada, etc.).
- **Idempotência na API**: chave de idempotência no header da requisição para evitar pedidos duplicados em caso de retry do cliente.
- **Paginação**: `GET /api/orders?customerId=` retorna todos os pedidos sem limite — em produção precisaria de paginação.
- **Métricas**: expor contadores de pedidos por status via Micrometer/Prometheus para observabilidade em produção.

---

## Uso de IA

Este projeto foi desenvolvido com auxílio do **Claude** (Anthropic) via Claude Code CLI, utilizado como pair programmer ao longo de todo o desenvolvimento.

### Como foi usado

- **Geração de código**: estrutura inicial das classes, implementação dos processors, configuração do Kafka, setup do Testcontainers
- **Debugging**: diagnóstico do problema de consumer Kafka não conectando (conflito entre `KafkaConfig` manual e auto-configuração do Spring Boot)
- **Decisões de arquitetura**: discussão sobre Strategy pattern, separação de perfis, abordagem de snapshot de preço
- **Testes**: geração dos testes unitários e de integração com mocks e fixtures
- **Documentação**: Swagger annotations, README, estrutura de ADRs

### Validação do código gerado

Todo o código gerado foi revisado, testado e ajustado antes do commit:
- Testes unitários rodados localmente (`mvn test`) a cada mudança
- Testes de integração executados via Testcontainers
- Fluxo completo validado em Docker com `make up` e chamadas reais à API
- Bugs identificados e corrigidos iterativamente (ex: bootstrap servers do Kafka não sendo resolvidos corretamente no Docker)

O uso de IA acelerou o desenvolvimento mas não substituiu o entendimento — cada decisão de design foi discutida e validada, e cada bug foi diagnosticado com raciocínio próprio antes de aplicar a correção.
