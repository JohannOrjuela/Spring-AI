import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

interface ChatResponse { requestId: string; answer: string; elapsedMs: number; status: string }

@Injectable({ providedIn: 'root' })
export class ChatService {
  private url = '/api/v1/chat';
  constructor(private http: HttpClient) {}

  sendQuestion(question: string): Observable<ChatResponse> {
    return this.http.post<ChatResponse>(this.url, { question });
  }
}
