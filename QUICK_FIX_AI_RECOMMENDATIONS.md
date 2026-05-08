# Quick Fix: AI Recommendations Not Showing

## Problem
You're seeing the fallback text "Configure an OpenAI or OpenRouter API key..." instead of actual AI-generated recommendations.

## Root Cause
The backend is not detecting your OpenRouter API key, even though it's in your `.env` file.

## Solution: 3 Steps

### Step 1: Verify Your .env File

Check that your `.env` file exists and has the correct format:

```bash
cat .env
```

Should show:
```
OPENROUTER_API_KEY=sk-or-v1-your-actual-key-here
SPOTIFY_CLIENT_ID=your-spotify-id
SPOTIFY_CLIENT_SECRET=your-spotify-secret
```

**Important:** No quotes, no spaces around `=`

### Step 2: Load Environment Variables

**Before starting the backend**, load the variables:

```bash
# Stop backend if running (Ctrl+C)

# Load .env file
set -a
source .env
set +a

# Verify they're loaded
./check-env.sh
```

You should see your API key (first 20 characters).

### Step 3: Start Backend in Same Terminal

**Critical:** Start the backend in the SAME terminal where you loaded the variables:

```bash
cd backend
mvn spring-boot:run
```

Watch for this line in the logs:
```
✅ AI chat client configured for provider=openrouter model=openai/gpt-4
```

If you see:
```
❌ AI chat client disabled (provider=openrouter, key configured=false)
```

Then the environment variable wasn't loaded properly.

## Alternative: Use the Helper Script

The `start-backend.sh` script should do all this automatically:

```bash
./start-backend.sh
```

## Verify It's Working

### 1. Check Backend Logs

Look for:
```
INFO  c.d.agent.AiChatClient - AI chat client configured for provider=openrouter model=openai/gpt-4
```

### 2. Run a New Validation

After fixing the environment variables, you need to run a NEW validation:

```bash
curl -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{"openApiUrl": "https://developer.spotify.com/reference/web-api/open-api-schema.yaml"}'
```

### 3. Check the New Report

Wait for validation to complete, then check:

```bash
curl http://localhost:8080/api/v1/validation/report/latest | jq '.recommendations[0].description'
```

Should show AI-generated text, not the fallback message.

## Common Mistakes

### ❌ Mistake 1: Starting Backend Before Loading .env

```bash
# WRONG - backend won't see the variables
cd backend
mvn spring-boot:run
# Then trying to load .env in another terminal
```

### ✅ Correct Way

```bash
# RIGHT - load first, then start in same terminal
set -a && source .env && set +a
cd backend
mvn spring-boot:run
```

### ❌ Mistake 2: Using Old Report

The old report was generated when AI wasn't configured. You need to:
1. Fix environment variables
2. Restart backend
3. Run a NEW validation
4. Check the NEW report

### ❌ Mistake 3: Wrong .env Format

```bash
# WRONG
export OPENROUTER_API_KEY="sk-or-v1-..."
OPENROUTER_API_KEY = sk-or-v1-...
OPENROUTER_API_KEY='sk-or-v1-...'

# RIGHT
OPENROUTER_API_KEY=sk-or-v1-...
```

## Debug Steps

### 1. Check if .env file exists
```bash
ls -la .env
```

### 2. Check environment variable in current terminal
```bash
echo $OPENROUTER_API_KEY
```

Should show your key, not empty.

### 3. Check backend configuration
```bash
cd backend
grep -A 5 "openrouter:" src/main/resources/application.yml
```

Should show:
```yaml
openrouter:
  api-key: ${OPENROUTER_API_KEY:your_openrouter_key}
```

### 4. Test OpenRouter API directly
```bash
curl https://openrouter.ai/api/v1/chat/completions \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "openai/gpt-4",
    "messages": [{"role": "user", "content": "Say hello"}]
  }'
```

Should return a response, not an error.

## Complete Fix Procedure

```bash
# 1. Stop backend (Ctrl+C if running)

# 2. Verify .env file
cat .env | grep OPENROUTER

# 3. Load environment variables
set -a
source .env
set +a

# 4. Verify they're loaded
echo $OPENROUTER_API_KEY

# 5. Start backend in SAME terminal
cd backend
mvn spring-boot:run

# 6. Wait for "AI chat client configured" in logs

# 7. In another terminal, run NEW validation
curl -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{"openApiUrl": "https://developer.spotify.com/reference/web-api/open-api-schema.yaml"}'

# 8. Wait 5-10 minutes for completion

# 9. Check NEW report
curl http://localhost:8080/api/v1/validation/report/latest | jq '.recommendations'
```

## Expected Result

After following these steps, you should see:

```json
{
  "recommendations": [
    {
      "title": "Overall Documentation Quality Improvement",
      "description": "Based on the validation results, I recommend focusing on the following areas:\n\n1. Status Code Consistency: Address the status code mismatches...\n\n2. Schema Documentation: Update schemas to include undocumented fields...\n\n3. Authentication Clarity: Document additional OAuth scopes...",
      "severity": "HIGH",
      "priority": 1
    }
  ]
}
```

**NOT:**
```json
{
  "description": "Configure an OpenAI or OpenRouter API key..."
}
```

## Still Not Working?

If you still see the fallback text after following all steps:

1. **Check backend logs** for errors
2. **Verify API key** at https://openrouter.ai/keys
3. **Test API key** with curl command above
4. **Check credits** at https://openrouter.ai/activity
5. **Enable debug logging** in application.yml:
   ```yaml
   logging:
     level:
       com.docvalidator.agent.AiChatClient: DEBUG
   ```

---

**The key is: Load .env BEFORE starting backend, in the SAME terminal!**