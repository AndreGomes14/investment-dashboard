import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { Portfolio } from '../model/portfolio.model';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Investment } from '../model/investment.model';

// Define interfaces matching backend DTOs
export interface InvestmentPerformance {
  ticker: string;
  type: string;
  unrealizedPnlPercentage: number;
  currentValue: number; // Current value per unit
  holdingValue: number; // Total current value of the holding
}

export interface PortfolioSummaryMetrics {
  portfolioName: string;
  totalValue: number;
  totalCostBasis: number;
  unrealizedPnlAbsolute: number;
  unrealizedPnlPercentage: number;
  realizedPnlAbsolute: number;
  assetAllocationByValue: { [key: string]: number }; // Map<string, number>
  activeInvestmentsCount: number;
  bestPerformer: InvestmentPerformance | null;
  worstPerformer: InvestmentPerformance | null;
}

export interface PortfolioSummaryResponse {
  summary: PortfolioSummaryMetrics;
  activeInvestments: Investment[];
  soldInvestments: Investment[];
}

@Injectable({
  providedIn: 'root'
})
export class PortfolioService {
  private readonly portfolioApiUrl = environment.apiUrl + '/api/portfolios';

  constructor(private readonly http: HttpClient, private readonly snackBar: MatSnackBar) { }

  /**
   * Get overall portfolio summary data.
   * @returns Observable of PortfolioSummaryResponse or null.
   */
  getOverallSummary(): Observable<PortfolioSummaryResponse | null> {
    const url = `${this.portfolioApiUrl}/summary`;
    return this.http.get<PortfolioSummaryResponse>(url).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('Error fetching overall portfolio summary:', error);
        this.snackBar.open('Failed to load overall portfolio summary.', 'Close', { duration: 3000 });
        return of(null);
      })
    );
  }

  /**
   * Get summary data for a specific portfolio.
   * @param portfolioId The ID of the portfolio.
   * @returns Observable of PortfolioSummaryResponse or null.
   */
  getPortfolioSummary(portfolioId: number): Observable<PortfolioSummaryResponse | null> {
    const url = `${this.portfolioApiUrl}/${portfolioId}/summary`;
    return this.http.get<PortfolioSummaryResponse>(url).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error(`Error fetching summary for portfolio ${portfolioId}:`, error);
        this.snackBar.open(`Failed to load summary for portfolio.`, 'Close', { duration: 3000 });
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
