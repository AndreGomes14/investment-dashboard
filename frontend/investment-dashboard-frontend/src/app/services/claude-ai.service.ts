import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

export interface ChatResponse {
  message: string;
  error?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ClaudeAiService {
  private apiUrl = environment.apiUrl + '/api/claude-ai';
  
  constructor(private http: HttpClient) { }

  /**
   * Send user message to Claude AI API
   * @param message User's question/message
   * @param conversationHistory Previous messages for context
   * @param dashboardContext Optional dashboard data to provide more context
   * @returns Observable of the AI response
   */
  sendMessage(
    message: string, 
    conversationHistory: ChatMessage[] = [],
    dashboardContext: any = null
  ): Observable<ChatResponse> {
    const payload = {
      message,
      history: conversationHistory,
      context: dashboardContext
    };
    
    return this.http.post<ChatResponse>(`${this.apiUrl}/chat`, payload)
      .pipe(
        catchError(error => {
          console.error('Error communicating with Claude AI:', error);
          return of({
            message: 'Sorry, I encountered an error while processing your request. Please try again later.',
            error: error.message
          });
        })
      );
  }
}
