import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PreferenceService {
  private preferredCurrency$ = new BehaviorSubject<string>(localStorage.getItem('preferredCurrency') || 'USD');
  private darkMode$ = new BehaviorSubject<boolean>(localStorage.getItem('darkMode') === 'true');

  private currencyApiUrl = environment.apiUrl + '/api/user/settings/preferred-currency';
  private darkModeApiUrl = environment.apiUrl + '/api/user/settings/dark-mode';

  constructor(private http: HttpClient) {
    // Apply initial dark mode on startup
    this.darkMode$.subscribe(isDark => {
      if (isDark) {
        document.body.classList.add('dark-theme');
      } else {
        document.body.classList.remove('dark-theme');
      }
    });
  }

  get currencyChanges() {
    return this.preferredCurrency$.asObservable();
  }

  get currentCurrency(): string {
    return this.preferredCurrency$.value;
  }

  changeCurrency(cur: string) {
    const upper = cur.toUpperCase();
    localStorage.setItem('preferredCurrency', upper);
    this.preferredCurrency$.next(upper);
    this.http.put<void>(this.currencyApiUrl, { currency: upper }).subscribe();
  }

  // --- Dark mode helpers --------------------------------------------------

  get darkModeChanges() {
    return this.darkMode$.asObservable();
  }

  get isDarkMode(): boolean {
    return this.darkMode$.value;
  }

  setDarkMode(enabled: boolean) {
    localStorage.setItem('darkMode', String(enabled));
    this.darkMode$.next(enabled);
    this.http.put<void>(this.darkModeApiUrl, { darkMode: enabled }).subscribe();
  }
} 