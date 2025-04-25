import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { MatSnackBar } from '@angular/material/snack-bar';

export interface InstrumentSearchResult {
  symbol: string;
  name: string;
  type: string;
  region: string;
  currency: string;
}

@Injectable({
  providedIn: 'root'
})
export class MarketDataService {

  private searchApiUrl = environment.apiUrl + '/api/search/instruments';

  constructor(private http: HttpClient, private snackBar: MatSnackBar) { }

  /**
   * Searches for instruments using the backend endpoint.
   * @param query The search keyword string.
   * @returns Observable of search results array or empty array on error/no results.
   */
  searchInstruments(query: string): Observable<InstrumentSearchResult[]> {
    if (!query || query.trim().length < 2) {
      return of([]);
    }

    const params = new HttpParams().set('query', query.trim());

    return this.http.get<InstrumentSearchResult[]>(this.searchApiUrl, { params }).pipe(
      map(results => results || []), // Ensure we always return an array
      catchError(error => {
        console.error('Error searching instruments:', error);
        this.snackBar.open('Instrument search failed. Please try again.', 'Close', { duration: 3000 });
        return of([]); // Return empty array on error
      })
    );
  }

}
