# Requisitos Técnicos

## Contexto de Negócio

Você está construindo um **Sistema de Processamento de Pedidos** para uma plataforma de e-commerce. Quando os clientes fazem pedidos, eles precisam ser validados, processados de forma assíncrona e rastreados através de vários estados.

O sistema deve lidar com pedidos de forma confiável, publicar eventos para sistemas downstream (inventário, envio, notificações) e fornecer APIs para consultas de status de pedidos.

### Tipos de Pedido

O sistema deve suportar **cinco tipos de pedidos** com comportamentos e integrações diferentes:

1. **Produtos Físicos (PHYSICAL)**: Itens que requerem verificação de estoque físico e logística de envio
    - **Verificação de inventário**: Checar disponibilidade em estoque
    - **Regras de negócio**:
        - Validar estoque disponível para a quantidade solicitada
        - Produtos com estoque < 5 unidades devem gerar alerta de estoque baixo
        - Reservar quantidade no inventário
        - Calcular prazo de entrega baseado na localização (mockar: 5-10 dias)
    - **Falhas possíveis**: `OUT_OF_STOCK`, `WAREHOUSE_UNAVAILABLE`
    - **Exemplo**: Livros, eletrônicos, roupas

2. **Assinaturas (SUBSCRIPTION)**: Serviços recorrentes que não requerem inventário físico
    - **Verificação**: Validar limites de assinaturas ativas por cliente
    - **Regras de negócio**:
        - Cliente não pode ter assinatura ativa do mesmo produto
        - Limite máximo de 5 assinaturas ativas por cliente
        - Validar compatibilidade entre assinaturas (ex: não pode ter plano Free e Premium simultaneamente)
        - Agendar primeira cobrança para data de ativação
    - **Falhas possíveis**: `SUBSCRIPTION_LIMIT_EXCEEDED`, `DUPLICATE_ACTIVE_SUBSCRIPTION`, `INCOMPATIBLE_SUBSCRIPTIONS`
    - **Exemplo**: Streaming, SaaS, memberships

3. **Produtos Digitais (DIGITAL)**: Produtos sem estoque físico, entregues digitalmente
    - **Verificação**: Validar licenças e direitos de distribuição
    - **Regras de negócio**:
        - Verificar disponibilidade de licenças (limite por produto digital)
        - Cliente não pode comprar mesmo produto digital mais de uma vez
        - Gerar chave de ativação/licença única
        - Envio imediato por email (mockar)
    - **Falhas possíveis**: `LICENSE_UNAVAILABLE`, `ALREADY_OWNED`, `DISTRIBUTION_RIGHTS_EXPIRED`
    - **Exemplo**: E-books, softwares, cursos online

4. **Pré-venda (PRE_ORDER)**: Produtos ainda não lançados, vendidos antecipadamente
    - **Verificação**: Validar disponibilidade de slots de pré-venda
    - **Regras de negócio**:
        - Verificar se data de lançamento é futura
        - Validar limite de pré-vendas disponíveis (ex: 1000 unidades)
        - Cobrança imediata, mas entrega apenas na data de lançamento
        - Permitir cancelamento até 7 dias antes do lançamento
        - Aplicar desconto de pré-venda se configurado
    - **Falhas possíveis**: `PRE_ORDER_SOLD_OUT`, `RELEASE_DATE_PASSED`, `INVALID_RELEASE_DATE`
    - **Exemplo**: Livros não lançados, games, eletrônicos

5. **Pedidos Corporativos (CORPORATE)**: Pedidos B2B com regras especiais
    - **Verificação**: Validar crédito corporativo e aprovações
    - **Regras de negócio**:
        - Validar limite de crédito da empresa
        - Pedidos > $50.000 requerem aprovação manual (status `PENDING_APPROVAL`)
        - Aplicar desconto por volume (ex: >100 itens = 15% desconto)
        - Prazo de pagamento diferenciado (30/60/90 dias)
        - Validar CNPJ e inscrição estadual
    - **Falhas possíveis**: `CREDIT_LIMIT_EXCEEDED`, `INVALID_CORPORATE_DATA`, `PENDING_MANUAL_APPROVAL`
    - **Exemplo**: Compras em massa, equipamentos, suprimentos

**Importante**: A lógica de processamento deve ser capaz de distinguir entre esses tipos e aplicar validações e integrações específicas para cada um. Pedidos podem conter itens de tipos diferentes (pedido misto).

