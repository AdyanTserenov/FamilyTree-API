
# High-Level Architecture Overview

## Overview
The FamilyTreeAPI project is a Spring Boot-based application focused on managing user authentication and family tree data. It uses a modular architecture with clear separation of concerns.

## Key Components

### 1. Application Entry Point
- `FamilyTreeApplication.java`: The main class that boots up the Spring Boot application. It includes `@SpringBootApplication` and `@EnableScheduling` annotations, indicating support for scheduled tasks.

### 2. Configuration
- `application.properties`: Contains key configuration settings, including:
  - Database connection details (PostgreSQL)
  - Server port (8080)
  - JPA/Hibernate settings
  - Swagger/OpenAPI configuration
  - Mail sender configuration (SMTP for Yandex Mail)

### 3. Security & Authentication
- `SecurityConfig.java`: Configures Spring Security with:
  - CSRF protection disabled
  - CORS enabled with default values
  - Stateless session management
  - Custom authentication entry point returning 401 for unauthorized access
  - Authorization rules:
    - `/auth/**` endpoints are publicly accessible
    - `/profile/**` and `/trees/**` require authentication
    - All other requests are permitted by default
  - Custom `TokenFilter` for token-based authentication

### 4. Domain Models
- `User.java`: Core domain model representing a user with:
  - Unique email
  - First, last, and optional middle name
  - Password
  - Timestamps for creation and last update
  - Enabled flag
  - Hibernate annotations for database mapping

### 5. Exception Handling
- `GlobalExceptionHandler.java`: Centralized exception handling with:
  - Specific handlers for validation errors, not found exceptions, invalid requests, registration failures, email sending errors, and access denied scenarios
  - Generic runtime exception handler for unforeseen errors
  - Consistent response format using `CustomApiResponse`

### 6. Other Key Modules
- **Controllers**: Located in `controllers` package, handle HTTP requests for profiles, security, and trees.
- **DTOs**: In `dto` package, used for request/response mapping.
- **Repositories**: In `repositories` package, provide data access layer for entities.
- **Services**: In `services` package, contain business logic for user, tree, and email operations.
- **Configurators**: Additional configuration classes for Swagger, security, and token cleanup.

## Data Flow
1. Incoming requests are processed by controllers.
2. Controllers interact with services for business logic.
3. Services use repositories to access the database.
4. Security is handled by Spring Security filters (including custom `TokenFilter`).
5. Exceptions are caught by `GlobalExceptionHandler` and returned as standardized API responses.

## Technologies Used
- Spring Boot
- Spring Security
- JPA/Hibernate
- PostgreSQL
