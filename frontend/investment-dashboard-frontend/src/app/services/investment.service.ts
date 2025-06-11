import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment'; // Ensure correct path
import { Investment } from '../model/investment.model'; // Assuming model exists
import { HttpErrorResponse as ApiErrorResponse } from '@angular/common/http'; // Import HttpErrorResponse
import { MatSnackBar } from '@angular/material/snack-bar'; // Import MatSnackBar
import { UpdateInvestmentRequest } from '../model/investment.model';
import { CreateInvestmentRequest } from '../model/investment.model';
import { SellInvestmentRequest } from '../model/investment.model';

@Injectable({
  providedIn: 'root'
})
export class InvestmentService {
  private apiUrl = `${environment.apiUrl}/api/investments`;
  private portfolioApiUrl = `${environment.apiUrl}/api/portfolios`;

  constructor(private readonly http: HttpClient, private readonly snackBar: MatSnackBar) { } // Inject MatSnackBar

  /**
   * Fetches all investments (potentially across all user portfolios).
   * Consider if this method is still needed or if fetching should always be per-portfolio.
   * @returns Observable of Investment array or null on error/no data
   */
  getAllInvestments(): Observable<Investment[] | null> {
    return this.http.get<Investment[]>(this.apiUrl).pipe(
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
    const url = `${this.portfolioApiUrl}/${portfolioId}/investments`;
    console.log(`Fetching investments from: ${url}`);
    return this.http.get<Investment[]>(url).pipe(
      catchError(error => {
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
    return this.http.post<Investment>(url, investmentData).pipe(
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
    const url = `${this.apiUrl}/${investmentId}`;
    console.log(`Attempting to delete (mark as deleted) investment at: ${url}`);
    return this.http.delete<void>(url, { observe: 'response' }) // Observe the full response
      .pipe(
        map(response => response.status === 200 || response.status === 204), // Check for OK or No Content
        catchError(error => {
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
    const url = `${this.apiUrl}/${investmentId}`;
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

  // Sell an investment (update status to SOLD)
  sellInvestment(investmentId: string, sellData: { sellPrice: number }): Observable<Investment | null> {
    const url = `${this.apiUrl}/${investmentId}/sell`;
    console.log(`Attempting to sell investment at: ${url} with data:`, sellData);
    // PATCH request with sellData in the body
    return this.http.patch<Investment>(url, sellData).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error(`Error selling investment ${investmentId}:`, error);
        const errorMessage = error.error?.message || error.error?.error || 'Failed to mark investment as SOLD.';
        this.snackBar.open(`Error: ${errorMessage}`, 'Close', { duration: 5000 });
        return of(null);
      })
    );
  }

  manuallyUpdateInvestmentCurrentValue(investmentId: number, newValue: number): Observable<Investment> {
    const payload = { currentValue: newValue };
    return this.http.put<Investment>(`${this.apiUrl}/${investmentId}/current-value`, payload)
      .pipe(catchError(this.handleError));
  }

  private handleError(error: HttpErrorResponse) {
    if (error.error instanceof ErrorEvent) {
      // A client-side or network error occurred.
      console.error('An error occurred:', error.error.message);
    } else {
      // The backend returned an unsuccessful response code.
      console.error(
        `Backend returned code ${error.status}, ` +
        `body was: ${JSON.stringify(error.error)}`);
    }
    // Return an observable with a user-facing error message.
    // Modify this to match how you generally handle errors and what your components expect (e.g., rethrow or return a specific error object).
    return throwError(() => new Error('Something bad happened; please try again later. Review console for details.'));
  }

  getInvestmentsForPortfolio(portfolioId: string): Observable<Investment[]> {
    return this.http.get<Investment[]>(`${this.portfolioApiUrl}/${portfolioId}/investments`);
  }

  /**
   * Fetches a single investment by its ID.
   * @param investmentId The ID of the investment.
   * @returns Observable of the Investment or null on error.
   */
  getInvestmentById(investmentId: string): Observable<Investment | null> {
    const url = `${this.apiUrl}/${investmentId}`;
    return this.http.get<Investment>(url).pipe(
      catchError(error => {
        console.error(`Error fetching investment ${investmentId}:`, error);
        this.snackBar.open(`Failed to load investment details.`, 'Close', { duration: 3000 });
        return of(null);
      })
    );
  }
}
