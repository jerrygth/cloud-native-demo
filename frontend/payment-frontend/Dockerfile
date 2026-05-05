FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build
# -------- Stage 2: Serve with Nginx

FROM nginx:stable-alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

# ── ENTRYPOINT SCRIPT ──────────────────────────────────────────────
# This script runs at container startup and:
#   1. Reads VITE_* env vars injected by K8s ConfigMap (envFrom)
#   2. Writes /usr/share/nginx/html/env-config.js
#   3. Starts Nginx

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
EXPOSE 80
ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]