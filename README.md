# File Sharing SaaS Application

A Spring Boot application for file sharing with Supabase Storage integration.

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security**
- **Spring Data JPA**
- **Thymeleaf**
- **PostgreSQL**
- **Supabase Storage**

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL database
- Supabase account

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database named `filesharing`:

```sql
CREATE DATABASE filesharing;
```

### 2. Configuration

Update the database connection in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/filesharing
    username: your_username
    password: your_password
```

### 3. Supabase Setup

The application is already configured with your Supabase credentials:
- Project URL: https://oabolgvcloamohipyexb.supabase.co
- API Key and Service Role Key are configured in `application.yml`

### 4. Build and Run

```bash
# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```

### 5. Access the Application

- **Home Page**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health

## Project Structure

```
src/main/java/com/filesharing/
├── FileSharingApplication.java    # Main application class
├── config/
│   ├── SecurityConfig.java        # Security configuration
│   └── SupabaseConfig.java        # Supabase configuration
└── controller/
    └── HomeController.java        # Home page controller

src/main/resources/
├── application.yml                # Application configuration
└── templates/
    └── home.html                  # Home page template
```

## Features Implemented

- ✅ Spring Boot 3.2.0 with Java 17
- ✅ Thymeleaf integration
- ✅ PostgreSQL configuration
- ✅ Supabase Storage configuration
- ✅ Basic security setup
- ✅ Home page with Bootstrap UI
- ✅ Health check endpoints

## Next Steps

This is Phase 1 - Foundation setup. The following phases will implement:
- Phase 2: File upload functionality
- Phase 3: File download functionality  
- Phase 4: Security and polish

## Troubleshooting

1. **Database Connection Error**: Ensure PostgreSQL is running and credentials are correct
2. **Port Already in Use**: Change the port in `application.yml` or stop other applications using port 8080
3. **Maven Build Issues**: Ensure Java 17 is installed and JAVA_HOME is set correctly 