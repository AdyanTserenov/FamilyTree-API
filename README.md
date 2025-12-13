# Family Tree API

This project has been refactored into a multi-service architecture with a reusable Spring Boot starter for authentication.

## Modules

- **family-tree-auth-starter**: A Spring Boot starter providing authentication, user management, JWT security, email services, and user/token models.
- **tree-service**: The main service handling family tree operations, using the auth starter for authentication.

## Architecture

- Shared PostgreSQL database.
- Auth starter includes controllers for auth endpoints (/auth/*), profile (/profile/*).
- Tree service includes controllers for tree management (/trees/*).

## Setup

1. Ensure Java 17 and Maven are installed.
2. Start PostgreSQL:
   ```bash
   docker-compose up -d
   ```
3. Build the project:
   ```bash
   mvn clean install
   ```
4. Run the tree-service:
   ```bash
   cd tree-service
   mvn spring-boot:run
   ```

## Configuration

Update `tree-service/src/main/resources/application.properties` with your mail and JWT settings.

## API Endpoints

- Auth: /auth/signin, /auth/signup, etc.
- Profile: /profile
- Trees: /trees/create, /trees/{id}/invite, etc.

## Next Steps

- Add Person model and controllers in tree-service.
- Implement frontend or additional services.
- Add tests.