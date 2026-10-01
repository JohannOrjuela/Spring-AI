import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: '../tests/integration',
  testMatch: 'frontend-chat-flow.spec.mjs',
  timeout: 240_000,
  workers: 1,
  expect: { timeout: 15_000 },
  use: {
    baseURL: 'http://localhost:4200',
    browserName: 'chromium'
  }
});
