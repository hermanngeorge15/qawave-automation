# QAWave E2E Tests Docker Image
# Uses official Playwright image with browsers pre-installed

FROM mcr.microsoft.com/playwright:v1.44.0-jammy

# Set working directory
WORKDIR /app

# Copy package files first (for better caching)
COPY package*.json ./

# Install dependencies
RUN npm ci

# Copy test source files
COPY tsconfig.json ./
COPY playwright.config.ts ./
COPY playwright.staging.config.ts ./
COPY src/ ./src/

# Set environment variables
ENV CI=true
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

# Create directories for reports
RUN mkdir -p /app/test-results /app/playwright-report /app/results

# Health check - verify playwright is working
RUN npx playwright --version

# Default command - run staging tests
CMD ["npx", "playwright", "test", "--config=playwright.staging.config.ts", "--reporter=list,html,junit"]
