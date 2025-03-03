# Investment Dashboard

A fullstack application for monitoring investment portfolios in real-time.

## Technologies

- Backend: Java 17/21 with Spring Boot 3.x
- Frontend: Angular 17
- Database: PostgreSQL 15

## Development Setup

### Prerequisites

- JDK 17 
- Node.js 20.x and npm
- Docker and Docker Compose
- Angular CLI

### Running the Application

1. Start the database:
   docker-compose up -d
   Copy
2. Start the backend:
   cd backend
   ./mvnw spring-boot:run
   Copy
3. Start the frontend:
   cd frontend/investment-dashboard-frontend
   npm install
   npm start
   Copy
4. Access the application:
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api
- PgAdmin: http://localhost:5050 (admin@admin.com/admin)

## Project Structure

- `/backend` - Spring Boot application
- `/frontend` - Angular application
- `/docs` - Project documentation