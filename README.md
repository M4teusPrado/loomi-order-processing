# Order Processing System

Backend system for async order processing using Spring Boot, PostgreSQL and Apache Kafka.

## How to Run

```bash
make up
```

Application available at `http://localhost:8080`.

## Commands

| Command | Description |
|---|---|
| `make up` | Build and start all containers |
| `make down` | Stop containers |
| `make test` | Run tests |
| `make logs` | Follow app logs |
| `make clean` | Stop and remove volumes |

## API

### Create Order
```
POST /api/orders
```

### Get Order
```
GET /api/orders/{orderId}
GET /api/orders?customerId={customerId}
```
