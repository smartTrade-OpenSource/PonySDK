// @ts-check
const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',
  timeout: 30000,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : 4,
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost/sample/',
    headless: true,
    viewport: { width: 1280, height: 720 },
    actionTimeout: 5000,
  },
  projects: [
    // PW_CHANNEL=chrome runs against the locally installed Google Chrome instead of the bundled build
    { name: 'chromium', use: { browserName: 'chromium', ...(process.env.PW_CHANNEL ? { channel: process.env.PW_CHANNEL } : {}) } },
  ],
});
