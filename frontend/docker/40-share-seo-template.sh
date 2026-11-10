#!/bin/sh
set -eu

# 脚本通过“写临时文件，再重命名”更新，避免后端读到只复制了一半的 HTML。
mkdir -p /seo-template
cp /usr/share/nginx/html/index.html /seo-template/index.html.tmp
chmod 644 /seo-template/index.html.tmp
mv /seo-template/index.html.tmp /seo-template/index.html
