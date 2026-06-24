# Deployment

This repository produces two separate Docker images:

- Backend: `./wmsBack`
- Frontend: `./wmsFront`

Local development still uses `docker compose up --build`.

## Why `linux/amd64`

Render runs `linux/amd64` images.
On Apple Silicon, a normal `docker build` often produces `linux/arm64`, which Render cannot run.
Use `buildx` with `--platform linux/amd64` for both images.

## Build and push to Docker Hub

Backend:

```bash
docker buildx build --platform linux/amd64 -t whatyz/isd-repos:backend ./wmsBack --push
```

Frontend:

```bash
docker buildx build \
  --platform linux/amd64 \
  --build-arg VITE_API_URL=https://BACKEND_RENDER_URL.onrender.com \
  -t whatyz/isd-repos:frontend-v3 \
  ./wmsFront \
  --push
```

Render image references:

- `docker.io/whatyz/isd-repos:backend`
- `docker.io/whatyz/isd-repos:frontend-v3`

## Render backend env vars

Configure the backend Web Service with:

```env
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://RENDER_DB_HOST:5432/RENDER_DB_NAME
SPRING_DATASOURCE_USERNAME=RENDER_DB_USER
SPRING_DATASOURCE_PASSWORD=RENDER_DB_PASSWORD
SPRING_FLYWAY_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
WMS_APP_URL=https://BACKEND_RENDER_SERVICE.onrender.com
WMS_FRONTEND_URL=https://FRONTEND_RENDER_SERVICE.onrender.com
OPENAI_API_KEY=YOUR_OPENAI_KEY
```

## Render frontend env vars

Build the frontend image with:

```env
VITE_API_URL=https://BACKEND_RENDER_SERVICE.onrender.com
```

The frontend container serves the built Vue app with Nginx on port `80`.
It does not proxy to a Docker Compose service name.

## Local Docker Compose

`docker-compose.yml` remains for local development.
It builds the frontend with:

```env
VITE_API_URL=http://localhost:8080
```

and the backend with local Compose database credentials.

## Notes

- Backend datasource settings come from environment variables.
- Backend CORS allows localhost dev origins and the Render frontend URL.
- Frontend API calls use the centralized Vite variable `VITE_API_URL`.
- Do not use `proxy_pass http://backend:8080` in the Render frontend image.
