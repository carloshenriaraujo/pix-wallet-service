# Pix Wallet Service 

> Microserviço de **carteira digital com suporte a Pix**, construído com **Spring Boot 3**, **Java 17** e **PostgreSQL**, seguindo princípios de **Clean Architecture**, **idempotência** e **consistência sob concorrência**.

---

## Executando o projeto

### 1️⃣ Clonar o repositório
```bash
git clone https://github.com/carloshenriaraujo/pix-wallet-service.git
cd pix-wallet-service
```

2️⃣ Subir o banco de dados (Docker)
```bash
docker compose up -d
```

Informações de user,senha e porta estão no arquivo : 

```bash
env.env
DB_NAME=postgres
DB_USER=postgres
DB_PASS=postgres
DB_PORT=5432
```

3️⃣ Rodar a aplicação
```bash
./mvnw spring-boot:run
```
A API subirá em http://localhost:8096

# Documentação automática (Swagger)

Acesse:
 http://localhost:8096/swagger-ui.html

Lá você encontra todos os endpoints prontos, com exemplos preenchidos automaticamente:
```bash

/wallets → Cria carteira

/wallets/{id}/pix-keys → Registra chave Pix

/wallets/{id}/deposit → Faz depósito

/wallets/{id}/withdraw → Faz saque

/wallets/{id}/balance → Consulta saldo (atual ou histórico)

/pix/transfers → Inicia Pix (idempotente)

/pix/webhook → Processa eventos CONFIRMED/REJECTED
```

# Decisões de Design

### Clean Architecture
``` bash

api/              -> controllers e DTOs
application/      -> casos de uso (interfaces + impl)
domain/           -> entidades e enums
infrastructure/   -> repositórios e configs
```

Persistência & Consistência

* Banco: PostgreSQL
* Transações ACID com @Transactional
* Lock pessimista (SELECT FOR UPDATE) para depósitos/saques
* Lock otimista (@Version) em PixTransfer para concorrência de webhooks

### Idempotência

* Transfers: idempotencyKey única por (walletId, idempotencyKey)
* Webhooks: eventId único na tabela webhook_events

### Ledger e Auditoria

* Todas as movimentações registradas em ledger_entries
* Cálculo de saldo histórico com base no ledger
* Logs estruturados em formato JSON (timestamp, logger, message)

### Máquina de Estados Pix
``` bash

PENDING  →  CONFIRMED (credita destino)
PENDING  →  REJECTED  (estorna origem)
CONFIRMED ou REJECTED → ignora duplicados/invertidos
```

### Observabilidade

* Actuator em /actuator/health e /actuator/metrics
* Logs estruturados (application.yml já pronto)
* Swagger UI com exemplos pré-preenchidos

### Testes

* +85% Coverage 
* Base de dados H2 (integrados)
* spring-boot-starter-test com JUnit 5 e Mockito
* Testes de integração validando idempotência de transferências e webhooks
* Testes unitários dos casos de uso
* Teste integração do valida carteira

### Tempo investido

Aproximadamente 15 horas (implementação, testes e documentação).

## Próximos passos (se fosse produção)

* Autenticação JWT (Spring Security)
* Tabelas de auditoria por tenant
* Observabilidade com Prometheus + Grafana
* Retentativas assíncronas de webhooks com Dead Letter Queue