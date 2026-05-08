# Environment Variables Setup Guide

## Problem

You have a `.env` file with your API keys, but Spring Boot doesn't automatically load `.env` files. The backend needs these environment variables to be available when it starts.

## Solution

### Option 1: Use the Helper Script (Easiest)

I've created a script that loads your `.env` file and starts the backend:

```bash
./start-backend.sh
```

This script:
1. Loads all variables from `.env`
2. Exports them to the environment
3. Starts the backend with `mvn spring-boot:run`

### Option 2: Manual Export (Alternative)

Load the `.env` file manually before starting:

```bash
# Load environment variables
set -a
source .env
set +a

# Start backend
cd backend
mvn spring-boot:run
```

### Option 3: Direct Export (Quick Test)

Export just the needed variables (replace with your actual keys):

```bash
export OPENROUTER_API_KEY="your-openrouter-api-key-here"
export SPOTIFY_CLIENT_ID="your-spotify-client-id-here"
export SPOTIFY_CLIENT_SECRET="your-spotify-client-secret-here"

cd backend
mvn spring-boot:run
```

## Verify It's Working

### 1. Check Backend Logs

When the backend starts, look for this line:

```
✅ AI chat client configured for provider=openrouter model=openai/gpt-4
```

If you see:
```
❌ AI chat client disabled (provider=openrouter, key configured=false)
```

Then the environment variable wasn't loaded.

### 2. Check Environment Variable

In the same terminal where you'll run the backend:

```bash
echo $OPENROUTER_API_KEY
```

Should output your actual API key, not empty.

### 3. Test a Validation Run

After starting the backend with proper env vars:

```bash
# Trigger validation
curl -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{"openApiUrl": "https://developer.spotify.com/reference/web-api/open-api-schema.yaml"}'

# Wait a few minutes, then check report
curl http://localhost:8080/api/v1/validation/report/latest | jq '.recommendations[0].description'
```

You should see AI-generated text, not `[]`.

## Your .env File Format

Make sure your `.env` file looks like this:

```bash
# .env
OPENROUTER_API_KEY=sk-or-v1-your-actual-key-here
SPOTIFY_CLIENT_ID=your-spotify-client-id
SPOTIFY_CLIENT_SECRET=your-spotify-client-secret
```

**Important:**
- No quotes around values
- No spaces around `=`
- One variable per line
- No `export` keyword needed

## Common Issues

### Issue 1: Script Permission Denied

```bash
chmod +x start-backend.sh
./start-backend.sh
```

### Issue 2: .env File Not Found

Make sure `.env` is in the project root (same directory as `start-backend.sh`):

```
tut03/
├── .env                    ← Here
├── start-backend.sh
├── backend/
└── frontend/
```

### Issue 3: Variables Not Exported

The `set -a` command in the script automatically exports all variables. If it's not working:

```bash
# Debug: Check if variables are loaded
set -a
source .env
set +a
env | grep OPENROUTER
```

Should show your API key.

### Issue 4: Wrong Shell

If using `zsh` instead of `bash`:

```bash
# For zsh
source .env
cd backend
mvn spring-boot:run
```

## IDE Setup (IntelliJ/VS Code)

### IntelliJ IDEA

1. Open Run Configuration
2. Add Environment Variables:
   ```
   OPENROUTER_API_KEY=your-key
   SPOTIFY_CLIENT_ID=your-id
   SPOTIFY_CLIENT_SECRET=your-secret
   ```
3. Run the application

### VS Code

1. Create `.vscode/launch.json`:
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "DocValidator",
      "request": "launch",
      "mainClass": "com.docvalidator.DocValidatorApplication",
      "projectName": "docvalidator",
      "env": {
        "OPENROUTER_API_KEY": "your-key",
        "SPOTIFY_CLIENT_ID": "your-id",
        "SPOTIFY_CLIENT_SECRET": "your-secret"
      }
    }
  ]
}
```

## Complete Startup Sequence

### Terminal 1: Backend with .env

```bash
# Option A: Use helper script
./start-backend.sh

# Option B: Manual
set -a && source .env && set +a
cd backend
mvn spring-boot:run
```

### Terminal 2: Frontend

```bash
cd frontend
npm run dev
```

### Terminal 3: Trigger Validation

```bash
curl -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{"openApiUrl": "https://developer.spotify.com/reference/web-api/open-api-schema.yaml"}'
```

## Expected Results

### Backend Logs (Success)
```
AI chat client configured for provider=openrouter model=openai/gpt-4
Starting validation workflow for: https://developer.spotify.com/...
Step 1: Parsing OpenAPI specification...
Parsed 150 endpoints
Step 2: Generating test cases...
Generated 450 total test cases
Step 3: Executing tests...
Executed 450 tests: 423 passed, 27 failed
Step 4: Generating validation report...
Validation workflow completed successfully
Health Score: 91.5%
```

### Frontend Reports (Success)
```
Overall Documentation Quality Improvement

Based on the validation results, I recommend focusing on the following areas:

1. Status Code Consistency: Address the 10 status code mismatches as they 
   directly impact API consumer expectations. Update the OpenAPI specification 
   to document that endpoints return 400 Bad Request for malformed parameters...

2. Schema Documentation: Update schemas to include the 8 undocumented fields 
   that appear in actual responses. This includes the 'popularity' field in 
   track objects...

3. Authentication Clarity: Document the additional OAuth scopes required for 
   collaborative playlist operations...
```

## Troubleshooting

### Still Seeing `[]`?

1. **Stop the backend** (Ctrl+C)
2. **Verify .env file exists:**
   ```bash
   cat .env | grep OPENROUTER
   ```
3. **Load and verify:**
   ```bash
   set -a && source .env && set +a
   echo $OPENROUTER_API_KEY
   ```
4. **Start backend in same terminal:**
   ```bash
   cd backend
   mvn spring-boot:run
   ```
5. **Check logs for:** `AI chat client configured`

### Need More Help?

Check the backend logs for specific error messages:
```bash
tail -f backend/logs/docvalidator.log
```

Or enable debug logging in `application.yml`:
```yaml
logging:
  level:
    com.docvalidator.agent.AiChatClient: DEBUG
```

---

**Once properly configured, you'll get intelligent AI-generated recommendations instead of empty arrays!**