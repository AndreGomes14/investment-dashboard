# Security Setup - Role-Based Authorization

## Overview
The application now includes a comprehensive role-based authorization system that replaces the previous basic authentication setup. This system allows fine-grained control over API access based on user roles.

## Key Features
- **Role-Based Authorization**: Users are assigned roles that determine their access levels
- **URL-to-Role Mapping**: Specific URL patterns are mapped to required roles
- **Configurable Security**: Can be enabled/disabled for development vs production
- **Automatic User/Role Initialization**: Default users and roles are created on startup

## Default Roles
- **ADMIN**: Full access to all endpoints including user/role management
- **MANAGER**: Elevated privileges, can manage users but not roles
- **USER**: Standard user access to portfolio and investment features

## Default Users
The system creates these default users on startup:
- **admin** / admin123 (ADMIN role)
- **manager** / manager123 (USER + MANAGER roles)  
- **testuser** / test123 (USER role)

## Configuration

### Production Mode (app.security.enable=true)
In production, role-based authorization is enabled with URL-to-role mappings:

```properties
app.security.enable=true

# URL to Role Mappings
app.security.url-to-role-list[0].url=/api/admin/**
app.security.url-to-role-list[0].roles[0]=ADMIN

app.security.url-to-role-list[1].url=/api/portfolios/**
app.security.url-to-role-list[1].roles[0]=USER
app.security.url-to-role-list[1].roles[1]=ADMIN
app.security.url-to-role-list[1].roles[2]=MANAGER
```

### Development Mode (app.security.enable=false)
For development, you can disable role-based authorization:

```bash
# Run with dev profile
java -jar app.jar --spring.profiles.active=dev
```

Or set in application-dev.properties:
```properties
app.security.enable=false
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - Login with username/password
- `POST /api/auth/register` - Register new user (if enabled)

### Role Management (Admin Only)
- `GET /api/admin/roles` - List all roles
- `POST /api/admin/roles` - Create new role
- `GET /api/admin/users` - List all users
- `POST /api/admin/users/{userId}/roles/{roleId}` - Assign role to user
- `DELETE /api/admin/users/{userId}/roles/{roleId}` - Remove role from user
- `GET /api/admin/users/{userId}/roles` - Get user's roles

### Protected Endpoints
- `/api/portfolios/**` - Requires USER, MANAGER, or ADMIN role
- `/api/investments/**` - Requires USER, MANAGER, or ADMIN role
- `/api/search/**` - Requires USER, MANAGER, or ADMIN role
- `/api/admin/**` - Requires ADMIN role
- `/api/users/**` (POST/PUT) - Requires ADMIN or MANAGER role
- `/api/users/**` (DELETE) - Requires ADMIN role

## Usage Examples

### 1. Login as Admin
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

### 2. Access Protected Endpoint
```bash
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Assign Role to User
```bash
curl -X POST http://localhost:8080/api/admin/users/{userId}/roles/{roleId} \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## Troubleshooting

### Access Denied Issues
1. Check if user has correct roles assigned
2. Verify JWT token is valid and includes roles
3. Check URL-to-role mappings in configuration
4. Review security logs for detailed error information

### Development Mode
If you need to bypass security for development:
1. Set `app.security.enable=false` in application-dev.properties
2. Use `--spring.profiles.active=dev` when running
3. All authenticated requests will be allowed regardless of roles

### Database Issues
The system automatically creates roles and users tables. If you see errors:
1. Check database connection
2. Ensure proper database permissions
3. Check Hibernate DDL settings
4. Review application logs for initialization errors

## Security Best Practices
1. Change default passwords in production
2. Use strong JWT secrets
3. Regularly audit user roles and permissions
4. Monitor authentication logs
5. Use HTTPS in production
6. Implement proper session timeout 