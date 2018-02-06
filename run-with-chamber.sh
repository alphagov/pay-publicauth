#!/usr/bin/env bash
echo "getting secrets..."
AWS_REGION="${ECS_AWS_REGION}" chamber exec "${ECS_SERVICE}" -- ./docker-startup.sh
