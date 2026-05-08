# Project Structure Clarification

## Two Separate Projects

This workspace contains planning documentation for **DocValidator**, which is completely separate from the greeting-api project.

---

## Project 1: Greeting API (tut02)
**Location**: `/Users/viku/Dev_Projects/Java_Projects/tut_java/tut02/greeting-api`

**Description**: A Quarkus-based RESTful API that manages greetings in multiple languages

**Technology**: 
- Quarkus 3.35.1
- PostgreSQL
- Hibernate ORM Panache

**Purpose**: This was initially analyzed as a potential target for validation, but is **NOT part of DocValidator**

---

## Project 2: DocValidator (tut03 - Current Workspace)
**Location**: `/Users/viku/Dev_Projects/Java_Projects/tut_java/tut03`

**Description**: AI-Powered API Documentation Testing Framework

**Technology**:
- Spring Boot 3.x
- JUnit 5, RestAssured
- MCP (Model Context Protocol)
- AI Integration (OpenAI/Claude)

**Purpose**: Validates API documentation against live systems

**Target API**: **Spotify Web API** (150+ endpoints)

---

## Important Notes

1. **DocValidator** (tut03) is the testing framework
2. **Greeting API** (tut02) is a separate, unrelated project
3. All DocValidator documentation focuses **exclusively on Spotify API**
4. No greeting-api code or references exist in DocValidator project

---

## DocValidator Documentation Files (tut03)

All files in the current workspace (tut03) are for DocValidator:

- `README.md` - Main project overview (Spotify-focused)
- `PROJECT_SUMMARY.md` - Executive summary (Spotify-focused)
- `ARCHITECTURE.md` - System architecture (Spotify-focused)
- `SPOTIFY_ARCHITECTURE.md` - Detailed Spotify demo
- `SPOTIFY_EXAMPLE.md` - Complete Spotify validation example
- `TEST_GENERATION_STRATEGY.md` - Test generation (Spotify examples)
- `HOW_IT_WORKS.md` - Simple explanation
- `UI_DESIGN.md` - Web UI design

**Zero greeting-api references in any DocValidator files.**

---

## If You See Greeting API Content

If you're seeing greeting-api content, you're looking at files from the **tut02 directory**, not the DocValidator project (tut03).

**Current Workspace**: `/Users/viku/Dev_Projects/Java_Projects/tut_java/tut03`

**Greeting API Location**: `/Users/viku/Dev_Projects/Java_Projects/tut_java/tut02/greeting-api`

These are **two completely separate projects** in different directories.