### Catálogo de Produtos (Mock)

O sistema deve possuir um **catálogo de produtos** que serve como fonte única de verdade para informações de produtos, incluindo preços, tipos e disponibilidade, que pode ser um serviço apartado ou não, ficando a seu critério.

**Implementação Sugerida**:

Você pode implementar o catálogo de produtos de duas formas:
- **Opção 1**: Tabela PostgreSQL `products` com dados iniciais (seed)
- **Opção 2**: Map/enum hard-coded em memória (mais simples para o desafio)
- **Opção 3**: Serviço apartado que recebe Rest Request solicitando os dados do produto.

**Schema Sugerido**:
```sql
CREATE TABLE products (
  product_id VARCHAR(50) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  product_type VARCHAR(20) NOT NULL, -- PHYSICAL, SUBSCRIPTION, DIGITAL, PRE_ORDER, CORPORATE
  price DECIMAL(10, 2) NOT NULL,
  stock_quantity INTEGER,
  active BOOLEAN DEFAULT true,
  metadata JSONB -- dados específicos do tipo (ex: releaseDate para PRE_ORDER)
);
```

**Exemplos de Produtos** (para popular o catálogo):

```json
// Produtos Físicos
{ "productId": "BOOK-CC-001", "name": "Clean Code", "productType": "PHYSICAL", "price": 89.90, "stock": 150 }
{ "productId": "LAPTOP-PRO-2024", "name": "Laptop Pro", "productType": "PHYSICAL", "price": 5499.00, "stock": 8 }
{ "productId": "LAPTOP-MBP-M3-001", "name": "MacBook Pro M3", "productType": "PHYSICAL", "price": 12999.00, "stock": 25 }

// Assinaturas
{ "productId": "SUB-PREMIUM-001", "name": "Premium Monthly", "productType": "SUBSCRIPTION", "price": 49.90 }
{ "productId": "SUB-BASIC-001", "name": "Basic Monthly", "productType": "SUBSCRIPTION", "price": 19.90 }
{ "productId": "SUB-ENTERPRISE-001", "name": "Enterprise Plan", "productType": "SUBSCRIPTION", "price": 299.00 }
{ "productId": "SUB-ADOBE-CC-001", "name": "Adobe Creative Cloud", "productType": "SUBSCRIPTION", "price": 159.00 }

// Digitais
{ "productId": "EBOOK-JAVA-001", "name": "Effective Java", "productType": "DIGITAL", "price": 39.90, "licenses": 1000 }
{ "productId": "EBOOK-DDD-001", "name": "Domain-Driven Design", "productType": "DIGITAL", "price": 59.90, "licenses": 500 }
{ "productId": "EBOOK-SWIFT-001", "name": "Swift Programming", "productType": "DIGITAL", "price": 49.90, "licenses": 800 }
{ "productId": "COURSE-KAFKA-001", "name": "Kafka Mastery", "productType": "DIGITAL", "price": 299.00, "licenses": 500 }

// Pré-venda
{ "productId": "GAME-2025-001", "name": "Epic Game 2025", "productType": "PRE_ORDER", "price": 249.90, "releaseDate": "2025-06-01", "preOrderSlots": 1000 }
{ "productId": "PRE-PS6-001", "name": "PlayStation 6", "productType": "PRE_ORDER", "price": 4999.00, "releaseDate": "2025-11-15", "preOrderSlots": 500 }
{ "productId": "PRE-IPHONE16-001", "name": "iPhone 16 Pro", "productType": "PRE_ORDER", "price": 7999.00, "releaseDate": "2025-09-20", "preOrderSlots": 2000 }

// Corporativo
{ "productId": "CORP-LICENSE-ENT", "name": "Enterprise License", "productType": "CORPORATE", "price": 15000.00 }
{ "productId": "CORP-CHAIR-ERG-001", "name": "Ergonomic Chair Bulk", "productType": "CORPORATE", "price": 899.00, "stock": 500 }
```

**Responsabilidades do Catálogo**:
- Fornecer informações completas do produto (nome, preço, tipo)
- Validar se produto existe e está ativo
- Retornar erro se produto não for encontrado ou estiver inativo

**Nota**: A API de criação de pedidos deve **sempre** buscar o preço do catálogo, nunca confiar em preços enviados pelo cliente.

## Requisitos Funcionais

### 1. API de Criação de Pedidos

**Endpoint**: `POST /api/orders`

