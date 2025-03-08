import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import {tap, catchError } from 'rxjs/operators';
import { AuthResponse, LoginRequest, RegisterRequest } from '../model/auth.model';
import {User} from '../model/user.model';
import {environment} from '../enviroments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private tokenExpirationTimer: any;

  constructor(private http: HttpClient) {
    this.loadStoredUser();
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials)
      .pipe(
        tap(response => this.handleAuthentication(response)),
        catchError(error => {
          console.error('Login failed', error);
          throw error;
        })
      );
  }

  register(userData: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/register`, userData)
      .pipe(
        tap(response => this.handleAuthentication(response)),
        catchError(error => {
          console.error('Registration failed', error);
          throw error;
        })
      );
  }

  logout(): void {
    localStorage.removeItem('auth_data');
    this.currentUserSubject.next(null);
    if (this.tokenExpirationTimer) {
      clearTimeout(this.tokenExpirationTimer);
    }
  }

  isAuthenticated(): boolean {
    return !!this.currentUserSubject.value;
  }

  getToken(): string | null {
    const authData = this.getAuthData();
    return authData ? authData.token : null;
  }

  private handleAuthentication(response: AuthResponse): void {
    localStorage.setItem('auth_data', JSON.stringify(response));
    this.currentUserSubject.next(response.user);

    // Set auto logout timer
    this.autoLogout(response.expiresAt - new Date().getTime());
  }

  private loadStoredUser(): void {
    const authData = this.getAuthData();
    if (authData && authData.expiresAt > new Date().getTime()) {
      this.currentUserSubject.next(authData.user);
      this.autoLogout(authData.expiresAt - new Date().getTime());
    } else if (authData) {
      this.logout(); // Token expired
    }
  }

  private autoLogout(expirationDuration: number): void {
    this.tokenExpirationTimer = setTimeout(() => {
      this.logout();
    }, expirationDuration);
  }

  private getAuthData(): AuthResponse | null {
    const authData = localStorage.getItem('auth_data');
    return authData ? JSON.parse(authData) : null;
  }
}
