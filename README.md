# Barbearia API

Uma API REST para organizar os atendimentos de uma barbearia: cadastro de clientes e barbeiros, autenticação e agendamentos.

Este é um projeto pessoal, criado para consolidar meus estudos de desenvolvimento backend com Java e Spring. Desenvolvi o projeto com apoio de inteligência artificial, usando-a para explorar soluções, revisar ideias e ajudar na documentação. O objetivo continua sendo aprender entendendo, testando e adaptando cada parte do código.

## Tecnologias

- Java 21 e Spring Boot
- Spring Web, Spring Data JPA e Spring Security
- Autenticação com JWT e senhas protegidas com BCrypt
- PostgreSQL em produção e H2 no desenvolvimento e nos testes
- Flyway para versionar as alterações do banco

## O que a API faz

- Cadastra usuários e autentica com JWT.
- Controla permissões de clientes e administradores.
- Gerencia clientes e barbeiros.
- Cria, consulta, cancela e conclui agendamentos.
- Valida o expediente e impede conflitos de horário.

## Rodar localmente

É necessário ter Java 21. No modo padrão, a aplicação usa o perfil `dev` e um banco H2 local.

```sh
./mvnw spring-boot:run
```

Os testes usam um banco H2 em memória separado. Com a aplicação rodando, a documentação da API fica disponível em `/swagger-ui.html`.

## Ambiente local com Docker

Com Docker Engine e o plugin Docker Compose instalados, copie o arquivo de exemplo, defina uma senha e um segredo JWT locais e inicie os serviços:

```sh
cp .env.example .env
```

Edite `.env` e substitua `DB_PASSWORD` e `JWT_SECRET`. O segredo JWT deve ter pelo menos 32 caracteres. Depois execute:

```sh
docker compose up --build
```

O Compose inicia o PostgreSQL e a API no perfil `prod`. A API aguarda o banco ficar saudável; na primeira inicialização, o Flyway aplica `V1__criar_esquema_inicial.sql`. A API fica em `http://localhost:8080`, e o Swagger em `http://localhost:8080/swagger-ui.html`.

Para parar os serviços, use `Ctrl+C` e depois `docker compose down`. Os dados ficam no volume `postgres-data` e são preservados. Para apagar também o banco local e começar do zero, execute `docker compose down -v`.

Este ambiente é para desenvolvimento local: o Compose usa uma única credencial do PostgreSQL para a aplicação e para as migrações. Não reutilize as credenciais do `.env` em produção.

### Teste automatizado do fluxo no Docker

Com a API e o PostgreSQL iniciados, instale `curl` e `jq` e execute o teste de fumaça:

```sh
./scripts/smoke-test.sh
```

O script solicita o email e a senha de uma conta `ADMIN`, sem gravá-los. Também aceita as variáveis `ADMIN_EMAIL`, `ADMIN_PASSWORD` e `API_URL` (padrão: `http://localhost:8080`). Ele cria um cliente e um barbeiro de teste, agenda um atendimento e confirma que o cliente consegue consultá-lo. Os registros ficam no banco para inspeção posterior; cada execução usa dados novos.

## PostgreSQL e migrações

O esquema é controlado pelo Flyway, com arquivos em `src/main/resources/db/migration`. Cada mudança futura deve entrar em uma nova migração SQL numerada (`V2__...sql`, `V3__...sql` etc.). Depois de aplicada em algum ambiente, uma migração não deve ser editada: crie outra versão.

No perfil `prod`, o Flyway aplica as migrações pendentes ao iniciar a aplicação. O Hibernate usa `ddl-auto=validate` para conferir se as entidades correspondem ao esquema, sem criar ou alterar tabelas.

### Criar um banco PostgreSQL vazio

Crie o banco e um usuário para executar as migrações. Em uma implantação, você também pode separar esse usuário do usuário da aplicação: o usuário de migração precisa de permissões para alterar o esquema; o da aplicação deve ter apenas as permissões necessárias para acessar os dados. O exemplo abaixo deve ser executado por um administrador do PostgreSQL, substituindo as senhas:

```sql
CREATE ROLE barbearia_migrator LOGIN PASSWORD 'senha-forte-da-migracao';
CREATE DATABASE barbearia OWNER barbearia_migrator;
```

Configure as variáveis de ambiente antes de iniciar:

```sh
export DB_URL='jdbc:postgresql://localhost:5432/barbearia'
export DB_USERNAME='barbearia_app'
export DB_PASSWORD='senha-forte-da-aplicacao'
export DB_MIGRATION_USERNAME='barbearia_migrator'
export DB_MIGRATION_PASSWORD='senha-forte-da-migracao'
export JWT_SECRET='uma-chave-secreta-longa-e-aleatoria'
export CORS_ALLOWED_ORIGINS='https://seu-frontend.example'
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

`DB_MIGRATION_USERNAME` e `DB_MIGRATION_PASSWORD` são opcionais. Se não forem informadas, o Flyway usa `DB_USERNAME` e `DB_PASSWORD`, que nesse caso também precisam ter permissão para executar migrações. Mantenha senhas e segredo JWT fora do Git.

O Flyway registra as versões aplicadas na tabela `flyway_schema_history`. Ao iniciar pela primeira vez um banco vazio, aplica `V1__criar_esquema_inicial.sql`. Essa migração foi feita para uma instalação vazia. Se o banco já tiver tabelas ou dados, faça backup e planeje a adoção do Flyway antes de iniciar a aplicação.

## Primeiro administrador

O cadastro público (`POST /auth/cadastro`) sempre cria um usuário `CLIENTE`. Para criar o primeiro `ADMIN`, configure temporariamente `BOOTSTRAP_ADMIN_EMAIL` e `BOOTSTRAP_ADMIN_PASSWORD` no ambiente em que a aplicação vai iniciar. A senha inicial precisa ter pelo menos 12 caracteres. Se o email ainda não existir, a aplicação cria o administrador com senha BCrypt; se já existir como `ADMIN`, a inicialização é repetível e não altera a senha. Se pertencer a um `CLIENTE`, a aplicação falha ao iniciar para evitar promover uma conta por engano. Use um email exclusivo e remova as variáveis de bootstrap depois da primeira inicialização.

Exemplo:

```sh
export BOOTSTRAP_ADMIN_EMAIL='admin@seudominio.com'
export BOOTSTRAP_ADMIN_PASSWORD='uma-senha-inicial-longa-e-unica'
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

Depois, autentique em `POST /auth/login` com essas credenciais. O administrador pode consultar, editar e remover clientes; criar, consultar, editar e remover barbeiros; e realocar agendamentos ainda ativos com `PUT /agendamentos/{id}`, informando `clienteId`, `barbeiroId` e `dataHora`. A realocação mantém as validações de expediente, conflito de horário e vínculo do cliente a uma conta. Novos clientes entram por `POST /auth/cadastro`, que cria a conta e o cadastro de cliente vinculados na mesma operação; não há criação manual de cliente sem usuário.

## Ideias para continuar

Quero seguir evoluindo o projeto conforme avanço nos estudos. Entre as possibilidades estão criar um frontend e experimentar recursos de IA para apoiar a organização dos agendamentos e consultar informações sobre a frequência dos clientes.
