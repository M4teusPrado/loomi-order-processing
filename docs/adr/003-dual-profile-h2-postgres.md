# ADR 003 — Perfis Separados: H2 para Dev Local, PostgreSQL para Produção

## Status
Aceito

## Contexto
O desenvolvimento local exige agilidade — ter que subir Docker a cada mudança de código torna o ciclo lento. Ao mesmo tempo, o ambiente de produção precisa de PostgreSQL real com Flyway migrations.

## Decisão
Dois perfis Spring:
- **default** (`application.yml`): H2 in-memory, Flyway desabilitado, Kafka excluído via `autoconfigure.exclude`. Roda com `mvn spring-boot:run` sem nenhuma dependência externa.
- **dev** (`application-dev.yml`): PostgreSQL + Kafka + Flyway. Ativado via `SPRING_PROFILES_ACTIVE=dev` no Docker Compose.

O perfil `application-default.yml` exclui `KafkaAutoConfiguration` especificamente para o perfil default, sem afetar o perfil dev.

## Consequências
**Positivas:**
- Desenvolvimento local sem Docker é possível e rápido
- Testes unitários rodam sem infraestrutura externa
- A troca de banco é transparente — mesmo código, configuração diferente

**Negativas:**
- H2 e PostgreSQL têm dialectos SQL diferentes; queries específicas do PostgreSQL (ex: JSONB) não funcionam no H2
- Migrations Flyway precisam ser compatíveis com o PostgreSQL, não com H2
