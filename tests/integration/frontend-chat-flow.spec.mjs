import { test, expect } from '../../frontend/node_modules/@playwright/test/index.mjs';

test('submits a chat and renders the answer with feature 003 metrics', async ({ page }) => {
  await page.goto('/');
  await page.locator('#sampling summary').click();
  await page.locator('#temperature').fill('0.7');
  await page.locator('#topP').fill('0.9');
  await page.locator('#topK').fill('40');
  await page.locator('#numPredict').fill('50');
  await page.locator('#seed').fill('7');
  await page.locator('#input').fill('Responde solamente con la palabra listo.');

  const requestPromise = page.waitForRequest(request => request.url().endsWith('/api/v1/chat'));
  await page.locator('#send').click();
  const request = await requestPromise;
  expect(request.postDataJSON()).toMatchObject({
    temperature: 0.7,
    topP: 0.9,
    topK: 40,
    numPredict: 50,
    seed: 7
  });

  const response = page.locator('.turn[data-who="bot"]').last();
  await expect(response).toBeVisible({ timeout: 240_000 });
  await expect(response.locator('.body')).not.toHaveText('');
  await expect(response.locator('.contract-row')).toHaveCount(11);

  for (const field of [
    'requestId', 'answer', 'elapsedMs', 'status', 'promptTokens',
    'completionTokens', 'totalTokens', 'model', 'finishReason', 'tokensPerSecond', 'errors'
  ]) {
    await expect(response.locator('.contract-key', { hasText: field })).toBeVisible();
  }
});
