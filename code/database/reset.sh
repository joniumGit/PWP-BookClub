#!/bin/bash
cd "$(dirname "$0")" || exit
docker-compose up -d
docker-compose down -v