import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatResponse {
  requestId: string;
  answer: string;
  elapsedMs: number;
  status: string;
  promptTokens: number | null;
  completionTokens: number | null;
  totalTokens: number | null;
  model: string | null;
  finishReason: string | null;
  tokensPerSecond: number | null;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private url = '/api/v1/chat';
  constructor(private http: HttpClient) {}

  sendQuestion(question: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.url, { question });
  }
}
