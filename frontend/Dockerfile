# ==============================================================================
# QAWave Frontend Dockerfile
# Multi-stage build for optimized production image
# ==============================================================================
# Build: docker build -t qawave/frontend:latest .
# Run:   docker run -p 80:8080 qawave/frontend:latest
# ==============================================================================

# Build arguments
ARG NODE_VERSION=20
ARG NGINX_VERSION=1.25

# ------------------------------------------------------------------------------
# Stage 1: Install dependencies
# ------------------------------------------------------------------------------
FROM node:${NODE_VERSION}-alpine AS deps

WORKDIR /app

# Copy package files
COPY package.json package-lock.json* ./

# Install dependencies (production only for smaller image)
RUN npm ci --prefer-offline --no-audit

# ------------------------------------------------------------------------------
# Stage 2: Build the application
# ------------------------------------------------------------------------------
FROM node:${NODE_VERSION}-alpine AS builder

WORKDIR /app

# Copy dependencies from deps stage
COPY --from=deps /app/node_modules ./node_modules

# Copy source code
COPY . .

# Build arguments for environment configuration
ARG VITE_API_BASE_URL
ARG VITE_AUTH_ENABLED=false

# Build the application
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 3: Production image
# ------------------------------------------------------------------------------
FROM nginx:${NGINX_VERSION}-alpine

# Image metadata
LABEL org.opencontainers.image.title="QAWave Frontend" \
      org.opencontainers.image.description="QAWave Web Application" \
      org.opencontainers.image.vendor="QAWave" \
      org.opencontainers.image.source="https://github.com/hermanngeorge15/qawave-automation" \
      org.opencontainers.image.licenses="MIT"

# Install security updates and required tools
# hadolint ignore=DL3018
RUN apk add --no-cache \
    curl \
    && apk upgrade --no-cache \
    && rm -rf /var/cache/apk/*

# Create non-root user
RUN addgroup -g 1000 nginx-app && \
    adduser -u 1000 -G nginx-app -D -s /sbin/nologin nginx-app && \
    # Create required directories
    mkdir -p /var/cache/nginx /var/run/nginx /var/log/nginx && \
    # Set ownership
    chown -R nginx-app:nginx-app /var/cache/nginx /var/run/nginx /var/log/nginx /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/nginx.conf
COPY --chown=nginx-app:nginx-app nginx.conf /etc/nginx/conf.d/default.conf

# Copy built assets from builder stage
COPY --from=builder --chown=nginx-app:nginx-app /app/dist /usr/share/nginx/html

# Remove default nginx config and unnecessary files
RUN rm -rf /etc/nginx/conf.d/default.conf.bak /docker-entrypoint.d

# Security: Remove server tokens
RUN sed -i 's/# server_tokens off;/server_tokens off;/' /etc/nginx/nginx.conf || true

# Switch to non-root user
USER nginx-app

# Expose port (non-privileged)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
