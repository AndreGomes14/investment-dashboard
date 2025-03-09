// src/app/interceptors/auth.interceptor.ts
import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { TokenRefreshService } from '../services/token-refresh.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(
    private authService: AuthService,
    private tokenRefreshService: TokenRefreshService,
    private router: Router
  ) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Skip adding auth headers for auth endpoints or public endpoints
    if (this.shouldSkipAuthHeader(request)) {
      return next.handle(request);
    }

    // Add auth token to all other requests
    const token = this.authService.getToken();
    if (token) {
      request = this.addTokenHeader(request, token);
    }

    return next.handle(request).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          // Handle 401 errors - try token refresh first
          return this.handle401Error(request, next);
        }
        return throwError(() => error);
      })
    );
  }

  private addTokenHeader(request: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
    return request.clone({
      headers: request.headers.set('Authorization', `Bearer ${token}`)
    });
  }

  private shouldSkipAuthHeader(request: HttpRequest<unknown>): boolean {
    const url = request.url.toLowerCase();
    // Skip auth endpoints and public endpoints
    return url.includes('/auth/login') ||
      url.includes('/auth/register') ||
      url.includes('/api/public/');
  }

  private handle401Error(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.tokenRefreshService.refreshToken().pipe(
        switchMap(response => {
          this.isRefreshing = false;
          this.refreshTokenSubject.next(response.token);

          // Retry the original request with new token
          return next.handle(this.addTokenHeader(request, response.token));
        }),
        catchError(err => {
          this.isRefreshing = false;
          this.authService.logout();
          this.router.navigate(['/login']);
          return throwError(() => err);
        })
      );
    } else {
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => next.handle(this.addTokenHeader(request, token)))
      );
    }
  }
}
