# ADR 002 — Kafka para Processamento Assíncrono

## Status
Aceito

## Contexto
O processamento de pedidos envolve múltiplas validações e operações de banco que podem levar tempo variável. A API precisa responder rapidamente ao cliente sem bloquear na lógica de negócio.

## Decisão
A criação do pedido persiste o registro com status `PENDING` e publica um evento `OrderCreated` no tópico `order-events`. Um consumer Kafka separado processa o evento de forma assíncrona e atualiza o status para `PROCESSED`, `FAILED` ou `PENDING_APPROVAL`.

## Consequências
**Positivas:**
- API responde imediatamente com `201 PENDING`, independente da complexidade do processamento
- Se o consumer cair, os eventos ficam retidos no Kafka e são processados quando ele voltar (at-least-once delivery)
- Producer e consumer são desacoplados e podem evoluir independentemente

**Negativas:**
- O cliente precisa consultar o pedido depois para saber o status final
- Requer infraestrutura adicional (Kafka + Zookeeper)
- Processamento idempotente é obrigatório para lidar com reentregas do Kafka
