# Coupon API

API REST para cadastro, consulta por UUID e exclusão lógica de cupons de desconto.

## Funcionalidades

- Cadastro de cupom com normalização do código e validação das regras de negócio.
- Consulta de cupom por UUID.
- Exclusão lógica, que preserva os dados cadastrados e não pode ser repetida.

## Tecnologias

| Finalidade | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA) |
| Banco de dados | PostgreSQL 18 |
| Migrações | Flyway |
| Documentação da API | springdoc-openapi 3.1.1 (Swagger UI) |
| Testes | JUnit 6, AssertJ, Testcontainers |
| Cobertura | JaCoCo 0.8.13 |
| Build | Maven Wrapper (Maven 3.9.6) |
| Containers | Docker e Docker Compose |

## Pré-requisitos

- Docker em execução e Docker Compose, para subir a aplicação e rodar os testes de integração.
- JDK 21, apenas para executar a aplicação ou os testes fora do Docker.

## Como executar

### Com Docker

O Compose sobe o PostgreSQL e a aplicação, aguarda o banco ficar saudável e aplica as migrações automaticamente.

```sh
docker compose up --build -d
docker compose logs -f app
```

Se o plugin Compose não estiver disponível, substitua `docker compose` por `docker-compose` em todos os comandos.

| Recurso | Endereço |
| --- | --- |
| API | http://localhost:8080/coupon |
| Swagger UI | http://localhost:8080/swagger-ui/index.html |
| OpenAPI (JSON) | http://localhost:8080/v3/api-docs |

Para parar:

```sh
docker compose down      # remove os containers e preserva os dados
docker compose down -v   # remove também o volume com os dados locais
```

### Aplicação local com banco no Docker

```sh
docker compose up -d postgres
./mvnw spring-boot:run
```

A primeira execução do Maven Wrapper precisa de acesso à internet para baixar o Maven e as dependências.

## Configuração

A aplicação lê as variáveis de ambiente abaixo. Os valores padrão já correspondem ao banco do `compose.yaml`.

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/tickets` | URL JDBC do PostgreSQL |
| `DB_USERNAME` | `tickets` | Usuário do banco |
| `DB_PASSWORD` | `tickets` | Senha do banco |
| `SERVER_PORT` | `8080` | Porta HTTP da aplicação |

As credenciais e portas do `compose.yaml` são fixas e destinadas ao desenvolvimento local. Aplicação e banco ficam expostos somente em `127.0.0.1`.

## API

| Método | Rota | Sucesso | Erros |
| --- | --- | --- | --- |
| `POST` | `/coupon` | `201` com o cupom | `400` entrada inválida |
| `GET` | `/coupon/{id}` | `200` com o cupom | `400` UUID malformado, `404` inexistente ou deletado |
| `DELETE` | `/coupon/{id}` | `204` sem corpo | `400` UUID malformado, `404` inexistente, `409` já deletado |

### Criar cupom

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `code` | string | Sim | Seis caracteres alfanuméricos após a remoção de caracteres especiais |
| `description` | string | Sim | Não pode ser vazia |
| `discountValue` | number | Sim | Mínimo de `0.5` |
| `expirationDate` | string | Sim | ISO-8601 com fuso; não pode estar no passado |
| `published` | boolean | Não | Padrão `false` |

```sh
curl -i -X POST http://localhost:8080/coupon \
  -H 'Content-Type: application/json' \
  -d '{"code":"ABC-123","description":"Desconto na próxima compra","discountValue":0.8,"expirationDate":"2099-12-31T23:59:59Z","published":true}'
```

```json
{
  "id": "071167b0-fa70-47c4-b88a-e1b70ea94c40",
  "code": "ABC123",
  "description": "Desconto na próxima compra",
  "discountValue": 0.8,
  "expirationDate": "2099-12-31T23:59:59Z",
  "status": "ACTIVE",
  "published": true,
  "redeemed": false
}
```

### Consultar e excluir

```sh
curl -i http://localhost:8080/coupon/{id}
curl -i -X DELETE http://localhost:8080/coupon/{id}
```

### Formato de erro

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Expiration date must not be in the past.",
  "instance": "/coupon",
  "field": "expirationDate"
}
```

