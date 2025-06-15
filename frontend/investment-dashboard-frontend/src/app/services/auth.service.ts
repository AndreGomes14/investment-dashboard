import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { AuthResponse, LoginRequest, RegisterRequest } from '../model/auth.model';
import { User } from '../model/user.model';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // Make sure this matches the actual server path - we're removing 'api' here
  private readonly API_URL = `/api/auth`;
  private readonly TOKEN_KEY = 'auth_data';
  private readonly currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private tokenExpirationTimer: any;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadStoredUser();
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json'
      }),
      withCredentials: true
    };

    return this.http.post<any>(`${this.API_URL}/login`, credentials, httpOptions)
      .pipe(
        map(response => response.data), // Extract data from ApiResponse wrapper
        tap(response => this.handleAuthentication(response)),
        catchError(error => {
          console.error('Login failed', error);
          // Extract error message from ApiResponse wrapper if available
          const errorMessage = error.error?.message || 'Invalid username or password';
          return throwError(() => new Error(errorMessage));
        })
      );
  }

  register(userData: RegisterRequest): Observable<AuthResponse> {
    const httpOptions = {
      headers: new HttpHeaders({
        'Content-Type': 'application/json'
      }),
      withCredentials: true
    };

    return this.http.post<any>(`${this.API_URL}/register`, userData, httpOptions)
      .pipe(
        map(response => response.data), // Extract data from ApiResponse wrapper
        tap(response => this.handleAuthentication(response)),
        catchError(error => {
          console.error('Registration failed', error);
          // Extract error message from ApiResponse wrapper if available
          const errorMessage = error.error?.message || 'Registration failed';
          return throwError(() => new Error(errorMessage));
        })
      );
  }

  testConnection(): Observable<any> {
    return this.http.get(`${this.API_URL}/test`);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.currentUserSubject.next(null);

    if (this.tokenExpirationTimer) {
      clearTimeout(this.tokenExpirationTimer);
      this.tokenExpirationTimer = null;
    }

    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return !!this.getToken() && !!this.currentUserSubject.value;
  }

  getToken(): string | null {
    const authData = this.getAuthData();
    return authData ? authData.token : null;
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  private handleAuthentication(response: AuthResponse): void {
    // Calculate expiration time from expiresIn (seconds)
    const expirationDate = new Date().getTime() + response.expiresIn * 1000;

    // Store with expiration date
    const authData = {
      ...response,
      expiresAt: expirationDate
    };

    localStorage.setItem(this.TOKEN_KEY, JSON.stringify(authData));
    this.currentUserSubject.next(response.user);

    // Set auto logout timer
    this.autoLogout(expirationDate - new Date().getTime());
  }

  private loadStoredUser(): void {
    const authData = this.getAuthData();

    if (authData && authData.expiresAt) {
      const now = new Date().getTime();

      if (authData.expiresAt > now) {
        this.currentUserSubject.next(authData.user);
        this.autoLogout(authData.expiresAt - now);
      } else {
        // Token expired
        this.logout();
      }
    }
  }

  private autoLogout(expirationDuration: number): void {
    this.tokenExpirationTimer = setTimeout(() => {
      this.logout();
    }, expirationDuration);
  }

  private getAuthData(): AuthResponse & { expiresAt: number } | null {
    const authData = localStorage.getItem(this.TOKEN_KEY);
    return authData ? JSON.parse(authData) : null;
  }
}
