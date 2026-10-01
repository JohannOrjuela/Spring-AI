import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TimeoutError } from 'rxjs';
import { ChatService, ChatResponse, ClassificationResponse, Sampling, Context } from '../services/chat.service';

type ApiResponse = ChatResponse | ClassificationResponse;
type Turn = { who: 'user' | 'bot' | 'fault'; label: string; text: string; contract?: ApiResponse };

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent implements OnInit {
  input = '';
  operation: 'chat' | 'classification' = 'chat';
  templateId: 'conciso' | 'tutor' | 'extractor' = 'conciso';
  rol = ''; dominio = ''; idioma = '';
  temperature: number | null = null;
  topP: number | null = null;
  topK: number | null = null;
  numPredict: number | null = null;
  seed: number | null = null;
  loading = false;
  healthText = 'comprobando el modelo…';
  healthState = 'probing';
  turns: Turn[] = [];
  readonly sessionId = globalThis.crypto?.randomUUID?.() ?? String(Date.now());

  constructor(private readonly api: ChatService) {}
  ngOnInit(): void { this.probe(); }

  probe(): void {
    this.api.health().subscribe({
      next: result => {
        this.healthState = result.ok ? 'up' : 'down';
        const names = result.result?.models?.map(model => model.name).filter(Boolean).join(', ');
        this.healthText = result.ok ? (names ? `ollama activo · ${names}` : 'ollama activo') : 'ollama no responde';
      },
      error: () => { this.healthState = 'down'; this.healthText = 'backend inalcanzable'; }
    });
  }

  send(): void {
    const text = this.input.trim();
    if (!text || this.loading) return;
    this.turns.push({ who: 'user', label: 'tú', text });
    this.input = '';
    this.loading = true;
    const observer = {
      next: (response: ApiResponse) => {
        const classification = 'classification' in response ? response.classification : null;
        const content = classification
          ? `${classification.categoria} · ${classification.confianza}%\n${classification.justificacion}`
          : response.answer ?? '';
        const valid = response.status === 'ok' && content.trim().length > 0;
        this.turns.push({
          who: valid ? 'bot' : 'fault',
          label: valid ? (classification ? 'clasificación' : 'modelo') : 'error',
          text: valid ? content : 'El modelo devolvió una respuesta vacía o inválida.',
          contract: response
        });
        this.loading = false;
        this.probe();
      },
      error: (error: HttpErrorResponse | TimeoutError) => {
        const contract = error instanceof HttpErrorResponse && error.error && typeof error.error === 'object'
          && typeof error.error.requestId === 'string' ? error.error as ApiResponse : undefined;
        const message = error instanceof TimeoutError ? 'Sin respuesta tras 180 s. Intenta de nuevo.'
          : error instanceof HttpErrorResponse && error.status
            ? `El backend respondió HTTP ${error.status}.`
            : 'No se pudo contactar el backend.';
        this.turns.push({ who: 'fault', label: 'error', text: message, contract });
        this.loading = false;
        this.probe();
      }
    };
    const options = { ...this.context(), ...this.sampling() };
    if (this.operation === 'chat') {
      this.api.sendChat({ question: text, sessionId: this.sessionId, templateId: this.templateId, ...options }).subscribe(observer);
    } else {
      this.api.classify({ text, ...options }).subscribe(observer);
    }
  }

  private sampling(): Sampling {
    const result: Sampling = {};
    for (const key of ['temperature', 'topP', 'topK', 'numPredict', 'seed'] as const) {
      if (this[key] !== null) result[key] = this[key];
    }
    return result;
  }

  private context(): Context {
    const result: Context = {};
    for (const key of ['rol', 'dominio', 'idioma'] as const) {
      if (this[key].trim()) result[key] = this[key].trim();
    }
    return result;
  }

  display(value: unknown): string {
    return value === null || value === undefined ? 'null' : typeof value === 'object' ? JSON.stringify(value) : String(value);
  }

  keys(contract: ApiResponse): string[] {
    return ['requestId', ...('classification' in contract ? ['classification', 'answer'] : ['answer']),
      'elapsedMs', 'status', 'promptTokens', 'completionTokens', 'totalTokens', 'model', 'finishReason', 'tokensPerSecond', 'errors'];
  }
}
