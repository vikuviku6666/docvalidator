# AI Configuration Guide - OpenRouter Setup

## Current Issue

You're seeing `[]` in recommendations because the AI client thinks the API key is not configured.

## Quick Fix

### Option 1: Environment Variable (Recommended)

Set the environment variable before starting the backend:

```bash
export OPENROUTER_API_KEY="sk-or-v1-your-actual-key-here"
cd backend
mvn spring-boot:run
```

### Option 2: Direct Configuration

Edit `backend/src/main/resources/application.yml`:

```yaml
docvalidator:
  ai:
    provider: openrouter
    openrouter:
      api-key: sk-or-v1-your-actual-key-here  # Replace with your actual key
      model: openai/gpt-4
      temperature: 0.2
      max-tokens: 2000
```

## Verify Configuration

### 1. Check Backend Logs

When the backend starts, you should see:

```
AI chat client configured for provider=openrouter model=openai/gpt-4
```

If you see:
```
AI chat client disabled (provider=openrouter, key configured=false)
```

Then the API key is not being read correctly.

### 2. Test API Key

You can test your OpenRouter API key directly:

```bash
curl https://openrouter.ai/api/v1/chat/completions \
  -H "Authorization: Bearer YOUR_OPENROUTER_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "openai/gpt-4",
    "messages": [{"role": "user", "content": "Say hello"}]
  }'
```

### 3. Check Environment Variables

```bash
echo $OPENROUTER_API_KEY
```

Should output your actual API key, not `your_openrouter_key`.

## Common Issues

### Issue 1: Placeholder Value

**Problem:** API key is still set to `your_openrouter_key`

**Solution:** Replace with actual key from https://openrouter.ai/keys

### Issue 2: Wrong Provider

**Problem:** Provider is set to `openai` but using OpenRouter key

**Solution:** Set `provider: openrouter` in application.yml or:
```bash
export AI_PROVIDER=openrouter
```

### Issue 3: Environment Variable Not Set

**Problem:** Backend starts before environment variable is set

**Solution:** 
```bash
# Stop backend (Ctrl+C)
export OPENROUTER_API_KEY="your-actual-key"
cd backend
mvn spring-boot:run
```

## Recommended Models for OpenRouter

```yaml
# Fast and cheap
model: openai/gpt-3.5-turbo

# Best quality
model: openai/gpt-4

# Alternative providers
model: anthropic/claude-3-opus
model: google/gemini-pro
model: meta-llama/llama-3-70b-instruct
```

## Expected Behavior After Fix

### In Backend Logs:
```
AI chat client configured for provider=openrouter model=openai/gpt-4
Generating validation report for 27 results
```

### In Frontend Reports:
Instead of `[]`, you'll see:

```
Overall Documentation Quality Improvement

Based on the validation results, I recommend focusing on the following areas:

1. Status Code Consistency: Address the 10 status code mismatches as they 
   directly impact API consumer expectations...

2. Schema Documentation: Update schemas to include the 8 undocumented fields...

3. Authentication Clarity: Document the additional OAuth scopes required...
```

## Quick Test

After configuring, run a validation and check the report:

```bash
# Start backend with proper env var
export OPENROUTER_API_KEY="your-key"
cd backend
mvn spring-boot:run

# In another terminal, trigger validation
curl -X POST http://localhost:8080/api/v1/validation/start \
  -H "Content-Type: application/json" \
  -d '{"openApiUrl": "https://developer.spotify.com/reference/web-api/open-api-schema.yaml"}'

# Wait for completion, then check report
curl http://localhost:8080/api/v1/validation/report/latest | jq '.recommendations[0].description'
```

You should see actual AI-generated text, not `[]`.

## Troubleshooting

### Enable Debug Logging

Add to `application.yml`:

```yaml
logging:
  level:
    com.docvalidator.agent.AiChatClient: DEBUG
```

This will show:
- API key validation
- HTTP requests to OpenRouter
- Response parsing

### Check API Key Format

OpenRouter keys start with: `sk-or-v1-`

If your key doesn't match this format, it might be invalid.

### Rate Limits

OpenRouter has rate limits. If you see errors, check:
- https://openrouter.ai/activity (your usage)
- Add credits if needed

## Cost Estimation

For a typical validation run with 150 endpoints:

- **GPT-3.5-turbo:** ~$0.10 per run
- **GPT-4:** ~$1.00 per run
- **Claude-3-opus:** ~$2.00 per run

## Need Help?

1. Check backend logs for errors
2. Verify API key at https://openrouter.ai/keys
3. Test API key with curl command above
4. Check OpenRouter dashboard for usage/errors

---

**Once configured correctly, you'll get intelligent, context-aware recommendations instead of empty arrays!**