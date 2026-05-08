# Troubleshooting Guide - "Nothing is Happening"

## 🔍 Quick Diagnosis

If you're sending requests to the validation endpoint and nothing is happening, follow these steps:

### Step 1: Check if Application is Running

```bash
# Test if application is responding
curl http://localhost:8080/api/health
```

**Expected Response:**
```json
{
  "status": "UP",
  "application": "DocValidator",
  "timestamp": 1234567890
}
```

**If you get "Connection refused":**
- Application is not running
- Start it with: `mvn spring-boot:run`

### Step 2: Check Configuration

```bash
# Check if API keys are configured (no auth required)
curl http://localhost:8080/api/debug/config
```

**Expected Response:**
```json
{
  "environmentVariables": {
    "OPENROUTER_API_KEY": "sk-o...",
    "SPOTIFY_CLIENT_ID": "abc1...",
    "SPOTIFY_CLIENT_SECRET": "xyz9..."
  },
  "configuration": {...},
  "readyToValidate": true,
  "missingKeys": []
}
```

**If `readyToValidate` is `false`:**
- Check `missingKeys` array
- Set the missing environment variables
- Restart the application

### Step 3: Test Debug Endpoint

```bash
# Simple test endpoint (no auth required)
curl http://localhost:8080/api/debug/test
```

**Expected Response:**
```json
{
  "status": "OK",
  "message": "Debug endpoint is working",
  "timestamp": 1234567890
}
```

## 🐛 Common Issues and Solutions

### Issue 1: Request Hangs / No Response

**Symptoms:**
- curl command hangs
- No response after several minutes
- Terminal shows no output

**Causes:**
1. Missing API keys (application waiting for external API)
2. Network timeout
3. OpenAI/OpenRouter API is slow or down

**Solutions:**

**A. Check Environment Variables**
```bash
# macOS/Linux
echo $OPENROUTER_API_KEY
echo $SPOTIFY_CLIENT_ID
echo $SPOTIFY_CLIENT_SECRET

# Windows PowerShell
echo $env:OPENROUTER_API_KEY
echo $env:SPOTIFY_CLIENT_ID
echo $env:SPOTIFY_CLIENT_SECRET
```

If any are empty, set them:
```bash
# macOS/Linux
export OPENROUTER_API_KEY="your-key-here"
export SPOTIFY_CLIENT_ID="your-id-here"
export SPOTIFY_CLIENT_SECRET="your-secret-here"

# Windows PowerShell
$env:OPENROUTER_API_KEY="your-key-here"
$env:SPOTIFY_CLIENT_ID="your-id-here"
$env:SPOTIFY_CLIENT_SECRET="your-secret-here"
```

**B. Restart Application**
```bash
# Stop the application (Ctrl+C)
# Then restart
mvn spring-boot:run
```

**C. Use Async Endpoint Instead**
Instead of `/run` (synchronous), use `/start` (asynchronous):
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}"]
  }'
```

Then check progress:
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/validation/progress
```

### Issue 2: 401 Unauthorized

**Symptoms:**
```
HTTP/1.1 401 Unauthorized
```

**Solution:**
Include authentication credentials:
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/validation/progress
```

### Issue 3: 404 Not Found

**Symptoms:**
```
HTTP/1.1 404 Not Found
```

**Common Mistakes:**
- Using `/api/validate` instead of `/api/v1/validation/start`
- Missing `/api/v1/` prefix

**Correct Endpoints:**
```bash
# ✅ Correct
POST /api/v1/validation/start
POST /api/v1/validation/run
GET  /api/v1/validation/progress

# ❌ Wrong
POST /api/validate
POST /validate
GET  /api/progress
```

### Issue 4: Empty Response / No Data

**Symptoms:**
- Request completes but returns `{}`
- No validation results

**Causes:**
1. Validation hasn't started yet
2. Validation is still running
3. No endpoints specified

**Solutions:**

**A. Check if validation is running:**
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/validation/progress
```

**B. Specify endpoints explicitly:**
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/run \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}", "/v1/artists/{id}"]
  }'
```

### Issue 5: Application Crashes / Errors in Console

**Check Console Logs:**
Look for error messages in the terminal where you ran `mvn spring-boot:run`

**Common Errors:**

**A. "Authentication failed"**
```
ERROR: Spotify authentication failed
```
**Solution:** Check Spotify credentials are correct

**B. "OpenAI API error"**
```
ERROR: Failed to call OpenAI API
```
**Solution:** Check OpenRouter API key is valid and has credits

**C. "Connection timeout"**
```
ERROR: Connection timeout to api.spotify.com
```
**Solution:** Check internet connection

## 📊 Step-by-Step Debugging

### 1. Verify Application is Running
```bash
curl http://localhost:8080/api/health
```

### 2. Check Configuration
```bash
curl http://localhost:8080/api/debug/config
```

### 3. Test Simple Endpoint
```bash
curl http://localhost:8080/api/debug/test
```

### 4. Test with Authentication
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/validation/progress
```

### 5. Start Async Validation
```bash
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}"]
  }'
```

### 6. Monitor Progress
```bash
# Check every few seconds
watch -n 2 'curl -s -u admin:admin123 http://localhost:8080/api/v1/validation/progress'
```

## 🔧 Advanced Debugging

### Enable Debug Logging

Edit `src/main/resources/application.yml`:
```yaml
logging:
  level:
    com.docvalidator: DEBUG
    org.springframework.web: DEBUG
```

Restart application and check logs.

### Check Application Logs

```bash
# View logs
tail -f logs/docvalidator.log

# Search for errors
grep ERROR logs/docvalidator.log
```

### Test with Minimal Request

```bash
# Simplest possible validation
curl -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}"]
  }'
```

## 📝 Checklist

Before running validation, ensure:

- [ ] Application is running (`mvn spring-boot:run`)
- [ ] Health check works (`curl http://localhost:8080/api/health`)
- [ ] Environment variables are set
  - [ ] `OPENROUTER_API_KEY`
  - [ ] `SPOTIFY_CLIENT_ID`
  - [ ] `SPOTIFY_CLIENT_SECRET`
- [ ] Configuration check passes (`curl http://localhost:8080/api/debug/config`)
- [ ] Using correct endpoint (`/api/v1/validation/start` or `/run`)
- [ ] Including authentication (`-u admin:admin123`)
- [ ] Request body is valid JSON

## 🆘 Still Not Working?

### 1. Restart Everything
```bash
# Stop application (Ctrl+C)
# Clear target directory
mvn clean
# Rebuild
mvn compile
# Restart
mvn spring-boot:run
```

### 2. Check Ports
```bash
# Check if port 8080 is in use
lsof -i :8080

# If another process is using it, kill it or change port in application.yml
```

### 3. Use Verbose curl
```bash
curl -v -u admin:admin123 \
  -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{
    "openApiUrl": "https://raw.githubusercontent.com/spotify/web-api/main/specifications/open-api.yml",
    "endpointPaths": ["/v1/albums/{id}"]
  }'
```

### 4. Test with Postman
- Import the endpoints
- Set Basic Auth (admin/admin123)
- Send request
- Check response and console

## 📚 Related Documentation

- [API_ENDPOINTS.md](API_ENDPOINTS.md) - All available endpoints
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Complete testing guide
- [LOGIN_GUIDE.md](LOGIN_GUIDE.md) - Authentication details