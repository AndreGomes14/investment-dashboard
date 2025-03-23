import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface PortfolioSummary {
  totalValue: number;
  allocation: {
    stocks: number;
    bonds: number;
    cash: number;
    alternative?: number;
  };
  performance: {
    oneMonth: number;
    threeMonths: number;
    ytd: number;
    oneYear: number;
  };
  latestTransactions?: Array<{
    date: Date;
    type: string;
    asset: string;
    amount: number;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class PortfolioService {
  private apiUrl = environment.apiUrl + '/api/portfolio';
  
  constructor(private http: HttpClient) { }

  /**
   * Get portfolio summary data
   * @returns Observable of portfolio summary
   */
  getPortfolioSummary(): Observable<PortfolioSummary> {
    return this.http.get<PortfolioSummary>(`${this.apiUrl}/summary`)
      .pipe(
        catchError(() => {
          // Return mock data if API fails
          return of({
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
          });
        })
      );
  }
}
