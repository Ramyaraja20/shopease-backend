# ShopEase – E-commerce Backend API

A REST API for an online shop, built with **Java, Spring Boot and MySQL**. It covers user accounts with JWT login, a product catalog with search and pagination, a shopping cart, and orders with safe stock handling. Users and admins have different permissions.

**API docs (Swagger UI):** `http://localhost:8080/swagger-ui/index.html` when running locally

## Tech stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4 (Spring Web, Spring Data JPA, Validation) |
| Security | Spring Security, JWT, BCrypt password hashing |
| Database | MySQL 8, Hibernate (JPA) |
| API docs | springdoc OpenAPI / Swagger UI |
| Testing | JUnit 5, Mockito |
| Build | Maven |

## Features

- **Authentication:** register and log in; the server returns a signed JWT that is sent in the `Authorization: Bearer <token>` header
- **Roles:** `USER` and `ADMIN`; admin-only endpoints are protected with `@PreAuthorize`
- **Products and categories:** public browsing, admin-only create, update and delete; search by name and pagination
- **Shopping cart:** add, update and remove items; stock is checked and totals are calculated on the server
- **Orders:** place an order from the cart, view your own orders, cancel an order that is still `PLACED` (stock is restored)
- **Admin order management:** list all orders and move them through `PLACED → PAID → SHIPPED → DELIVERED`
- **Consistent error responses:** one JSON format for validation errors, not-found, conflicts and unexpected errors
- **Unit tests** for the JWT, authentication and order logic (14 tests)

## API overview

Full interactive documentation is available in Swagger UI.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create an account |
| POST | `/api/auth/login` | Public | Log in and receive a JWT |
| GET | `/api/users/me` | Logged in | Current user profile |
| GET | `/api/categories` | Public | List categories |
| POST | `/api/categories` | Admin | Create a category |
| GET | `/api/products` | Public | List or search products (`search`, `page`, `size`) |
| GET | `/api/products/{id}` | Public | Get one product |
| POST | `/api/products` | Admin | Create a product |
| PUT | `/api/products/{id}` | Admin | Update a product |
| DELETE | `/api/products/{id}` | Admin | Delete a product |
| GET | `/api/cart` | Logged in | View your cart |
| POST | `/api/cart/items` | Logged in | Add a product to your cart |
| PUT | `/api/cart/items/{itemId}` | Logged in | Change an item's quantity |
| DELETE | `/api/cart/items/{itemId}` | Logged in | Remove an item |
| POST | `/api/orders` | Logged in | Place an order from your cart |
| GET | `/api/orders` | Logged in | List your orders |
| GET | `/api/orders/{id}` | Logged in | View one of your orders |
| PUT | `/api/orders/{id}/cancel` | Logged in | Cancel an order in `PLACED` status |
| GET | `/api/admin/orders` | Admin | List all orders (paginated) |
| PUT | `/api/admin/orders/{id}/status` | Admin | Move an order to its next status |

## Getting started

### Prerequisites

- Java 17
- MySQL 8 running locally

### Configuration

Secrets are read from environment variables, so nothing sensitive is stored in the code.

| Variable | Description |
|---|---|
| `DB_PASSWORD` | Password of your MySQL `root` user |
| `JWT_SECRET` | Secret used to sign tokens, at least 32 characters |

The database `shopease_db` and all tables are created automatically on the first start.

### Run locally

```bash
git clone https://github.com/<Ramyaraja20>/shopease-backend.git
cd shopease-backend
```

Windows (Command Prompt):

```bash
set DB_PASSWORD=your_mysql_password
set JWT_SECRET=a-long-random-secret-of-at-least-32-characters
mvnw.cmd spring-boot:run
```

macOS / Linux:

```bash
export DB_PASSWORD=your_mysql_password
export JWT_SECRET=a-long-random-secret-of-at-least-32-characters
./mvnw spring-boot:run
```

Then open `http://localhost:8080/swagger-ui/index.html`.

### Try it in Swagger UI

1. Call `POST /api/auth/register`, then `POST /api/auth/login`.
2. Copy the token from the response, click **Authorize**, and paste it.
3. Call the protected endpoints (cart, orders, `/api/users/me`).

### Create an admin

Every new account is a `USER`. To make an account an admin, run this in MySQL after registering:

```sql
USE shopease_db;
UPDATE users SET role = 'ADMIN' WHERE id = 1;
```

## Running the tests

```bash
mvnw.cmd test
```

(Use `./mvnw test` on macOS / Linux.) The unit tests use Mockito, so they run without a database.

## Design decisions

- **Stateless JWT security:** the server stores no sessions. Tokens expire after one hour.
- **Passwords:** hashed with BCrypt, never stored or returned in plain text.
- **DTOs:** entities are never exposed directly. Clients cannot set fields like `role`, and responses never contain the password hash.
- **No IDOR:** carts and orders are always looked up from the logged-in user's identity, never from a user id sent by the client.
- **Safe orders:** placing an order runs in one transaction. Product rows are locked (`SELECT ... FOR UPDATE`) in a fixed order, so concurrent buyers cannot oversell stock and deadlocks are avoided. If any step fails, nothing is saved.
- **Price history:** each order item stores `priceAtPurchase`, so old orders keep the price that was paid.
- **Order status flow:** a small state machine only allows valid status changes; invalid ones return `409 Conflict`.
- **Money:** `BigDecimal` is used for all prices and totals.
- **Central error handling:** a `@RestControllerAdvice` returns the same JSON error format everywhere.

## Project structure

```
src/main/java/com/shopease/shopease
├── config        # Security, JWT filter, OpenAPI
├── controller    # REST controllers
├── dto           # Request and response records
├── entity        # JPA entities
├── exception     # Global exception handling
├── repository    # Spring Data JPA repositories
└── service       # Business logic
```

## Possible improvements

- Soft delete for products, so products that appear in orders can be hidden instead of deleted
- Refresh tokens and logout
- Real payment integration
- Integration tests with a real database (Testcontainers)
- Fetch joins to avoid N+1 queries in order lists

## Author

Built by **Ramya R** as a portfolio project.