# Multi-stage build for React application
FROM node:20-alpine AS build

WORKDIR /app

# Copy package files and install dependencies (cached layer)
COPY package.json package-lock.json* ./
RUN npm ci

# Copy source code and build
COPY . .
RUN npm run build

# Production stage with nginx
FROM nginx:alpine

# Copy built assets from build stage
COPY --from=build /app/dist /usr/share/nginx/html

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]
