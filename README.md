
# Java Project Documentation 

## Structure of REST Resources

### 1. **AuthenticationResource** (`/auth`)
This resource handles user authentication and management operations.

#### Endpoints
- **POST /register**: Registers a new user.
  - **Input**: `CreateUserRequest` (JSON)
  - **Output**: `CreateUserResponse` (JSON)

- **POST /verify**: Verifies a user's token.
  - **Input**: `token` (JSON)
  - **Output**: Success or error message (JSON)

- **POST /login**: Logs the user in.
  - **Input**: `LoginRequest` (JSON)
  - **Output**: Session details, user role (JSON)

- **DELETE /logout**: Logs the user out.
  - **Input**: `SESSION_COOKIE` (cookie)
  - **Output**: Empty response

- **GET /profile**: Retrieves the authenticated user's profile.
  - **Input**: `SESSION_COOKIE` (cookie)
  - **Output**: `CreateUserResponse` (JSON)

- **GET /clients**: Retrieves all clients (admin only).
  - **Input**: `SESSION_COOKIE` (cookie)
  - **Output**: List of users (JSON)

- **DELETE /delete/{id}**: Deletes a specific user (admin only).
  - **Input**: `SESSION_COOKIE` (cookie), `id` (path)
  - **Output**: Empty response

### 2. **ProductResource** (`/api/product`)
This resource manages product operations.

#### Endpoints
- **POST /**: Creates a new product (admin only).
  - **Input**: `Product` (JSON)
  - **Output**: Created `Product` (JSON)

- **GET /**: Retrieves all products.
  - **Output**: List of products (JSON)

- **GET /export**: Exports products in Excel format (admin only).
  - **Output**: Excel file

- **GET /{id}**: Retrieves a product by ID.
  - **Input**: `id` (path)
  - **Output**: `Product` (JSON)

- **GET /{name}**: Retrieves a product by name.
  - **Input**: `name` (path)
  - **Output**: `Product` (JSON)

- **PUT /{id}**: Updates the stock of a product (admin only).
  - **Input**: `SESSION_COOKIE` (cookie), `id` (path), updated `Product` (JSON)
  - **Output**: Empty response

- **PUT /{name}**: Updates product details (admin only).
  - **Input**: `SESSION_COOKIE` (cookie), `name` (path), updated `Product` (JSON)
  - **Output**: Updated `Product` (JSON)

- **DELETE /{id}**: Deletes a product (admin only).
  - **Input**: `SESSION_COOKIE` (cookie), `id` (path)
  - **Output**: Empty response

### 3. **OrderResource** (`/api/order`)
This resource handles order operations.

#### Endpoints
- **POST /**: Creates a new order.
  - **Input**: `OrderMongo` (JSON)
  - **Output**: Status response (JSON)

- **PUT /{id}**: Updates an existing order (admin only).
  - **Input**: `SESSION_COOKIE` (cookie), `id` (path), `OrderMongo` (JSON)
  - **Output**: Success or error message

- **PUT /accept/{id}**: Accepts an order (admin only).
  - **Input**: `SESSION_COOKIE` (cookie), `id` (path)
  - **Output**: Success or error message

- **PUT /deliver/{id}**: Marks an order as delivered (admin only).
  - **Input**: `SESSION_COOKIE` (cookie), `id` (path)
  - **Output**: Success or error message

- **DELETE /{id}**: Deletes an order.
  - **Input**: `id` (path)
  - **Output**: Success or error message

- **GET /**: Retrieves all orders (admin only).
  - **Input**: `SESSION_COOKIE` (cookie)
  - **Output**: List of orders (JSON)

- **GET /{id}**: Retrieves an order by ID.
  - **Input**: `id` (path)
  - **Output**: `OrderMongo` (JSON)

- **GET /user/{email}**: Retrieves orders of a user by email.
  - **Input**: `email` (path)
  - **Output**: List of orders (JSON)

- **GET /date/{date}**: Retrieves orders for a specific date (admin only).
  - **Input**: `SESSION_COOKIE` (cookie), `date` (path)
  - **Output**: List of orders (JSON)

- **GET /date/{date}/export**: Exports orders for a specific date in Excel format (admin only).
  - **Input**: `SESSION_COOKIE` (cookie), `date` (path)
  - **Output**: Excel file

