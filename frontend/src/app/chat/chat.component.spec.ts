import { TestBed, ComponentFixture } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { ChatComponent } from './chat.component';
import { ChatService } from '../services/chat.service';

describe('ChatComponent', () => {
  let fixture:ComponentFixture<ChatComponent>;
  let api:jasmine.SpyObj<ChatService>;
  const metrics={requestId:'request',elapsedMs:10,status:'ok',promptTokens:1,completionTokens:2,totalTokens:3,model:'model',finishReason:'stop',tokensPerSecond:200,errors:null};
  beforeEach(async () => {
    api=jasmine.createSpyObj('ChatService',['health','sendChat','classify']);
    api.health.and.returnValue(of({ok:true,result:{models:[{name:'gemma3:4b'}]}}));
    api.sendChat.and.returnValue(of({...metrics,answer:'<img src=x onerror=alert(1)>'}));
    api.classify.and.returnValue(of({...metrics,answer:null,classification:{categoria:'soporte',confianza:90,justificacion:'Acceso'}}));
    await TestBed.configureTestingModule({imports:[ChatComponent],providers:[{provide:ChatService,useValue:api}]}).compileComponents();
    fixture=TestBed.createComponent(ChatComponent);fixture.detectChanges();
  });
  it('shows health and only registered template choices', () => {
    expect(fixture.nativeElement.textContent).toContain('gemma3:4b');
    const ids=Array.from(fixture.nativeElement.querySelectorAll('#templateId option')).map((x:any)=>x.value);
    expect(ids).toEqual(['conciso','tutor','extractor']);
    expect(fixture.nativeElement.querySelector('[name=systemPrompt],[name=promptPath]')).toBeNull();
  });
  it('keeps a session, sends zero values, omits defaults and renders response safely', () => {
    const component=fixture.componentInstance;
    component.temperature=0;component.topK=0;component.input='hello';component.send();fixture.detectChanges();
    const request=api.sendChat.calls.mostRecent().args[0];
    expect(request.temperature).toBe(0);expect(request.topK).toBe(0);expect(request.topP).toBeUndefined();
    expect(request.sessionId).toBeTruthy();expect(request.question).toBe('hello');
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('<img src=x');
    expect(fixture.nativeElement.querySelectorAll('.contract-row').length).toBe(11);
    component.input='again';component.send();expect(api.sendChat.calls.mostRecent().args[0].sessionId).toBe(request.sessionId);
  });
  it('sends text for classification and displays its three fields with metrics', () => {
    const c=fixture.componentInstance;c.operation='classification';c.input='cannot login';c.dominio='soporte';c.send();fixture.detectChanges();
    expect(api.classify.calls.mostRecent().args[0]).toEqual({text:'cannot login',dominio:'soporte'});
    expect(api.sendChat).not.toHaveBeenCalled();expect(fixture.nativeElement.textContent).toContain('soporte');
    expect(fixture.nativeElement.textContent).toContain('90');expect(fixture.nativeElement.textContent).toContain('Acceso');
  });
  it('renders error envelopes and clears loading', () => {
    api.sendChat.and.returnValue(throwError(()=>new HttpErrorResponse({status:400,error:{...metrics,status:'error',answer:'Request validation failed'}})));
    fixture.componentInstance.input='x';fixture.componentInstance.send();fixture.detectChanges();
    expect(fixture.componentInstance.loading).toBeFalse();expect(fixture.nativeElement.textContent).toContain('400');
  });
});
