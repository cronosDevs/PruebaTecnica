# Products API — Backend

API REST para la gestión de productos, desarrollada con Spring Boot. Incluye autenticación JWT, persistencia en PostgreSQL y mensajería asíncrona mediante RabbitMQ.

---

## Requisitos previos

| Herramienta     | Versión mínima |
|-----------------|----------------|
| Java (JDK)      | 21             |
| Maven           | 3.9+           |
| PostgreSQL      | 15+            |

---

## Stack tecnológico

| Componente              | Versión   |
|-------------------------|-----------|
| Spring Boot             | 3.3.4     |
| Spring Security         | (Boot BOM)|
| Spring Data JPA         | (Boot BOM)|
| PostgreSQL Driver       | (Boot BOM)|
| JJWT                    | 0.12.3    |
| Lombok                  | (Boot BOM)|

---

## Configuración de la base de datos

### 1. Crear la base de datos

```sql
CREATE DATABASE products_db;
```

### 2. Crear la tabla de productos

```sql
CREATE TABLE public.products (
    id      serial         NOT NULL,
    nombre  varchar(255)   NOT NULL,
    precio  numeric(38, 2) NOT NULL,
    stock   int4           NOT NULL,
    CONSTRAINT products_pkey       PRIMARY KEY (id),
    CONSTRAINT products_stock_check CHECK ((stock >= 0))
);
```

> La aplicación tiene `spring.jpa.hibernate.ddl-auto=none`, por lo que el esquema **debe crearse manualmente** antes de arrancar.

---

## Variables de configuración

Editar `src/main/resources/application.properties` con los valores del entorno:

```properties
# Servidor
server.port=8080

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/products_db
spring.datasource.username=<DB_USER>
spring.datasource.password=<DB_PASSWORD>

# JWT
app.jwt.secret=<SECRET_HEX_64_CHARS>
app.jwt.expiration=86400000          # 24 horas en milisegundos


---

## Levantar el proyecto

### Opción A — Maven Wrapper (sin instalar Maven)

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

### Opción B — Maven instalado

```bash
mvn spring-boot:run
```

### Opción C — JAR ejecutable

```bash
mvn clean package -DskipTests
java -jar target/products-api-0.0.1-SNAPSHOT.jar
```

La aplicación estará disponible en `http://localhost:8080`.

---

## Autenticación

La API usa **JWT Bearer Token**. Todos los endpoints bajo `/api/products` requieren autenticación.

### Usuario por defecto (hardcoded)

| Campo    | Valor      |
|----------|------------|
| username | `admin`    |
| password | `admin123` |

### 1. Obtener token

**POST** `/api/auth/login`

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Respuesta:

```json
{
  "token": "<JWT>"
}
```

### 2. Usar el token

Incluir en el header de cada petición:

```
Authorization: Bearer <JWT>
```

---

## Endpoints de la API

### Productos — `/api/products`

| Método | Ruta                  | Descripción              | Auth |
|--------|-----------------------|--------------------------|------|
| GET    | `/api/products`       | Listar todos             | Si   |
| GET    | `/api/products/{id}`  | Obtener por ID           | Si   |
| POST   | `/api/products`       | Crear producto           | Si   |
| PUT    | `/api/products/{id}`  | Actualizar producto      | Si   |
| DELETE | `/api/products/{id}`  | Eliminar producto        | Si   |

### Autenticación — `/api/auth`

| Método | Ruta             | Descripción    | Auth |
|--------|------------------|----------------|------|
| POST   | `/api/auth/login`| Obtener token  | No   |

---

## Modelo de producto

```json
{
  "id":     1,
  "nombre": "Laptop",
  "precio": 999.99,
  "stock":  50
}
```

### Reglas de validación

| Campo    | Restricción                        |
|----------|------------------------------------|
| `nombre` | Requerido, no vacío                |
| `precio` | Requerido, valor positivo          |
| `stock`  | Requerido, mayor o igual a 0       |

---

## Mensajería RabbitMQ

La aplicación publica eventos de tipo `InvoiceCreatedEvent` al exchange `invoice.exchange` con dos routing keys:

| Routing Key             | Cola destino                  |
|-------------------------|-------------------------------|
| `invoice.stock`         | `invoice.stock.queue`         |
| `invoice.notification`  | `invoice.notification.queue`  |

### Estructura del evento

```json
{
  "invoiceId":   "INV-001",
  "productId":   1,
  "productName": "Laptop",
  "quantity":    2,
  "unitPrice":   999.99
}
```

---

## Estructura del proyecto

```
src/
└── main/
    ├── java/com/example/demo/
    │   ├── PruebaTecnicaApplication.java
    │   ├── config/
    │   │   └── SecurityConfig.java
    │   ├── controller/
    │   │   ├── AuthController.java
    │   │   └── ProductController.java
    │   ├── dto/
    │   │   ├── AuthRequest.java
    │   │   ├── AuthResponse.java
    │   │   ├── InvoiceCreatedEvent.java
    │   │   └── ProductDTO.java
    │   ├── entity/
    │   │   └── Product.java
    │   ├── exception/
    │   │   ├── GlobalExceptionHandler.java
    │   │   └── ResourceNotFoundException.java
    │   ├── repository/
    │   │   └── ProductRepository.java
    │   ├── security/
    │   │   ├── JwtAuthFilter.java
    │   │   ├── JwtService.java
    │   │   └── UserDetailsServiceImpl.java
    │   └── service/
    │       ├── ProductService.java
    │       └── ProductServiceImpl.java
    └── resources/
        └── application.properties
```

---

## Manejo de errores

| Código HTTP | Situación                              |
|-------------|----------------------------------------|
| 400         | Datos de entrada inválidos (validación)|
| 401         | Credenciales incorrectas / token ausente|
| 404         | Recurso no encontrado                  |
| 500         | Error interno del servidor             |

---

## Notas de seguridad

- El secreto JWT configurado por defecto es solo para desarrollo. **Cambiar en producción.**
- El usuario `admin` está definido en memoria (`UserDetailsServiceImpl`). Para producción se recomienda persistir usuarios en base de datos.
- CORS habilitado únicamente para `http://localhost:4200` (frontend Angular en desarrollo).