**Request Body**:
```json
{
  "customerId": "string",
  "items": [
    {
      "productId": "string",  // SKU do produto
      "quantity": number,
      "metadata": {
        // Campos opcionais específicos por tipo
      }
    }
  ]
}
```

> **⚠️ Nota Importante sobre Preços**: O preço **NÃO** deve vir do cliente na requisição. O sistema deve **sempre** buscar o preço do catálogo de produtos no backend para evitar manipulação de preços e garantir consistência. O cliente envia apenas `productId` e `quantity`.

**Exemplo 1 - Produto Físico Simples**:
```json
{
  "customerId": "customer-123",
  "items": [
    {
      "productId": "BOOK-CC-001",
      "quantity": 2,
      "metadata": {
        "warehouseLocation": "SP"
      }
    }
  ]
}
```

**Exemplo 2 - Assinatura**:
```json
{
  "customerId": "customer-456",
  "items": [
    {
      "productId": "SUB-PREMIUM-001",
      "quantity": 1,
      "metadata": {
        "billingCycle": "MONTHLY",
        "autoRenewal": true
      }
    }
  ]
}
```

**Exemplo 3 - Produto Digital**:
```json
{
  "customerId": "customer-789",
  "items": [
    {
      "productId": "EBOOK-DDD-001",
      "quantity": 1,
      "metadata": {
        "format": "PDF",
        "deliveryEmail": "customer@email.com"
      }
    }
  ]
}
```

**Exemplo 4 - Pré-venda**:
```json
{
  "customerId": "customer-101",
  "items": [
    {
      "productId": "PRE-PS6-001",
      "quantity": 1,
      "metadata": {
        "releaseDate": "2025-11-15"
      }
    }
  ]
}
```

**Exemplo 5 - Pedido Corporativo**:
```json
{
  "customerId": "company-acme-corp",
  "items": [
    {
      "productId": "CORP-CHAIR-ERG-001",
      "quantity": 150,
      "metadata": {
        "cnpj": "12.345.678/0001-90",
        "paymentTerms": "NET_60",
        "purchaseOrder": "PO-2025-001"
      }
    }
  ]
}
```

**Exemplo 6 - Pedido Misto Complexo** (cenário real):
```json
{
  "customerId": "customer-premium-999",
  "items": [
    {
      "productId": "LAPTOP-MBP-M3-001",
      "quantity": 1,
      "metadata": {
        "warehouseLocation": "SP"
      }
    },
    {
      "productId": "SUB-ADOBE-CC-001",
      "quantity": 1,
      "metadata": {
        "billingCycle": "ANNUAL",
        "autoRenewal": true
      }
    },
    {
      "productId": "EBOOK-SWIFT-001",
      "quantity": 1,
      "metadata": {
        "format": "EPUB"
      }
    },
    {
      "productId": "PRE-IPHONE16-001",
      "quantity": 1,
      "metadata": {
        "releaseDate": "2025-09-20"
      }
    }
  ]
}
```

**Exemplo 7 - Pedido com Validações de Negócio Complexas**:
```json
{
  "customerId": "customer-vip-777",
  "items": [
    {
      "productId": "SUB-ENTERPRISE-001",
      "quantity": 1,
      "metadata": {
        "billingCycle": "ANNUAL",
        "slaLevel": "PLATINUM"
      }
    },
    {
      "productId": "SUB-BASIC-001",
      "quantity": 1,
      "metadata": {
        "billingCycle": "MONTHLY"
      }
    }
  ]
}
```
_Nota: Este pedido deve falhar com `INCOMPATIBLE_SUBSCRIPTIONS` pois cliente não pode ter planos Enterprise e Basic simultaneamente._

**Response**: `201 Created`
```json
{
  "orderId": "string",
  "status": "PENDING",
  "totalAmount": number,
  "createdAt": "timestamp"
}
```

**Requisitos**:
- Validar payload da requisição (itens não-vazios, quantidades positivas)
- **Buscar informações de cada produto** de uma tabela/serviço de produtos:
    - Validar que `productId` existe e está ativo
    - Obter `productType` do produto (PHYSICAL, SUBSCRIPTION, etc.)
    - Obter `price` atual do produto
    - Validar disponibilidade básica (produto ativo, não descontinuado)
