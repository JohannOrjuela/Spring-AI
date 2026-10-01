import { test, expect } from '../../frontend/node_modules/@playwright/test/index.mjs';

const metricFields = ['requestId','elapsedMs','status','promptTokens','completionTokens','totalTokens','model','finishReason','tokensPerSecond','errors'];
test('health probe is reachable from the browser origin', async ({ page }) => {
  await page.goto('/');
  await expect(page.locator('.dot')).toHaveAttribute('data-state', 'up');
  await expect(page.locator('.probe')).toContainText('ollama activo');
});
async function assertMetrics(turn) {
  for (const field of metricFields)
    await expect(turn.locator('.contract-key', { hasText: new RegExp('^' + field + '$') })).toBeVisible();
}
test('template-selected chat preserves sampling and feature 003 metrics', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.goto('/');
  await page.locator('#templateId').selectOption('tutor');
  await page.locator('#rol').fill('docente');
  await page.locator('#dominio').fill('programacion');
  await page.locator('#idioma').fill('espanol');
  await page.locator('#sampling summary').click();
  await page.locator('#temperature').fill('0');
  await page.locator('#topP').fill('0.9');
  await page.locator('#topK').fill('0');
  await page.locator('#numPredict').fill('100');
  await page.locator('#seed').fill('7');
  await page.locator('#input').fill('Explica una interfaz Java en una frase.');
  const responsePromise = page.waitForResponse(response => response.url().endsWith('/api/v1/chat') && response.request().method() === 'POST');
  await page.locator('#send').click();
  const response = await responsePromise;
  expect(response.request().postDataJSON()).toMatchObject({
    templateId:'tutor', rol:'docente', dominio:'programacion', idioma:'espanol',
    temperature:0, topP:0.9, topK:0, numPredict:100, seed:7
  });
  expect(response.status()).toBe(200);
  const turn = page.locator('.turn[data-who="bot"]').last();
  await expect(turn).toBeVisible();
  await expect(turn.locator('.body')).not.toHaveText('');
  await expect(turn.locator('.contract-row')).toHaveCount(11);
  await assertMetrics(turn);

  await page.locator('#input').fill('Continua con un segundo ejemplo breve.');
  const secondResponsePromise = page.waitForResponse(response => response.url().endsWith('/api/v1/chat') && response.request().method() === 'POST');
  await page.locator('#send').click();
  const secondResponse = await secondResponsePromise;
  expect(secondResponse.status()).toBe(200);
  expect(secondResponse.request().postDataJSON().sessionId).toBe(response.request().postDataJSON().sessionId);
  const secondTurn = page.locator('.turn[data-who="bot"]').last();
  await expect(secondTurn.locator('.body')).not.toHaveText('');
  await expect(secondTurn.locator('.contract-row')).toHaveCount(11);
  await assertMetrics(secondTurn);
  expect(errors).toEqual([]);
});
test('classification displays typed result and metrics from the model', async ({ page }) => {
  await page.goto('/');
  await page.locator('#operation').selectOption('classification');
  await expect(page.locator('#templateId')).toHaveCount(0);
  await page.locator('#dominio').fill('soporte tecnico');
  await page.locator('#sampling summary').click();
  await page.locator('#temperature').fill('0');
  await page.locator('#numPredict').fill('256');
  await page.locator('#input').fill('No puedo iniciar sesion en mi cuenta.');
  const responsePromise = page.waitForResponse(response => response.url().endsWith('/api/v1/classifications') && response.request().method() === 'POST');
  await page.locator('#send').click();
  const response = await responsePromise;
  expect(response.request().postDataJSON()).toEqual({
    text:'No puedo iniciar sesion en mi cuenta.', dominio:'soporte tecnico', temperature:0, numPredict:256
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.classification.categoria.trim()).not.toBe('');
  expect(body.classification.justificacion.trim()).not.toBe('');
  expect(Number.isInteger(body.classification.confianza)).toBe(true);
  expect(body.classification.confianza).toBeGreaterThanOrEqual(0);
  expect(body.classification.confianza).toBeLessThanOrEqual(100);
  const turn = page.locator('.turn[data-who="bot"]').last();
  await expect(turn).toContainText(body.classification.categoria);
  await expect(turn).toContainText(body.classification.justificacion);
  await expect(turn.locator('.contract-row')).toHaveCount(12);
  await assertMetrics(turn);
});
