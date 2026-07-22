# Frontend estático servido por Caddy, en una sola imagen.
#
# El contexto de build es la RAÍZ del repo (ver docker-compose.prod.yml), para
# poder copiar tanto frontend/ como deploy/Caddyfile.

FROM node:24-alpine AS build
WORKDIR /app

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build

FROM caddy:2-alpine
COPY --from=build /app/dist /srv
COPY deploy/Caddyfile /etc/caddy/Caddyfile
