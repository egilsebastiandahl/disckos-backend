# Disckos Backend - Admin Auth Scaffold

Minimal single-admin auth with HS256 JWT access + refresh tokens (no users table).

## Environment variables

Required env vars:
- `ADMIN_USERNAME` - admin username
- `ADMIN_PASSWORD` - bcrypt hash of admin password
- `JWT_SECRET` - HS256 secret for access tokens (at least 32 bytes)
- `REFRESH_JWT_SECRET` - HS256 secret for refresh tokens (at least 32 bytes)
- `ACCESS_TOKEN_EXPIRY_SECONDS` - access token lifetime in seconds (e.g., 900)
- `REFRESH_TOKEN_EXPIRY_SECONDS` - refresh token lifetime in seconds (e.g., 604800)

Example `.env` for local dev:
```
ADMIN_USERNAME=admin
ADMIN_PASSWORD=$2a$10$ZtZ4zA8mKx3aPh3Ox9D9Yekz3vTtXawvyP4vuwF6wglcUo5G7w0Bq
JWT_SECRET=dev-secret-dev-secret-dev-secret-dev-secret
REFRESH_JWT_SECRET=dev-refresh-dev-refresh-dev-refresh-dev
ACCESS_TOKEN_EXPIRY_SECONDS=900
REFRESH_TOKEN_EXPIRY_SECONDS=604800
```

## Generate bcrypt hash

Option 1 (Python, requires `pip install bcrypt`):
```
python - <<'PY'
import bcrypt
print(bcrypt.hashpw(b"your-password", bcrypt.gensalt()).decode())
PY
```

Option 2 (Apache `htpasswd`):
```
htpasswd -nbBC 12 admin "your-password" | cut -d: -f2
```

## Dependencies (Gradle)

```
implementation("org.springframework.boot:spring-boot-starter-webmvc")
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
implementation("tools.jackson.module:jackson-module-kotlin")
```

## Curl examples

Login:
```
curl -X POST http://localhost:8080/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"secret"}'
```

Refresh:
```
curl -X POST http://localhost:8080/admin/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshJwt>"}'
```

Protected request:
```
curl -X GET http://localhost:8080/admin/players \
  -H "Authorization: Bearer <accessJwt>"
```

## Security notes

- Use BCrypt to check password hashes.
- Keep secrets in env vars, not in code.
- Use HTTPS in production.
- Rate-limit `/admin/login` and `/admin/refresh` (plug in a limiter like Bucket4j or Resilience4j).
- With a single-admin/no-DB setup, refresh-token revocation is not possible server-side. To revoke, rotate `REFRESH_JWT_SECRET` or add a DB table for refresh tokens later.
