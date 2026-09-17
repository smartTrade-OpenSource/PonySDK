// @ts-check
const path = require('path');
const { defineConfig } = require('@playwright/test');

// The FlexLayout demo runs under the `flexlayout` Spring profile, which listens on 8081.
const baseURL = process.env.BASE_URL || 'http://localhost:8081/sample/';

// The wrapper lives at the repository root, one level above this file. The path is built
// absolute on purpose: cmd.exe does not resolve a bare `gradlew.bat` from the working
// directory, so a relative command works on Linux and fails on Windows.
const repoRoot = path.resolve(__dirname, '..');
const gradlew = path.join(repoRoot, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew');

module.exports = defineConfig({
  testDir: './tests',
  timeout: 30000,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : 4,
  use: {
    baseURL,
    headless: true,
    viewport: { width: 1280, height: 720 },
    actionTimeout: 5000,
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } },
  ],
  // Playwright owns the demo server: it waits for the port to answer, surfaces the server
  // output when startup fails, and tears the process down afterwards. Declaring it here
  // means a local run and CI start the server the same way, from a single definition.
  webServer: {
    command: `"${gradlew}" :sample:processResources :sample:runFlexLayoutDemo --no-daemon`,
    cwd: repoRoot,
    url: baseURL,
    timeout: 180000,
    reuseExistingServer: !process.env.CI,
    stdout: 'pipe',
    stderr: 'pipe',
  },
});
