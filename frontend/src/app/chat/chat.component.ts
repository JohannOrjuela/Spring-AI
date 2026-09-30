import { Component } from '@angular/core';
import { ChatService } from '../services/chat.service';
import { ChatResponse } from '../services/chat.service';

@Component({
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.css']
})
export class ChatComponent {
  messages: {from: string, text: string, response?: ChatResponse}[] = [];
  input = '';
  loading = false;

  constructor(private chatService: ChatService) {}

  send() {
    const question = this.input.trim();
    if (!question) return;
    this.messages.push({from: 'user', text: question});
    this.input = '';
    this.loading = true;
    this.chatService.sendQuestion(question).subscribe({
      next: (resp) => {
        this.messages.push({
          from: 'bot',
          text: resp.answer,
          response: resp
        });
        this.loading = false;
      },
      error: (err) => {
        this.messages.push({from: 'bot', text: 'Error: could not get response.'});
        this.loading = false;
      }
    });
  }

  displayValue(value: string | number | null | undefined): string {
    return value === null || value === undefined ? 'null' : String(value);
  }
}
