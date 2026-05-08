#!/bin/bash
echo "Checking environment variables..."
echo ""
echo "OPENROUTER_API_KEY: ${OPENROUTER_API_KEY:0:20}..." 
echo "SPOTIFY_CLIENT_ID: ${SPOTIFY_CLIENT_ID:0:20}..."
echo "SPOTIFY_CLIENT_SECRET: ${SPOTIFY_CLIENT_SECRET:0:20}..."
echo ""
echo "If you see empty values above, the .env file is not loaded."
echo "Run: source .env"
