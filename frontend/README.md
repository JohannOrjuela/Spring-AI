# Frontend Angular 20

Desde `frontend/`, con Node.js 24:

```sh
npm ci
npm start
```

Abre http://localhost:4200. El backend debe estar disponible en http://localhost:8080; comprueba su estado en http://localhost:8080/health/llm.

- **Chat**: elige conciso, tutor o extractor; puedes añadir rol, dominio e idioma.
- **Clasificación**: envía texto y recibe categoria, confianza (0–100) y justificacion. El servidor selecciona extractor.
- Los cinco parámetros de muestreo son opcionales. Vacío conserva los defaults; cero se envía cuando es válido.
- Cada respuesta muestra las métricas del backend y los errores de validación.
- El contenido se muestra mediante interpolación de texto de Angular.

Validación:

```sh
npm run build
npm test -- --watch=false
npx playwright install chromium
npm run test:e2e
```

Las pruebas unitarias usan ChromeHeadless (puedes configurar `CHROME_BIN`). Playwright necesita backend, Ollama y frontend activos; ejecuta ambos flujos reales una vez.

Desde la raíz del repositorio, `docker compose -f docker/docker-compose.yml up -d --build` construye Angular y sirve `dist/pruebachat-frontend/browser` con Nginx en el puerto 4200. El backend y Ollama mantienen los puertos 8080 y 11434.

Requisitos de hardware, variables, procedencia y términos del modelo: [quickstart de la feature 005](../specs/005-prompt-templates-structured-output/quickstart.md).