- Gerar ID único do pedido
- **Calcular valor total** usando preços obtidos do backend
- **Criar snapshot de preços**: armazenar preço de cada item no momento da compra
- Persistir pedido no PostgreSQL com status `PENDING` (incluindo snapshot de preços)
- Publicar evento `OrderCreated` no Kafka (com preços validados)
- Retornar detalhes do pedido imediatamente (não aguardar processamento)

**Tratamento de Erros**:
- Payload inválido → `400 Bad Request`
- Produto não encontrado → `404 Not Found` com mensagem: "Product {productId} not found"
- Produto inativo/descontinuado → `400 Bad Request` com mensagem: "Product {productId} is not available"
- Quantidade inválida → `400 Bad Request`
- Erros do sistema → `500 Internal Server Error` com logging adequado

---

### 2. Processamento Assíncrono de Pedidos

**Event Consumer**: Escutar eventos `OrderCreated` do Kafka

**Lógica de Processamento**:
1. Receber evento `OrderCreated`
2. **Validações Globais**:
    - Verificar se valor total > $10.000 (pedido de alto valor, requer validação adicional)
    - Simular processamento de pagamento (pode ser lógica mockada)
    - Validar fraude para pedidos > $20.000 (mockar: 5% de chance de fraud alert)

3. **Processar itens por tipo** (lógica diferenciada):

   **Para PHYSICAL (Produtos Físicos)**:
    - Simular verificação de inventário físico (pode ser lógica mockada)
    - Verificar disponibilidade em estoque
    - Se estoque < 5 unidades → gerar evento `LowStockAlert`
    - Reservar quantidade no inventário
    - Calcular prazo de entrega baseado em `warehouseLocation`
    - Se indisponível → `FAILED` com reason `OUT_OF_STOCK`

   **Para SUBSCRIPTION (Assinaturas)**:
    - Simular verificação de limites de assinatura (pode ser lógica mockada)
    - Validar se cliente não possui assinatura ativa do mesmo produto
    - Validar limite máximo de 5 assinaturas ativas por cliente
    - Validar compatibilidade entre assinaturas (ex: Enterprise vs Basic)
    - Agendar primeira cobrança
    - Falhas possíveis: `SUBSCRIPTION_LIMIT_EXCEEDED`, `DUPLICATE_ACTIVE_SUBSCRIPTION`, `INCOMPATIBLE_SUBSCRIPTIONS`

   **Para DIGITAL (Produtos Digitais)**:
    - Verificar disponibilidade de licenças (simular pool de licenças)
    - Validar se cliente já possui o produto digital
    - Gerar chave de ativação/licença única (mockar UUID)
    - Simular envio de email com download link
    - Falhas possíveis: `LICENSE_UNAVAILABLE`, `ALREADY_OWNED`

   **Para PRE_ORDER (Pré-vendas)**:
    - Validar se `releaseDate` é futura
    - Verificar limite de slots de pré-venda (ex: máximo 1000)
    - Aplicar desconto de pré-venda se `preOrderDiscount` presente
    - Marcar para envio futuro na data de lançamento
    - Falhas possíveis: `PRE_ORDER_SOLD_OUT`, `RELEASE_DATE_PASSED`

   **Para CORPORATE (Pedidos Corporativos)**:
    - Validar CNPJ (simular validação de formato)
    - Verificar limite de crédito da empresa (mockar limite de $100.000)
    - Se pedido > $50.000 → alterar status para `PENDING_APPROVAL` (não processar automaticamente)
    - Se quantidade > 100 itens → aplicar 15% de desconto por volume
    - Configurar prazo de pagamento conforme `paymentTerms`
    - Falhas possíveis: `CREDIT_LIMIT_EXCEEDED`, `INVALID_CORPORATE_DATA`

4. **Validações de Negócio Complexas** (cenários especiais):
    - Se pedido contém PHYSICAL + PRE_ORDER → calcular envio separado
    - Se pedido contém múltiplas SUBSCRIPTIONS → validar compatibilidade entre elas
    - Se CORPORATE com desconto por volume → recalcular total
    - Se pedido tem itens incompatíveis → falhar todo o pedido

5. Atualizar status do pedido baseado no resultado:
    - Sucesso → `PROCESSED`
    - Aprovação necessária → `PENDING_APPROVAL`
    - Falha → `FAILED` (com reason específico)

6. Publicar evento de resultado:
    - Sucesso → evento `OrderProcessed`
    - Falha → evento `OrderFailed`
    - Aprovação → evento `OrderPendingApproval`
    - Alerta → eventos adicionais (`LowStockAlert`, `FraudAlert`, etc.)

