import { defineConfig } from '@playwright/test'

const baseURL = process.env.BASE_URL ?? 'http://127.0.0.1:8080'

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  timeout: 240_000,
  expect: { timeout: 15_000 },
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    // Failure artifacts required by the e2e workflow.
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
    video: 'off',
    navigationTimeout: 60_000,
    actionTimeout: 30_000,
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
})
