#!/bin/sh
set -e

ENV_CONFIG_FILE="/usr/share/nginx/html/env-config.js"

echo "Generating runtime env config..."

cat > "$ENV_CONFIG_FILE" << EOF
window.__ENV__ = {
  VITE_API_GATEWAY_URL: "${VITE_API_GATEWAY_URL:-http://localhost:8081}",
  VITE_LOGIN_URL: "${VITE_LOGIN_URL:-http://localhost:8081/oauth2/authorization/auth0}",
  VITE_LOGOUT_URL: "${VITE_LOGOUT_URL:-http://localhost:8081/logout}"
};
EOF

echo "env-config.js written:"
cat "$ENV_CONFIG_FILE"

# Start the command passed as arguments (nginx -g 'daemon off;')
exec "$@"