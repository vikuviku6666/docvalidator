# How to Start DocValidator Servers

## Quick Start (Recommended)

### Terminal 1: Start Backend
```bash
cd backend
mvn spring-boot:run
```

### Terminal 2: Start Frontend
```bash
cd frontend
npm run dev
```

## With Environment Variables

### Terminal 1: Backend with .env
```bash
# Load environment variables
export OPENROUTER_API_KEY="your-openrouter-key"
export SPOTIFY_CLIENT_ID="your-spotify-client-id"
export SPOTIFY_CLIENT_SECRET="your-spotify-client-secret"

# Start backend
cd backend
mvn spring-boot:run
```

### Terminal 2: Frontend
```bash
cd frontend
npm run dev
```

## Using Helper Script

### Terminal 1: Backend
```bash
./start-backend.sh
```

### Terminal 2: Frontend
```bash
cd frontend
npm run dev
```

## Verify Servers Are Running

### Check Backend
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP"}
```

### Check Frontend
Open browser: http://localhost:3000

## Troubleshooting

### Backend Won't Start - Port 8080 in Use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Then restart backend
cd backend
mvn spring-boot:run
```

### Frontend Won't Start - Port 3000 in Use
```bash
# Kill process on port 3000
lsof -ti:3000 | xargs kill -9

# Then restart frontend
cd frontend
npm run dev
```

### Backend Starts But Frontend Can't Connect
1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. Check backend logs for errors
3. Restart both servers

### Environment Variables Not Loaded
Make sure to export variables in the same terminal session before starting the backend:
```bash
export OPENROUTER_API_KEY="your-key"
cd backend
mvn spring-boot:run
```

## Expected Output

### Backend Started Successfully
```
Started DocValidatorApplication in X.XXX seconds
```

### Frontend Started Successfully
```
✓ Ready in Xs
○ Local:   http://localhost:3000
```

## Access Points

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

---

## For Hackathon Demo

1. Open 2 terminal windows
2. Terminal 1: `cd backend && mvn spring-boot:run`
3. Terminal 2: `cd frontend && npm run dev`
4. Wait for both to start (backend ~10s, frontend ~5s)
5. Open http://localhost:3000 in browser
6. Ready to demo! 🎉