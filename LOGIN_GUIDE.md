# DocValidator Login Guide

## 🔐 Login Credentials

When you access `http://localhost:8080`, you'll be redirected to the login page at `http://localhost:8080/login`.

### Available Users

#### Admin User (Full Access)
- **Username:** `admin`
- **Password:** `admin123`
- **Roles:** USER, ADMIN
- **Access:** Full access to all endpoints and features

#### Viewer User (Read-Only)
- **Username:** `viewer`
- **Password:** `viewer123`
- **Roles:** USER
- **Access:** Read-only access to view reports

## 🚀 How to Use

### 1. Start the Application
```bash
mvn spring-boot:run
```

### 2. Access the Application
Open your browser and go to:
```
http://localhost:8080
```

You'll be automatically redirected to the login page.

### 3. Login
Enter one of the credential sets above and click "Sign in".

### 4. Access API Endpoints

After logging in, you can access:

#### Health Check (No Auth Required)
```bash
curl http://localhost:8080/api/health
```

#### Start Validation (Auth Required)
```bash
curl -u admin:admin123 -X POST http://localhost:8080/api/validate
```

#### Get Validation Report (Auth Required)
```bash
curl -u admin:admin123 http://localhost:8080/api/validate/{reportId}
```

## 🔧 Security Configuration

The security is configured in `src/main/java/com/docvalidator/config/SecurityConfig.java`:

- **Form-based login** at `/login`
- **In-memory user store** (for development)
- **BCrypt password encoding**
- **CSRF protection** disabled for API endpoints
- **H2 Console** accessible without authentication

## 📝 Notes

### For Development
- The current setup uses in-memory authentication (users are lost on restart)
- Passwords are BCrypt encoded
- CSRF is disabled for API endpoints to allow easy testing

### For Production
You should:
1. Use a database-backed user store
2. Enable CSRF protection
3. Use HTTPS
4. Implement proper session management
5. Add rate limiting
6. Use environment variables for credentials

## 🔄 Logout

To logout, access:
```
http://localhost:8080/logout
```

## 🛠️ Customization

To add more users or change passwords, edit `SecurityConfig.java`:

```java
@Bean
public UserDetailsService userDetailsService() {
    UserDetails newUser = User.builder()
            .username("newuser")
            .password(passwordEncoder().encode("newpassword"))
            .roles("USER")
            .build();
    
    return new InMemoryUserDetailsManager(user, viewer, newUser);
}
```

## 🐛 Troubleshooting

### Issue: Can't login
- **Solution:** Make sure you're using the correct username and password
- Check console logs for any errors

### Issue: Redirected to login after successful login
- **Solution:** Clear browser cookies and try again
- Check if the application is running on port 8080

### Issue: API calls return 401 Unauthorized
- **Solution:** Include credentials in your API calls:
  ```bash
  curl -u admin:admin123 http://localhost:8080/api/validate
  ```

## 📚 Related Documentation

- [README.md](README.md) - Main project documentation
- [GETTING_STARTED.md](GETTING_STARTED.md) - Getting started guide
- [docs/SPOTIFY_SETUP.md](docs/SPOTIFY_SETUP.md) - Spotify API setup