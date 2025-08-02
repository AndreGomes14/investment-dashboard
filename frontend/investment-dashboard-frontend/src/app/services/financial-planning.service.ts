import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';
import { environment } from '../../environments/environment';
import {
  FinancialProfile,
  FinancialGoal,
  CreateFinancialProfileRequest,
  UpdateFinancialProfileRequest,
  CreateFinancialGoalRequest,
  UpdateFinancialGoalRequest,
  FinancialInsights
} from '../model/financial-planning.model';

@Injectable({
  providedIn: 'root'
})
export class FinancialPlanningService {
  private readonly apiUrl = `${environment.apiUrl}/api/financial-planning`;

  constructor(
    private readonly http: HttpClient,
    private readonly snackBar: MatSnackBar
  ) { }

  // Financial Profile operations
  createOrUpdateFinancialProfile(request: CreateFinancialProfileRequest): Observable<FinancialProfile | null> {
    return this.http.post<FinancialProfile>(`${this.apiUrl}/profile`, request).pipe(
      catchError(error => {
        console.error('Error creating/updating financial profile:', error);
        this.snackBar.open('Failed to save financial profile.', 'Close', { duration: 3000 });
        return of(null);
      })
    );
  }

  updateFinancialProfile(request: UpdateFinancialProfileRequest): Observable<FinancialProfile | null> {
    return this.http.put<FinancialProfile>(`${this.apiUrl}/profile`, request).pipe(
      catchError(error => {
        console.error('Error updating financial profile:', error);
        this.snackBar.open('Failed to update financial profile.', 'Close', { duration: 3000 });
        return of(null);
      })
    );
  }

  getFinancialProfile(): Observable<FinancialProfile | null> {
    return this.http.get<FinancialProfile>(`${this.apiUrl}/profile`).pipe(
      catchError(error => {
        if (error.status === 404) {
          // Profile doesn't exist yet - this is expected for new users
          return of(null);
        }
        console.error('Error fetching financial profile:', error);
        this.snackBar.open('Failed to load financial profile.', 'Close', { duration: 3000 });
        return of(null);
      })
    );
  }

  // Financial Goals operations
  createFinancialGoal(request: CreateFinancialGoalRequest): Observable<FinancialGoal | null> {
    return this.http.post<FinancialGoal>(`${this.apiUrl}/goals`, request).pipe(
      catchError(error => {
        console.error('Error creating financial goal:', error);
        this.handleGoalError(error, 'Failed to create financial goal.');
        return of(null);
      })
    );
  }

  updateFinancialGoal(goalId: string, request: UpdateFinancialGoalRequest): Observable<FinancialGoal | null> {
    return this.http.put<FinancialGoal>(`${this.apiUrl}/goals/${goalId}`, request).pipe(
      catchError(error => {
        console.error('Error updating financial goal:', error);
        this.snackBar.open('Failed to update financial goal.', 'Close', { duration: 3000 });
        return of(null);
      })
    );
  }

  deleteFinancialGoal(goalId: string): Observable<boolean> {
    return this.http.delete<void>(`${this.apiUrl}/goals/${goalId}`).pipe(
      map(() => {
        this.snackBar.open('Goal deleted successfully.', 'Close', { duration: 3000 });
        return true;
      }),
      catchError(error => {
        console.error('Error deleting financial goal:', error);
        this.snackBar.open('Failed to delete financial goal.', 'Close', { duration: 3000 });
        return of(false);
      })
    );
  }

  getFinancialGoals(): Observable<FinancialGoal[] | null> {
    return this.http.get<FinancialGoal[]>(`${this.apiUrl}/goals`).pipe(
      catchError(error => {
        if (error.status === 404) {
          // No profile exists yet
          return of([]);
        }
        console.error('Error fetching financial goals:', error);
        this.snackBar.open('Failed to load financial goals.', 'Close', { duration: 3000 });
        return of(null);
      })
    );
  }

  getActiveFinancialGoals(): Observable<FinancialGoal[] | null> {
    return this.http.get<FinancialGoal[]>(`${this.apiUrl}/goals/active`).pipe(
      catchError(error => {
        if (error.status === 404) {
          return of([]);
        }
        console.error('Error fetching active financial goals:', error);
        this.snackBar.open('Failed to load active financial goals.', 'Close', { duration: 3000 });
        return of(null);
      })
    );
  }

  // Helper methods
  private handleGoalError(error: HttpErrorResponse, defaultMessage: string): void {
    let message = defaultMessage;
    
    if (error.error && typeof error.error === 'string') {
      if (error.error.includes('Financial profile not found')) {
        message = 'Please create your financial profile first before adding goals.';
      }
    }
    
    this.snackBar.open(message, 'Close', { duration: 4000 });
  }

  // Utility methods for calculations
  calculateSavingsRate(profile: FinancialProfile): number {
    if (!profile.monthlyIncome || profile.monthlyIncome === 0) {
      return 0;
    }
    const savings = profile.monthlyIncome - profile.monthlyExpenses;
    return (savings / profile.monthlyIncome) * 100;
  }

  calculateMonthsToTarget(goal: FinancialGoal, monthlySaving: number): number {
    if (monthlySaving <= 0 || !goal.remainingAmount) {
      return 0;
    }
    return Math.ceil(goal.remainingAmount / monthlySaving);
  }

  formatCurrency(amount: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency
    }).format(amount);
  }

  formatPercentage(value: number): string {
    return `${value.toFixed(1)}%`;
  }
}