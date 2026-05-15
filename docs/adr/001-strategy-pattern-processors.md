# ADR 001 — Strategy Pattern para Processadores de Pedido

## Status
Aceito

## Contexto
O sistema precisa processar cinco tipos de pedido com regras completamente diferentes: PHYSICAL verifica estoque, SUBSCRIPTION valida limites e compatibilidade, DIGITAL controla licenças, PRE_ORDER valida slots e datas, CORPORATE valida CNPJ e crédito. A lógica de cada tipo cresce independentemente.

## Decisão
Cada tipo de produto tem seu próprio `OrderProcessor` que implementa a interface `OrderProcessor`. O `OrderProcessingService` recebe todos os processors via injeção de dependência e seleciona o correto pelo método `getSupportedType()`.

## Consequências
**Positivas:**
- Adicionar um novo tipo de pedido não exige alterar código existente (Open/Closed Principle)
- Cada processor é testável isoladamente sem depender dos outros
- A lógica de negócio fica encapsulada no lugar certo

**Negativas:**
- Para pedidos com itens de tipos diferentes, o resultado final depende da lógica de agregação no `OrderProcessingService` — um item FAILED faz o pedido inteiro falhar
