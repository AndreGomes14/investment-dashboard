import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, shareReplay } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class CompanyNameService {
  private cache = new Map<string, Observable<string>>();

  constructor(private http: HttpClient) {}

  getName(ticker: string): Observable<string> {
    const upper = ticker.toUpperCase();
    if (this.cache.has(upper)) {
      return this.cache.get(upper)!;
    }
    const obs = this.http
      .get<any[]>(`/api/search/instruments`, {
        params: { query: upper }
      })
      .pipe(
        // Expect array of results; pick first matching symbol exactly or fallback
        shareReplay(1),
        map(results => {
          if (!Array.isArray(results) || results.length === 0) {
            return upper;
          }
          const exact = results.find(r => r.symbol?.toUpperCase() === upper);
          return (exact?.name as string) || (results[0].name as string) || upper;
        })
      );
    this.cache.set(upper, obs);
    return obs;
  }
} 