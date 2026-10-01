import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ChatService } from './chat.service';

describe('ChatService contracts', () => {
  let service: ChatService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({providers:[provideHttpClient(), provideHttpClientTesting()]});
    service=TestBed.inject(ChatService);http=TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('preserves legacy fields and explicit zero options without adding defaults', () => {
    const payload={question:'q',sessionId:'session',temperature:0,topK:0};
    service.sendChat(payload).subscribe();
    const req=http.expectOne('http://localhost:8080/api/v1/chat');
    expect(req.request.body).toEqual(payload);expect(req.request.method).toBe('POST');req.flush({});
  });
  it('sends classification text, context and sampling without chat prompt controls', () => {
    const payload={text:'text',rol:'analista',dominio:'soporte',idioma:'español',seed:0};
    service.classify(payload).subscribe();
    const req=http.expectOne('http://localhost:8080/api/v1/classifications');
    expect(req.request.body).toEqual(payload);req.flush({});
  });
  it('checks model health', () => {
    service.health().subscribe(result => expect(result.ok).toBeTrue());
    http.expectOne('http://localhost:8080/health/llm').flush({ok:true});
  });
});
