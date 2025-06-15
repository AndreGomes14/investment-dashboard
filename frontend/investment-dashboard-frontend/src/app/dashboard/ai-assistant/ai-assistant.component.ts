import { Component, OnInit, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { finalize } from 'rxjs/operators';

import { ClaudeAiService, ChatMessage } from '../../services/claude-ai.service';
import { PortfolioService } from '../../services/portfolio.service';

@Component({
  selector: 'app-ai-assistant',
  templateUrl: './ai-assistant.component.html',
  styleUrls: ['./ai-assistant.component.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ]
})
export class AiAssistantComponent implements OnInit, AfterViewChecked {
  chatForm: FormGroup;
  messages: ChatMessage[] = [];
  isLoading = false;
  portfolioData: any = null;
  
  @ViewChild('chatMessages') private messagesContainer!: ElementRef;

  constructor(
    private fb: FormBuilder,
    private claudeService: ClaudeAiService,
    private portfolioService: PortfolioService
  ) {
    this.chatForm = this.fb.group({
      message: ['', [Validators.required, Validators.minLength(2)]]
    });
  }

  ngOnInit(): void {
    // Get portfolio data to provide as context to the AI
    this.loadPortfolioContext();
    
    // Add a welcome message
    this.messages.push({
      role: 'assistant',
      content: 'Hello! I\'m your AI financial assistant. How can I help you with your investments today?',
      timestamp: new Date()
    });
  }
  
  ngAfterViewChecked() {
    this.scrollToBottom();
  }
  
  scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
    } catch(err) { }
  }

  loadPortfolioContext() {
    // This is a placeholder - replace with actual portfolio service call
    // this.portfolioService.getPortfolioSummary().subscribe(data => {
    //   this.portfolioData = data;
    // });
    
    // For now, we'll use mock data
    this.portfolioData = {
      totalValue: 250000,
      allocation: {
        stocks: 60,
        bonds: 30,
        cash: 10
      },
      performance: {
        oneMonth: 2.5,
        threeMonths: 4.2,
        ytd: 7.8,
        oneYear: 12.3
      }
    };
  }

  sendMessage() {
    if (this.chatForm.invalid || this.isLoading) return;
    
    const userMessage = this.chatForm.get('message')?.value;
    
    // Add user message to the chat
    this.messages.push({
      role: 'user',
      content: userMessage,
      timestamp: new Date()
    });
    
    // Clear the input field
    this.chatForm.get('message')?.reset();
    
    // Set loading state
    this.isLoading = true;
    
    // Send the message to Claude AI
    this.claudeService.sendMessage(userMessage, this.messages, this.portfolioData)
      .pipe(
        finalize(() => this.isLoading = false)
      )
      .subscribe(response => {
        // Add AI response to the chat
        this.messages.push({
          role: 'assistant',
          content: response.message,
          timestamp: new Date()
        });
      });
  }
}