## Regras de negócio

- `code`, `description`, `discountValue` e `expirationDate` são obrigatórios.
- O código tem os caracteres fora de `A–Z`, `a–z` e `0–9` removidos e precisa terminar com exatamente seis caracteres. Não há truncamento nem conversão de maiúsculas e minúsculas, Ex.: `ABC-123` vira `ABC123`.
- O desconto tem mínimo de `0.5` e não tem valor máximo.
- A data de expiração não pode estar no passado no momento da criação.
- Um cupom pode ser criado já publicado. Se `published` não for enviado ou for nulo a API assume = `false`.
- A API define o UUID, `status=ACTIVE` e `redeemed=false`. Campos extras no JSON, como `id`, `status` ou `redeemed` são ignorados.
- A exclusão muda o status para `DELETED` e registra a data preservando os dados cadastrados.
- Um cupom já deletado não pode ser deletado mais de uma vez, a segunda tentativa retorna `409` e mantém a data original.

## Decisões de projeto

- **PostgreSQL em vez de H2:** A especificação pede H2 em memória. A entrega usa PostgreSQL para validar o bloqueio da exclusão e o soft delete em um banco real, por isso a aplicação e os testes de integração precisam de Docker.
- **Códigos repetidos são permitidos:** A especificação não estabelece unicidade, sendo assim cada cupom é identificado pelo UUID.
- **Exclusões simultâneas:** A exclusão mantém um bloqueio pessimista da linha durante a transação de várias exclusões simultâneas do mesmo cupom, uma tem sucesso e as demais recebem `409`.
- **Precisão do desconto:** No código é utilizado `BigDecimal` e o banco usa `numeric` sem escala fixa e sem arredondamento.
- **Status `INACTIVE`:** Está declarado porque faz parte do contrato, mas nenhuma regra da especificação leva um cupom para esse estado.

## Arquitetura

O projeto segue arquitetura hexagonal. As dependências apontam para dentro: `infrastructure` conhece `application`, que conhece `domain`. As camadas `domain` e `application` não importam Spring nem JPA.

```text
src/main/java/com/larissafalcao/tickets_api
├── domain/coupon               Coupon, objetos de valor e regras de negócio
├── application/coupon          Casos de uso e dados de entrada e saída
├── application/port            Interfaces de repositório, horário e transação
└── infrastructure
    ├── web                     Controller, requisição e tratamento de erros
    ├── persistence             Entidade JPA e acesso ao PostgreSQL
    └── config                  Implementações das portas e registro dos casos de uso
```

## Logs


```text
Coupon created: id=071167b0-fa70-47c4-b88a-e1b70ea94c40
Coupon retrieved: id=071167b0-fa70-47c4-b88a-e1b70ea94c40
Coupon deleted: id=071167b0-fa70-47c4-b88a-e1b70ea94c40
Coupon rejected: field=discountValue reason=Discount value must be at least 0.5.
Request rejected: status=409 reason=Coupon 071167b0-fa70-47c4-b88a-e1b70ea94c40 has already been deleted.
Request rejected: status=400 cause=HttpMessageNotReadableException
```

## Testes

```sh
./mvnw test          # domínio, casos de uso e arquitetura; não exige Docker
./mvnw clean verify  # todos os testes, build e verificação de cobertura; exige Docker
```


O JaCoCo exige pelo menos 80% de linhas e ramificações nos pacotes de domínio e application. Relatório: `target/site/jacoco/index.html`.

## Solução de problemas

**Porta 8080 ou 5432 ocupada.** Altere o lado esquerdo do mapeamento em `ports` no `compose.yaml`, por exemplo `127.0.0.1:18080:8080`. Se mudar a porta do banco e rodar a aplicação fora do Docker informe a mesma porta:

```sh
DB_URL=jdbc:postgresql://localhost:55432/tickets ./mvnw spring-boot:run
```

**Se Testcontainers não encontrar o Docker com Colima (macOS).**

```sh
DOCKER_HOST="unix://${HOME}/.colima/default/docker.sock" \
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock \
./mvnw clean verify
```

