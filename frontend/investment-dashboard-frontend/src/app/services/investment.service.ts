import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment'; // Ensure correct path
import { Investment } from '../model/investment.model'; // Assuming model exists
import { HttpErrorResponse } from '@angular/common/http'; // Import HttpErrorResponse
import { MatSnackBar } from '@angular/material/snack-bar'; // Import MatSnackBar

@Injectable({
  providedIn: 'root'
})
export class InvestmentService {
  private readonly investmentsApiUrl = environment.apiUrl + '/api/investments';
  private readonly portfolioApiUrl = environment.apiUrl + '/api/portfolios'; // Need portfolio endpoint base

  constructor(private readonly http: HttpClient, private readonly snackBar: MatSnackBar) { } // Inject MatSnackBar

  /**
   * Fetches all investments (potentially across all user portfolios).
   * Consider if this method is still needed or if fetching should always be per-portfolio.
   * @returns Observable of Investment array or null on error/no data
   */
  getAllInvestments(): Observable<Investment[] | null> {
    return this.http.get<Investment[]>(this.investmentsApiUrl).pipe(
      map(investments => {
        return (Array.isArray(investments) && investments.length > 0) ? investments : null;
      }),
      catchError(error => {
        console.error('Error fetching all investments:', error);
        this.snackBar.open('Failed to load investment data.', 'Close', { duration: 3000 });
        return of(null);
      })
    );
  }

  /**
   * Fetches investments for a specific portfolio.
   * @param portfolioId The ID of the portfolio.
   * @returns Observable of Investment array or null on error/no data.
   */
  getInvestmentsByPortfolioId(portfolioId: number): Observable<Investment[] | null> {
    // Construct the URL, e.g., /api/portfolios/{portfolioId}/investments
    const url = `${this.portfolioApiUrl}/${portfolioId}/investments`;
    console.log(`Fetching investments from: ${url}`); // Log URL for debugging

    return this.http.get<Investment[]>(url).pipe(
      map(investments => {
        // Return investments if array, otherwise null (even if empty array, let component decide)
        return Array.isArray(investments) ? investments : null;
      }),
      catchError((error: HttpErrorResponse) => {
        console.error(`Error fetching investments for portfolio ${portfolioId}:`, error);
        this.snackBar.open(`Failed to load investments for the selected portfolio.`, 'Close', { duration: 3000 });
        return of(null); // Return null on error
      })
    );
  }

  /**
   * Creates a new investment within a specific portfolio.
   * @param portfolioId The ID of the portfolio to add the investment to.
   * @param investmentData The data for the new investment.
   * @returns Observable of the created Investment or null on error.
   */
  createInvestment(portfolioId: number, investmentData: any): Observable<Investment | null> {
    const url = `${this.portfolioApiUrl}/${portfolioId}/investments`;
    console.log(`Attempting to create investment at: ${url}`, investmentData);

    const backendPayload = {
      ticker: investmentData.ticker,
      type: investmentData.type,
      currency: investmentData.currency,
      amount: investmentData.amount,
      purchasePrice: investmentData.purchasePrice
    };

    return this.http.post<Investment>(url, backendPayload).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error(`Error creating investment for portfolio ${portfolioId}:`, error);
        const errorMessage = error.error?.message || error.error?.error || 'Failed to create investment.'; // Try to get backend error message
        this.snackBar.open(`Error: ${errorMessage}`, 'Close', { duration: 5000 });
        return of(null); // Return null on error
      })
    );
  }

  /**
   * Deletes an investment by its ID.
   * @param investmentId The ID of the investment to delete.
   * @returns Observable<boolean> indicating success (true) or failure (false).
   */
  deleteInvestment(investmentId: string): Observable<boolean> {
    const url = `${this.investmentsApiUrl}/${investmentId}`;
    console.log(`Attempting to delete investment at: ${url}`);

    return this.http.delete<void>(url).pipe( // Expecting no content on success (204)
      map(() => true), // Map successful deletion (void response) to true
      catchError((error: HttpErrorResponse) => {
        console.error(`Error deleting investment ${investmentId}:`, error);
        const errorMessage = error.error?.message || error.error?.error || 'Failed to delete investment.';
        this.snackBar.open(`Error: ${errorMessage}`, 'Close', { duration: 5000 });
        return of(false); // Return false on error
      })
    );
  }

  /**
   * Updates an existing investment.
   * Sends only the fields that can be updated (amount, purchasePrice).
   * @param investmentId The ID of the investment to update.
   * @param updateData An object containing the fields to update (e.g., { amount: number, purchasePrice: number }).
   * @returns Observable of the updated Investment or null on error.
   */
  updateInvestment(investmentId: string, updateData: { amount?: number, purchasePrice?: number }): Observable<Investment | null> {
    const url = `${this.investmentsApiUrl}/${investmentId}`;
    console.log(`Attempting to update investment at: ${url}`, updateData);

    // Construct payload with only the allowed updatable fields
    const backendPayload: any = {};
    if (updateData.amount !== undefined) {
      backendPayload.amount = updateData.amount;
    }
    if (updateData.purchasePrice !== undefined) {
      backendPayload.purchasePrice = updateData.purchasePrice;
    }

    return this.http.put<Investment>(url, backendPayload).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error(`Error updating investment ${investmentId}:`, error);
        const errorMessage = error.error?.message || error.error?.error || 'Failed to update investment.';
        this.snackBar.open(`Error: ${errorMessage}`, 'Close', { duration: 5000 });
        return of(null); // Return null on error
      })
    );
  }
}
