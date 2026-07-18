#!/bin/bash
export PATH="$HOME/.acme.sh:$PATH"
echo "=== issue cert webroot ==="
mkdir -p /var/www/toogulu/.well-known/acme-challenge
~/.acme.sh/acme.sh --issue -d toogoolu.szh5.cn -w /var/www/toogulu --keylength 2048
echo "ISSUE_RESULT=$?"

echo "=== install cert ==="
mkdir -p /etc/nginx/ssl/toogoolu
~/.acme.sh/acme.sh --install-cert -d toogoolu.szh5.cn \
  --key-file /etc/nginx/ssl/toogoolu/privkey.pem \
  --fullchain-file /etc/nginx/ssl/toogoolu/fullchain.pem \
  --reloadcmd "systemctl reload nginx"
echo "INSTALL_RESULT=$?"

echo "=== cert files ==="
ls -la /etc/nginx/ssl/toogoolu/
