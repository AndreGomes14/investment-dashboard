import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Portfolio } from '../model/portfolio.model';
import { MatSnackBar } from '@angular/material/snack-bar';

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
  private readonly portfolioApiUrl = environment.apiUrl + '/api/portfolios';
  private readonly summaryApiUrl = environment.apiUrl + '/api/portfolio/summary';

  constructor(private readonly http: HttpClient, private readonly snackBar: MatSnackBar) { }

  /**
   * Get portfolio summary data.
   * Returns null if the API call fails.
   * @returns Observable of portfolio summary or null.
   */
  getPortfolioSummary(): Observable<PortfolioSummary | null> {
    return this.http.get<PortfolioSummary>(this.summaryApiUrl)
      .pipe(
        catchError((error: HttpErrorResponse) => {
          console.error('Error fetching portfolio summary:', error);
          return of(null);
        })
      );
  }

  /**
   * Fetches all portfolios for the logged-in user.
   * Assumes the backend filters portfolios based on the authenticated user.
   * @returns Observable of Portfolio array or null on error/no data.
   */
  getUserPortfolios(): Observable<Portfolio[] | null> {
    return this.http.get<Portfolio[]>(this.portfolioApiUrl).pipe(
      map(portfolios => {
        return (Array.isArray(portfolios) && portfolios.length > 0) ? portfolios : null;
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('Error fetching user portfolios:', error);
        this.snackBar.open('Failed to fetch portfolios. Please try again later.', 'Close', { duration: 5000 });
        return of(null);
      })
    );
  }

  /**
   * Creates a new portfolio.
   * @param portfolioData Data for the new portfolio (e.g., { name: string, description?: string })
   * @returns Observable of the created Portfolio or null on error.
   */
  createPortfolio(portfolioData: { name: string; description?: string }): Observable<Portfolio | null> {
    // We don't send userId, assuming backend assigns it based on logged-in user
    return this.http.post<Portfolio>(this.portfolioApiUrl, portfolioData).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Error creating portfolio:', error);
        // Provide more specific error message if possible
        const message = error.error?.message || 'Failed to create portfolio. Please check the details and try again.';
        this.snackBar.open(message, 'Close', { duration: 5000 });
        return of(null);
      })
    );
  }

  /**
   * Updates an existing portfolio.
   * @param portfolioId The ID of the portfolio to update.
   * @param portfolioData The updated portfolio data (e.g., { name: string, description?: string })
   * @returns Observable of the updated Portfolio or null on error.
   */
  updatePortfolio(portfolioId: number, portfolioData: { name: string; description?: string }): Observable<Portfolio | null> {
    return this.http.put<Portfolio>(`${this.portfolioApiUrl}/${portfolioId}`, portfolioData).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Error updating portfolio:', error);
        const message = error.error?.message || 'Failed to update portfolio. Please check the details and try again.';
        this.snackBar.open(message, 'Close', { duration: 5000 });
        return of(null);
      })
    );
  }

  /**
   * Deletes a portfolio by its ID.
   * @param portfolioId The ID of the portfolio to delete.
   * @returns Observable<boolean> indicating success (true) or failure (false).
   */
  deletePortfolio(portfolioId: number): Observable<boolean> {
    return this.http.delete<void>(`${this.portfolioApiUrl}/${portfolioId}`).pipe(
      map(() => true), // Return true on successful delete (2xx status)
      catchError((error: HttpErrorResponse) => {
        console.error('Error deleting portfolio:', error);
        const message = error.error?.message || 'Failed to delete portfolio.';
        this.snackBar.open(message, 'Close', { duration: 5000 });
        return of(false); // Return false on error
      })
    );
  }
}
