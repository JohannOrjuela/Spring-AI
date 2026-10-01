import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timeout } from 'rxjs';

export interface Sampling {
  temperature?: number; topP?: number; topK?: number; numPredict?: number; seed?: number;
}
export interface Context { rol?: string; dominio?: string; idioma?: string; }
export interface Metrics {
  requestId: string;
  elapsedMs: number;
  status: string;
  promptTokens: number | null;
  completionTokens: number | null;
  totalTokens: number | null;
  model: string | null;
  finishReason: string | null;
  tokensPerSecond: number | null;
  errors: { field: string; message: string }[] | null;
}
export interface ChatRequest extends Sampling, Context {
  question: string; sessionId: string; templateId?: 'conciso' | 'tutor' | 'extractor';
}
export interface ChatResponse extends Metrics { answer: string; }
export interface ClassificationRequest extends Sampling, Context { text: string; }
export interface Clasificacion { categoria: string; confianza: number; justificacion: string; }
export interface ClassificationResponse extends Metrics {
  classification: Clasificacion | null;
  answer: string | null;
}
export interface HealthResponse { ok: boolean; result?: { models?: { name?: string }[] }; }

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly api = 'http://localhost:8080';
  constructor(private readonly http: HttpClient) {}
  health(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>(`${this.api}/health/llm`).pipe(timeout(8000));
  }
  sendChat(request: ChatRequest): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(`${this.api}/api/v1/chat`, request).pipe(timeout(180000));
  }
  classify(request: ClassificationRequest): Observable<ClassificationResponse> {
    return this.http.post<ClassificationResponse>(`${this.api}/api/v1/classifications`, request).pipe(timeout(180000));
  }
}
