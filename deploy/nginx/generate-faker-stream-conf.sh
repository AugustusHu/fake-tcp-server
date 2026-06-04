#!/usr/bin/env sh
set -eu

PUBLIC_START="${PUBLIC_START:-14400}"
PUBLIC_END="${PUBLIC_END:-14700}"
LOCAL_OFFSET="${LOCAL_OFFSET:-1000}"

port="$PUBLIC_START"
while [ "$port" -le "$PUBLIC_END" ]; do
  local_port=$((port + LOCAL_OFFSET))
  cat <<EOF
server {
    listen ${port};
    proxy_pass 127.0.0.1:${local_port};
    proxy_connect_timeout 10s;
    proxy_timeout 300s;
}

EOF
  port=$((port + 1))
done
