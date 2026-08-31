# Chesst

A real, full-stack chess analysis platform. Real Stockfish engine (UCI, server-side),
real PostgreSQL database, JWT auth with email verification, Lichess & Chess.com import,
and a complete ECO opening database (220 entries, A00–E99, FEN-validated).

This repository contains two independent applications:

```
.
├── backend/           Java 17 + Spring Boot 3 + PostgreSQL + JWT + Stockfish
│   ├── Dockerfile
│   └── render.yaml    Render Blueprint (self-contained)
├── frontend/          Vanilla HTML/CSS/JavaScript (no React, no Next.js, no Vue)
│   ├── Dockerfile
│   └── netlify.toml   Netlify config (self-contained)
├── docker-compose.yml
├── README.md
└── .gitignore
```

Each folder is self-contained and can be deployed as if it were its own repository.

---

## Backend

**Stack:** Java 17, Spring Boot 3.2, Spring Security, JWT (jjwt), JPA/Hibernate, PostgreSQL, Flyway, Bean Validation, WebFlux (Lichess/Chess.com clients), Spring Mail.

**Architecture:** `controller → service → repository → entity → DTO` with a global exception handler and a JWT auth filter.

### Run locally

Prerequisites: JDK 17, Maven, PostgreSQL (or use Docker Compose — see below), and **Stockfish** installed (`apt install stockfish` on Debian/Ubuntu, `brew install stockfish` on macOS).

```bash
cd backend

# Create the database
createdb chesst

# Set env (adjust as needed)
export DATABASE_URL="jdbc:postgresql://localhost:5432/chesst"
export DATABASE_USERNAME="chesst"
export DATABASE_PASSWORD="chesst"
export JWT_SECRET="your-long-random-secret-at-least-256-bits"
export STOCKFISH_PATH="/usr/games/stockfish"
export CORS_ORIGINS="http://localhost:5500,https://chesst.netlify.app,https://chesst.js.org"

# Run
mvn spring-boot:run
```

The backend starts on `http://localhost:8080`. Flyway runs `V1__init.sql` (schema) and `V2__seed_openings.sql` (220 ECO openings) automatically on first boot.

### API endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | no | Create account (username, email, password, confirmPassword). Sends a 6-digit verification code. |
| POST | `/api/auth/login` | no | Login; returns JWT. Rejected until email verified. |
| POST | `/api/auth/logout` | yes | Stateless logout (client discards token). |
| POST | `/api/auth/verify-email` | no | Verify email with 6-digit code. |
| POST | `/api/auth/resend-verification` | no | Resend verification code. |
| POST | `/api/auth/forgot-password` | no | Request a password reset token (emailed). |
| POST | `/api/auth/reset-password` | no | Reset password with token + new password. |
| GET | `/api/profile` | yes | Current user's profile + game count. |
| PATCH | `/api/profile` | yes | Update display name / bio / avatar URL. |
| GET | `/api/games` | yes | List current user's saved games. |
| POST | `/api/games` | yes | Save a game (PGN + metadata). |
| GET | `/api/games/{id}` | yes | Get one game (owner only). |
| DELETE | `/api/games/{id}` | yes | Delete a game (owner only). |
| POST | `/api/analyses/position` | yes | Analyze a single FEN with Stockfish. Returns best move, eval, depth, PV. |
| POST | `/api/analyses/games/{gameId}` | yes | Run full-game analysis (per-ply). Persists report. |
| GET | `/api/analyses/games/{gameId}` | yes | Analysis history for a game. |
| GET | `/api/openings?q=&eco=&page=&size=` | no | Search openings; filter by ECO volume. |
| GET | `/api/openings/counts` | no | Per-volume counts (A/B/C/D/E + ALL). |
| GET | `/api/openings/{id}` | no | One opening. |
| GET | `/api/integrations/lichess/{username}/games` | yes | Fetch recent Lichess games (PGN parsed). |
| POST | `/api/integrations/lichess/{username}/import` | yes | Import Lichess games into archive. |
| GET | `/api/integrations/lichess/{username}/profile` | yes | Lichess profile. |
| GET | `/api/integrations/chesscom/{username}/games` | yes | Fetch recent Chess.com games. |
| POST | `/api/integrations/chesscom/{username}/import` | yes | Import Chess.com games. |
| GET | `/api/integrations/chesscom/{username}/profile` | yes | Chess.com profile. |
| GET | `/actuator/health` | no | Health check. |

### Security

- **JWT** access tokens (HMAC-SHA256), 60-minute TTL, signed with `JWT_SECRET`.
- **BCrypt** password hashing (strength 10).
- **CORS** configured to the allowed origins in `CORS_ORIGINS`.
- **SQL injection** protection via JPA parameterized queries (no string concatenation in repositories).
- **XSS** protection: inputs validated with Bean Validation; outputs HTML-escaped on the frontend.
- **Global exception handler** normalizes all errors into a consistent JSON shape.
- **Email verification required** before login.