7. Persistir atualização de status no PostgreSQL

**Requisitos**:
- Processamento idempotente (lidar com eventos duplicados)
- Tratamento adequado de erros e logging
- Gerenciamento de transações (atualização DB + publicação de evento)
- Lógica de retry configurável para falhas transientes

---

### 3. API de Consulta de Pedidos

**Endpoint**: `GET /api/orders/{orderId}`

**Response**: `200 OK`
```json
{
  "orderId": "string",
  "customerId": "string",
  "items": [...],
  "totalAmount": number,
  "status": "PENDING|PROCESSED|FAILED",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

**Endpoint**: `GET /api/orders?customerId={customerId}`

**Response**: `200 OK`
```json
{
  "orders": [
    {
      "orderId": "string",
      "totalAmount": number,
      "status": "string",
      "createdAt": "timestamp"
    }
  ]
}
```

**Requisitos**:
- Consultar pedidos por ID ou ID do cliente
- Retornar 404 se pedido não for encontrado
- Suportar paginação para consultas de lista (bônus)

---

### 4. Schema de Eventos

**Tópico**: `order-events`

**Evento OrderCreated**:
```json
{
  "eventId": "string",
  "eventType": "ORDER_CREATED",
  "timestamp": "ISO-8601",
  "payload": {
    "orderId": "string",
    "customerId": "string",
    "items": [...],
    "totalAmount": number
  }
}
```

**Evento OrderProcessed**:
```json
{
  "eventId": "string",
  "eventType": "ORDER_PROCESSED",
  "timestamp": "ISO-8601",
  "payload": {
    "orderId": "string",
    "processedAt": "timestamp"
  }
}
```

**Evento OrderFailed**:
```json
{
  "eventId": "string",
  "eventType": "ORDER_FAILED",
  "timestamp": "ISO-8601",
  "payload": {
    "orderId": "string",
    "reason": "string",
    "failedAt": "timestamp"
  }
}
```

---

## Requisitos Não-Funcionais

### 1. Persistência de Dados (PostgreSQL)

**Restrições**:
- Pedidos não podem ser deletados (trilha de auditoria)
- Transições de status devem ser válidas (PENDING → PROCESSED/FAILED)

---

### 2. Mensageria (Apache Kafka ou Redpanda)

Você pode escolher entre **Apache Kafka** ou **Redpanda** (Kafka-compatible) para implementar a mensageria. Ambos são aceitos.

**Configuração**:
- Tópico: `order-events` (ou múltiplos tópicos se preferir)
- Consumer group para processamento de pedidos
- Semântica de entrega at-least-once
- Serialização adequada (JSON recomendado)

**Requisitos**:
- Producer: Publicar eventos de forma confiável com tratamento de erros
- Consumer: Lidar com mensagens de forma idempotente
- Dead Letter Topic para mensagens envenenadas (bônus)

**Nota**: Redpanda é compatível com a API do Kafka e pode simplificar a configuração do ambiente (não requer Zookeeper).

---

### 3. Testes

**Testes de Integração** (Obrigatório):
- Usar Testcontainers para PostgreSQL e Kafka/Redpanda
- Testar fluxo completo de pedido: API → Evento → Processamento → Atualização de Status
- Testar cenários de erro: payloads inválidos, falhas de processamento
- Testar idempotência: tratamento de eventos duplicados
- Testes devem ser executáveis via `./gradlew test` ou `mvn test`

**Testes Unitários**:
- Validação de lógica de negócio
- Comportamento do modelo de domínio
- Lógica da camada de serviço

**Cobertura de Testes**:
- Mínimo de 70% de cobertura de código
- Foco em caminhos críticos e casos extremos

---

### 4. Containerização (Docker) e Automação (Make)

**Arquivos Obrigatórios**:
- `Dockerfile` para a aplicação
- `docker-compose.yml` orquestrando:
    - Container da aplicação (Java/Kotlin containerizado)
    - Container PostgreSQL
    - Containers Kafka + Zookeeper (ou Kafka KRaft ou Redpanda)
- **`Makefile`** para automação de comandos (altamente recomendado)

**Requisitos de Automação com Make**:

O uso de `Makefile` é **altamente recomendado** para simplificar a execução do projeto. Comandos esperados:

```makefile
# Exemplos de targets esperados (adapte conforme necessário)
make setup          # Configurar ambiente (build de containers, etc.)
make up             # Subir toda a infraestrutura (docker-compose up)
make down           # Derrubar infraestrutura
make build          # Build da aplicação
make test           # Executar testes
make clean          # Limpar containers e volumes
make logs           # Ver logs da aplicação
make db-migrate     # Executar migrações de banco (se aplicável)
```

**Requisitos de Containerização**:
- Aplicação deve iniciar com: `make up` ou `docker-compose up`
- Aplicações Java/Kotlin devem rodar **dentro de container Docker** (não apenas dependências)
- Schema do banco de dados deve ser inicializado automaticamente (migrations)
- Aplicação deve aguardar dependências estarem prontas (health checks + wait-for)
- Health checks para todos os serviços
- Configuração adequada de variáveis de ambiente

---

### 5. Tratamento de Erros e Resiliência

**Requisitos**:
- Tratamento adequado de exceções (sem falhas silenciosas)
- Mensagens de erro significativas nas respostas
- Logging estruturado (formato JSON preferido)
- Lógica de retry para falhas transientes (DB, Kafka)
- Padrão Circuit Breaker para chamadas externas (bônus)
- Tratamento de desligamento gracioso

**Padrões de Logging**:
- Usar SLF4J/Logback (não `printStackTrace()`)
- Níveis de log: ERROR para falhas, INFO para eventos-chave, DEBUG para detalhes
- Incluir IDs de correlação para rastreamento (bônus)
- Logs estruturados com contexto (orderId, customerId, etc.)

---

### 6. Gerenciamento de Configuração

**Requisitos**:
- Configuração externalizada (variáveis de ambiente)
- Sem credenciais ou URLs hardcoded
- Configurações separadas para diferentes ambientes (dev, test, prod)
- Defaults sensatos para desenvolvimento local

**Itens de Configuração**:
- Conexão com banco de dados (host, porta, credenciais)
- Brokers Kafka
- Porta da aplicação
- Níveis de logging
- Regras de negócio (ex: limite de alto valor)

---

### 7. Qualidade de Código

**Arquitetura**:
- Separação clara de responsabilidades (camadas/hexagonal/clean architecture)
- Lógica de domínio isolada da infraestrutura
- Injeção de dependência
- Aderência aos princípios SOLID

**Padrões de Código**:
- Nomes significativos para variáveis/métodos/classes
- Funções/métodos pequenos e focados
- Encapsulamento adequado
- Evitar duplicação de código
- Comentários para lógica não-óbvia

**Específico para Kotlin** (se usar Kotlin):
- Usar data classes para DTOs
- Aproveitar null safety
- Usar extension functions apropriadamente
- Preferir imutabilidade
- Padrões idiomáticos de Kotlin

---

## Features Bônus (Opcional)

Estas não são obrigatórias mas serão vistas positivamente:

### 1. Idempotência Avançada
- Chaves de idempotência para requisições de API
- Tabela de rastreamento de deduplicação

### 2. Dead Letter Queue
- Tópico separado para mensagens falhadas
- Mecanismo de retry com exponential backoff

### 3. Atualizações de Pedidos
- `PATCH /api/orders/{orderId}` para atualizar itens do pedido
- Suporte a cancelamento com evento `OrderCancelled`

### 4. Observabilidade
- Endpoints de health check (`/actuator/health`)
- Endpoint de métricas (formato Prometheus)
- Rastreamento distribuído (OpenTelemetry)

### 5. Documentação de API
- Especificação OpenAPI/Swagger
- Documentação interativa de API

### 6. Migrações de Banco de Dados
- Flyway ou Liquibase para versionamento de schema

### 7. Segurança
- Autenticação de API (basic auth ou API keys)
- Sanitização de input
- Prevenção de SQL injection

---

## Critérios de Sucesso

Sua submissão será avaliada em:

✅ **Completude**:
- Todos os requisitos funcionais principais implementados
- Todos os requisitos não-funcionais atendidos
- Aplicação executa com sucesso via Docker

✅ **Qualidade**:
- Código limpo e manutenível
- Testes abrangentes (integração + unitários)
- Tratamento adequado de erros e logging

✅ **Documentação**:
- README claro com instruções de setup
- Decisões de arquitetura documentadas
- Comentários de código onde necessário

✅ **Prontidão para Produção**:
- Containerizado e deployável
- Configuração externalizada
- Resiliente a falhas

---

## Restrições e Premissas

**Restrição de Tempo**:
- Foque em qualidade sobre quantidade
- Implemente features principais bem, depois adicione bônus se houver tempo

**Simplificações Permitidas**:
- Lógica de negócio mock (pagamento, inventário) - foque na arquitetura
- Autenticação simples (ou pule) a menos que queira demonstrá-la
- UI básica não é necessária - apenas API está ok

**Escolhas Tecnológicas**:
- **Preferencial**: Spring Boot + PostgreSQL
    - Spring Boot é altamente recomendado para demonstrar conhecimento do ecossistema Spring
    - PostgreSQL deve ser usado como banco de dados relacional
- Você pode escolher outras tecnologias se preferir, mas documente sua decisão
- Justifique trade-offs no seu README

---

## Uso de Inteligência Artificial (IA)

### Política de IA

**O uso de IA é permitido e incentivado** como ferramenta de produtividade no desenvolvimento moderno. Acreditamos que saber usar IA de forma eficaz é uma habilidade essencial para desenvolvedores atuais.

### O Que Você Pode Fazer

✅ **Permitido e Incentivado**:
- Usar ferramentas de IA (ChatGPT, Claude, GitHub Copilot, etc.) para assistência no código
- Gerar código boilerplate, configurações, schemas
- Obter sugestões de design patterns e arquitetura
- Debugar erros e problemas
- Escrever testes
- Gerar documentação
- Pesquisar melhores práticas

### O Que Esperamos de Você

📋 **Requisitos Obrigatórios**:

1. **Entendimento Completo**:
    - Você **deve ser capaz de explicar cada linha** de código submetido
    - Você **deve entender** todas as decisões de arquitetura e design
    - Você **deve saber justificar** por que escolheu determinada abordagem

2. **Durante a Entrevista Técnica**:
    - Esteja preparado para explicar **qualquer parte do código** em detalhes
    - Esperamos que você demonstre **domínio** sobre o que foi implementado
    - Perguntaremos sobre **trade-offs**, **alternativas** e **decisões de design**
    - Você será questionado sobre **como usou IA** no processo

### Por Que Isso Importa

No mundo real, desenvolvedores profissionais usam IA como ferramenta. O que diferencia um bom profissional é:

1. **Saber usar IA efetivamente** (prompt engineering, validação)
2. **Entender profundamente** o que a IA gerou
3. **Ter senso crítico** para aceitar/rejeitar sugestões
4. **Adaptar e melhorar** código gerado

**Não é sobre escrever cada linha manualmente. É sobre dominar o que você entrega.**

### Red Flags (O Que NÃO Fazer)

❌ **Evite**:
- Copiar código sem entender
- Não conseguir explicar o que você submeteu
- Não documentar o uso de IA
- Mentir sobre o uso de IA na entrevista
- Submeter código com erros óbvios não revisados

---

## Organização do Repositório e Pull Requests

### Requisito Obrigatório

**O código deve ser organizado em Pull Requests (PRs) bem estruturados**, não apenas commits diretos na branch principal.

### Requisitos de Pull Requests

1. **Estrutura de Branches**:
    - Desenvolva cada feature em uma branch separada
    - Use nomenclatura clara: `feature/nome-da-feature`
    - Crie PRs para merge na branch `main`

2. **Organização de PRs**:
    - Cada PR deve implementar **uma funcionalidade específica**
    - Não misturar múltiplas features em um único PR
    - PRs devem ter descrição clara do que foi implementado

3. **Commits Atômicos**:
    - Cada commit deve ser uma **unidade lógica completa**
    - Commits devem compilar e passar nos testes
    - Mensagens devem ser claras e descritivas

4. **Conventional Commits** (obrigatório):
    - Use o padrão: `<type>(<scope>): <subject>`
    - Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`
    - Exemplo: `feat(order-api): implement POST /api/orders endpoint`

5. **Descrição de PRs**:
    - Explique o que foi implementado e por quê
    - Liste as principais mudanças
    - Mencione testes adicionados
    - Use checklist se necessário

### Por Que Isso Importa

- **Demonstra profissionalismo** e conhecimento de workflows modernos
- **Facilita code review** com histórico organizado e incremental
- **Reflete práticas de mercado** usadas em empresas de tecnologia
- É uma **habilidade avaliada** no processo seletivo

---

## Dúvidas?

Se algo não estiver claro, por favor entre em contato.

Boa sorte!
