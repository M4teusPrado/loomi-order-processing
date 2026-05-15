# ADR 004 — Snapshot de Preço no Momento da Compra

## Status
Aceito

## Contexto
O preço dos produtos pode mudar ao longo do tempo. Um pedido criado hoje precisa registrar o valor que o cliente pagou, independentemente de mudanças futuras no catálogo.

## Decisão
No momento da criação do pedido, o `OrderService` busca o preço atual do produto no catálogo e armazena em `OrderItem.unitPrice`. O preço nunca vem do cliente na requisição — apenas `productId` e `quantity` são aceitos. O total do pedido é calculado com os preços coletados do backend.

## Consequências
**Positivas:**
- Histórico de pedidos é imutável e auditável — reflete exatamente o que foi cobrado
- Previne manipulação de preço pelo cliente
- Mudanças no catálogo não afetam pedidos existentes

**Negativas:**
- Se o preço mudar entre a criação e o processamento assíncrono, o pedido usa o preço do momento da criação (comportamento correto e esperado)
