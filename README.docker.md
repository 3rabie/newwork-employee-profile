# Docker Deployment Guide

This guide explains how to run the Employee Profile Management System using Docker Compose.

## Prerequisites

- Docker (version 20.10+)
- Docker Compose (version 2.0+)

## Quick Start

1. **Copy environment file**:
   ```bash
   cp .env.example .env
   ```

2. **Update environment variables** in `.env`:
   - Set a strong `JWT_SECRET` (minimum 256 bits for HS256)
   - Add your `APP_AI_HF_API_KEY` for HuggingFace API
   - Adjust other settings as needed

3. **Build and start all services**:
   ```bash
   docker-compose up -d
   ```

4. **Access the application**:
   - Frontend: http://localhost (port 80)
   - Backend API: http://localhost:8080
   - GraphQL Playground: http://localhost:8080/graphiql
   - API Docs (Swagger): http://localhost:8080/swagger-ui.html

## Architecture

The Docker Compose setup includes three services:

### 1. PostgreSQL Database (`postgres`)
- **Image**: `postgres:15-alpine`
- **Port**: 5432
- **Database**: employee_profile
- **Credentials**: postgres/postgres (change in production!)
- **Volume**: `postgres_data` for data persistence

### 2. Spring Boot Backend (`backend`)
- **Build**: Multi-stage build using Maven
- **Port**: 8080
- **Health Check**: `/actuator/health` endpoint
- **Wait Strategy**: Waits for database to be healthy

### 3. React Frontend (`frontend`)
- **Build**: Multi-stage build with Node.js + Nginx
- **Port**: 80
- **Health Check**: HTTP request to root
- **Wait Strategy**: Waits for backend to be healthy

## Docker Commands

### Start services
```bash
# Start in foreground
docker-compose up

# Start in background
docker-compose up -d

# Build and start
docker-compose up --build
```

### Stop services
```bash
# Stop services
docker-compose stop

# Stop and remove containers
docker-compose down

# Stop, remove containers, and delete volumes
docker-compose down -v
```

### View logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f postgres
```

### Rebuild services
```bash
# Rebuild all
docker-compose build

# Rebuild specific service
docker-compose build backend

# Rebuild with no cache
docker-compose build --no-cache
```

### Check service status
```bash
docker-compose ps
```

### Execute commands in containers
```bash
# Backend
docker-compose exec backend sh

# Database
docker-compose exec postgres psql -U postgres -d employee_profile

# Frontend
docker-compose exec frontend sh
```

## Environment Variables

### JWT Configuration
- `JWT_SECRET`: Secret key for JWT token signing (min 256 bits)
- `JWT_EXPIRATION`: Token expiration time in milliseconds (default: 24 hours)

### HuggingFace API
- `APP_AI_HF_ENABLED`: Enable/disable AI features (default: true)
- `APP_AI_HF_URL`: HuggingFace API endpoint
- `APP_AI_HF_MODEL`: AI model to use
- `APP_AI_HF_API_KEY`: Your HuggingFace API key
- `APP_AI_HF_TIMEOUT`: Request timeout (default: PT10S)

### Security
- `APP_SECURITY_ALLOWED_ORIGINS`: CORS allowed origins
- `APP_SECURITY_ALLOWED_METHODS`: CORS allowed HTTP methods
- `APP_SECURITY_ALLOWED_HEADERS`: CORS allowed headers
- `APP_SECURITY_ALLOW_CREDENTIALS`: Enable CORS credentials
- `APP_SECURITY_SWITCH_USER_ENABLED`: Enable user switching feature

### Frontend
- `VITE_API_BASE_URL`: Backend API URL
- `VITE_ENABLE_SWITCH_USER`: Enable user switching in UI

## Volumes

### postgres_data
Persists PostgreSQL database data. To reset the database:
```bash
docker-compose down -v
docker-compose up -d
```

## Networking

All services communicate through the `employee_network` bridge network:
- Services can reference each other by service name (e.g., `postgres`, `backend`)
- Only specified ports are exposed to the host machine

## Health Checks

Each service includes health checks:
- **PostgreSQL**: `pg_isready` command
- **Backend**: HTTP GET to `/actuator/health`
- **Frontend**: HTTP GET to root `/`

Services wait for dependencies to be healthy before starting.

## Troubleshooting

### Backend can't connect to database
```bash
# Check database is running
docker-compose ps postgres

# Check database logs
docker-compose logs postgres

# Test database connection
docker-compose exec postgres psql -U postgres -c "SELECT 1"
```

### Frontend can't connect to backend
```bash
# Check backend is running
docker-compose ps backend

# Check backend health
curl http://localhost:8080/actuator/health

# Check backend logs
docker-compose logs backend
```

### Rebuild from scratch
```bash
# Stop and remove everything
docker-compose down -v

# Remove images
docker-compose rm -f
docker rmi employee-profile-backend employee-profile-frontend

# Rebuild and start
docker-compose up --build
```

### View application logs
```bash
# Backend logs (Spring Boot)
docker-compose logs -f backend

# Frontend logs (Nginx)
docker-compose logs -f frontend
```

## Production Considerations

For production deployments:

1. **Change default credentials**:
   - Update PostgreSQL password
   - Set strong JWT secret
   - Update all API keys

2. **Use environment-specific .env files**:
   ```bash
   docker-compose --env-file .env.production up -d
   ```

3. **Enable HTTPS**:
   - Add reverse proxy (Nginx/Traefik)
   - Configure SSL certificates

4. **Set resource limits**:
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '1'
         memory: 1G
   ```

5. **Use Docker secrets** for sensitive data

6. **Regular backups** of postgres_data volume

7. **Monitor services** with health checks

8. **Use production profiles**:
   ```bash
   SPRING_PROFILES_ACTIVE=prod docker-compose up -d
   ```