### Tests

```bash
cd backend
mvn test
```

Includes JUnit 5 + MockMvc tests for the auth flow (registration, password mismatch, duplicate rejection, login-before-verification) plus a unit test for PGN parsing. Tests run against an in-memory H2 database (PostgreSQL mode) via the `test` profile.

### Docker

```bash
# from the repository root
docker compose up --build
```

Starts PostgreSQL 16 and the backend (with Stockfish pre-installed) on `http://localhost:8080`.

---

## Frontend

**Stack:** Plain HTML5, CSS3, vanilla JavaScript (ES modules). Chess UI via [chess.js](https://github.com/jhlywa/chess.js) + [chessboard.js](https://chessboardjs.com). No build step.

### Pages

- `index.html` — Home / landing
- `login.html`, `register.html` — Auth (with email verification flow)
- `analysis.html` — Interactive board + engine panel + move list + PGN/FEN load + save
- `openings.html` — ECO explorer with search, volume filter, win rates
- `archive.html` — Saved games with filters and delete
- `profile.html` — User profile + stats + recent games
- `settings.html` — Account info, Lichess/Chess.com import, API endpoint, theme

### Run locally

Any static file server works. For example:

```bash
cd frontend
python3 -m http.server 5500
# open http://localhost:5500/index.html
```

Then open **Settings** and set the API endpoint to `http://localhost:8080` (or leave the default `https://chesst.onrender.com` for production).

### Configuration

The frontend reads the backend URL from `localStorage['chesst:apiBase']`, defaulting to `https://chesst.onrender.com`. Change it on the Settings page — no rebuild needed.

---

## Deployment

### Backend → Render

The `render.yaml` Blueprint lives inside `backend/`. Two deployment options:

**Option A — monorepo (single repo, recommended to start):**
1. Push this whole repo to GitHub.
2. On [Render](https://render.com), create a new Blueprint. In the service settings, set **Root Directory** to `backend` so Render finds `backend/render.yaml`.
3. Create a PostgreSQL database (`chesst-db`) and link it.
4. Set environment variables: `JWT_SECRET`, `CORS_ORIGINS=https://chesst.netlify.app,https://chesst.js.org`, `STOCKFISH_PATH=/usr/games/stockfish`, `MAIL_USERNAME`, `MAIL_PASSWORD`.
5. The Docker image installs `stockfish` via apt, so real server-side analysis works on Render.
6. Backend URL: `https://chesst.onrender.com`.

**Option B — split repos (full separation):**
1. Push only the `backend/` folder to its own GitHub repo.
2. Render reads `render.yaml` at the repo root automatically — no Root Directory setting needed.

### Frontend → Netlify

The `netlify.toml` config lives inside `frontend/`. Two deployment options:

**Option A — monorepo (single repo, recommended to start):**
1. On [Netlify](https://netlify.com), create a new site from this repo.
2. In site settings, set **Base directory** to `frontend` so Netlify finds `frontend/netlify.toml`.
3. **Build command:** (none — static)
4. **Publish directory:** `.` (relative to base — `netlify.toml` already sets `publish = "."`)
5. Frontend URL (stage 1): `https://chesst.netlify.app`.

**Option B — split repos (full separation):**
1. Push only the `frontend/` folder to its own GitHub repo.
2. Netlify reads `netlify.toml` at the repo root automatically — no Base directory setting needed.

After deployment, open **Settings** on the deployed site and confirm the API endpoint is `https://chesst.onrender.com`.

### Custom domain → JS.ORG (stage 2)

1. After the Netlify site is live at `chesst.netlify.app`, request the `chesst.js.org` subdomain from [js.org](https://js.org).
2. Add the JS.ORG CNAME to your Netlify domain settings.
3. Update `CORS_ORIGINS` on Render to include `https://chesst.js.org`.

### Deployment order

1. Frontend → Netlify (`https://chesst.netlify.app`)
2. Backend → Render (`https://chesst.onrender.com`)
3. Connect: set the API endpoint on the deployed Settings page (or it defaults to Render already).
4. Bind `chesst.js.org` via JS.ORG.

---

## Database schema

Created by Flyway in `backend/src/main/resources/db/migration/`:

- **users** — id, username, email, password_hash, display_name, bio, avatar_url, rating, email_verified, verification_code, verification_code_exp, lichess_username, chesscom_username, timestamps.
- **openings** — id, eco, name, pgn, fen, white_wins, draws, black_wins, created_at. Seeded with 220 ECO entries (V2).
- **games** — id, owner_id, white, black, result, event, site, date_played, eco, opening_name, pgn, start_fen, move_count, source, source_game_id, timestamps.
- **analyses** — id, game_id, user_id, depth, payload (JSON), accuracy_w/b, blunders_w/b, mistakes_w/b, created_at.

---

## License

MIT. Use it, fork it, ship it.
