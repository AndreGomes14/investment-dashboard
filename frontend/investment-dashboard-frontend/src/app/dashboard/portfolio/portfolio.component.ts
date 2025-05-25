import { Component, OnInit, HostListener } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { Observable, switchMap, of, tap, map, BehaviorSubject, combineLatest, startWith, catchError } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NgxChartsModule, Color, ScaleType } from '@swimlane/ngx-charts';

import { PortfolioService, PortfolioSummaryResponse, PortfolioSummaryMetrics, InvestmentPerformance, HistoricalDataPoint } from '../../services/portfolio.service';

interface ChartDataPoint {
  name: string;
  value: number;
}

// For ngx-charts line chart
export interface LineChartDataPoint {
  name: Date; // Typically timestamp (can be string for display)
  value: number;
}
export interface LineChartSeries {
  name: string; // Series name, e.g., "Total Portfolio Value"
  series: LineChartDataPoint[];
}

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatButtonToggleModule,
    RouterLink,
    NgxChartsModule
  ],
  providers: [DatePipe],
  templateUrl: './portfolio.component.html',
  styleUrls: ['./portfolio.component.css']
})
export class PortfolioComponent implements OnInit {
  summaryData$: Observable<PortfolioSummaryResponse | null> = of(null);
  private previousSummaryMetrics: PortfolioSummaryMetrics | null = null;

  // Properties for animated display values
  displayTotalValue: number = 0;
  displayUnrealizedPnlAbsolute: number = 0;
  // Add more for other stats if needed, e.g., displayRealizedPnlAbsolute

  allocationChartData$: Observable<ChartDataPoint[]> = of([]);
  allocationColorScheme: Color = {
    name: 'portfolioAllocation',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#3949ab', '#43a047', '#ff9800', '#e53935', '#5e35b1', '#039be5']
  };
  isLoadingSummary = true;
  isRefreshingValues = false;

  // For Historical Chart
  historicalData$: Observable<LineChartSeries[] | null> = of(null);
  selectedTimeRange$ = new BehaviorSubject<string>('1m'); // Default to 1 month
  isLoadingHistory = true;
  historyChartColorScheme: Color = {
    name: 'portfolioHistory',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#007bff'] // Changed to blue color
  };
  isHistoryChartFullscreen = false;

  _windowHeight: number = window.innerHeight;

  @HostListener('window:resize', ['$event'])
  onResize(event?: Event) {
    this._windowHeight = window.innerHeight;
  }

  get windowHeight(): number {
    return this._windowHeight;
  }

  portfolioId: number | null = null;
  private refreshTrigger$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly portfolioService: PortfolioService,
    private readonly snackBar: MatSnackBar,
    private readonly route: ActivatedRoute,
    private readonly datePipe: DatePipe
  ) {}

  ngOnInit(): void {
    const paramMap$ = this.route.paramMap.pipe(
      tap(params => {
        const id = params.get('id');
        this.portfolioId = id ? Number(id) : null;
        if (id && isNaN(this.portfolioId as any)) {
            console.error("Invalid portfolio ID in route parameter.");
            this.portfolioId = null;
        }
      })
    );

    this.summaryData$ = combineLatest([paramMap$, this.refreshTrigger$]).pipe(
      tap(() => {
        this.isLoadingSummary = true;
        console.log("Route params changed or refresh triggered, fetching summary...")
      }),
      switchMap(([params, _]) => { // We only need params from paramMap$
        // portfolioId is now set in the first tap
        if (this.portfolioId !== null) {
          console.log(`PortfolioComponent: Loading summary for specific portfolio ID: ${this.portfolioId}`);
          return this.portfolioService.getPortfolioSummary(this.portfolioId);
        } else {
          console.log(`PortfolioComponent: Loading overall summary.`);
          return this.portfolioService.getOverallSummary();
        }
      }),
      tap(summaryData => {
        this.isLoadingSummary = false;
        if (summaryData && summaryData.summary) {
          console.log('PortfolioComponent: Summary data received:', summaryData);
          this.updateAnimatedStatistics(summaryData.summary);
          this.previousSummaryMetrics = { ...summaryData.summary }; // Store a copy
        } else {
          console.log('PortfolioComponent: Received null or incomplete summary data from service.');
          // Reset display values if data is invalid or gone
          this.displayTotalValue = 0;
          this.displayUnrealizedPnlAbsolute = 0;
          this.previousSummaryMetrics = null;
        }
      })
    );

    this.allocationChartData$ = this.summaryData$.pipe(
      map(summaryData => {
        if (!summaryData?.summary?.assetAllocationByValue) {
          return [];
        }
        return Object.entries(summaryData.summary.assetAllocationByValue)
          .map(([key, value]) => ({ name: key, value: value }));
      })
    );

    // Initialize historical data fetching
    this.historicalData$ = this.selectedTimeRange$.pipe(
      tap((range) => {
        console.log('[HistChart] Time range changed, will fetch for:', range);
        // Defer the update to isLoadingHistory to the next microtask
        Promise.resolve().then(() => {
            this.isLoadingHistory = true;
            // If OnPush strategy is used and this doesn't work, cdr.detectChanges() might be needed here.
        });
      }),
      switchMap(range => 
        this.portfolioService.getOverallUserValueHistory(range).pipe(
          tap(rawData => console.log('[HistChart] Raw data from service:', JSON.stringify(rawData))),
          catchError(err => {
            console.error('[HistChart] Error fetching historical data from service:', err);
            // Defer this as well for consistency, though it might be less critical here
            Promise.resolve().then(() => this.isLoadingHistory = false);
            return of([]); 
          })
        )
      ),
      map(dataPoints => {
        this.isLoadingHistory = false; 
        console.log('[HistChart] Processing dataPoints in map:', dataPoints);
        if (!dataPoints || dataPoints.length === 0) {
          console.log('[HistChart] No dataPoints or empty array (possibly due to error or no data), returning [].');
          return []; 
        }
        // Transform to ngx-charts format
        let chartSeries: LineChartDataPoint[] = [];
        try {
          chartSeries = dataPoints.map(dp => {
            const timestampDate = new Date(dp.timestamp); // Attempt to parse the timestamp
            const pointValue = Number(dp.value); // Ensure value is a number

            if (isNaN(timestampDate.getTime())) {
              console.warn('[HistChart] Invalid date for timestamp:', dp.timestamp, '- For data point:', dp);
              // Skip this point or return a structure that can be filtered out later, 
              // or use a placeholder if that makes sense for your chart.
              // For now, we'll effectively skip it by not erroring but it might lead to a gap or misrepresentation.
              // A better approach might be to filter out invalid points before this map.
              return null; // This will be filtered out later
            }
            if (isNaN(pointValue)) {
              console.warn('[HistChart] Invalid value (NaN) for data point:', dp);
              return null; // Filter out
            }

            return {
              name: timestampDate, // Use the Date object directly
              value: pointValue
            };
          }).filter(point => point !== null) as LineChartDataPoint[]; // Filter out any nulls from parsing errors

          // Sort the series by date to ensure the line chart displays correctly
          chartSeries.sort((a, b) => (a.name as Date).getTime() - (b.name as Date).getTime());

        } catch (e) {
          console.error('[HistChart] Error during dataPoints.map transformation:', e);
          this.isLoadingHistory = false; // Ensure loading stops if transformation fails
          return []; // Return empty on error
        }
        
        console.log('[HistChart] Transformed chartSeries (using Date objects for name):', JSON.stringify(chartSeries.map(s => ({...s, name: (s.name as Date).toISOString()})))); // Log ISO string for readability
        const finalChartData = [{ name: 'Total Portfolio Value', series: chartSeries }];
        console.log('[HistChart] Final data for chart (series with Date objects):', finalChartData); // Not easily stringifiable with raw dates, but structure is key
        return finalChartData;
      }),
      startWith([]) 
    );
  }

  // Method to change time range for historical chart
  setTimeRange(range: string): void {
    this.selectedTimeRange$.next(range);
  }

  // Custom X-axis tick formatting for dates
  formatXAxisTick = (value: any): string => {
    if (value instanceof Date) {
      return this.datePipe.transform(value, 'MMM d, yy') || ''; // Format: e.g., "Sep 15, 23"
    }
    // Fallback for unexpected value types, common when chart is initializing
    try {
      const date = new Date(value);
      // Check if it's a valid date after parsing
      if (!isNaN(date.getTime())) {
        return this.datePipe.transform(date, 'MMM d, yy') || '';
      }
    } catch (e) {
      // If parsing fails, or it's not a Date object, return the original value as string
    }
    return value ? value.toString() : '';
  };

  private animateValue(start: number, end: number, duration: number, updater: (value: number) => void): void {
    if (start === end) {
      updater(end);
      return;
    }
    const range = end - start;
    let current = start;
    const increment = end > start ? 1 : -1;
    const stepTime = Math.abs(Math.floor(duration / range));

    // If range is too large or stepTime is 0, do it faster or directly set
    if (range === 0 || stepTime === 0 || Math.abs(range) > 500000) { // Heuristic to prevent overly long animations for huge changes
        updater(end);
        return;
    }

    const timer = setInterval(() => {
      current += increment * Math.ceil(Math.abs(range / (duration/50))); // Adjust step for smoother/faster animation over duration
      if ((increment === 1 && current >= end) || (increment === -1 && current <= end)) {
        current = end;
        updater(current);
        clearInterval(timer);
      } else {
        updater(current);
      }
    }, 50); // Update interval
  }

  private updateAnimatedStatistics(newMetrics: PortfolioSummaryMetrics): void {
    const animationDuration = 750; // ms

    const prevTotalValue = this.previousSummaryMetrics ? this.previousSummaryMetrics.totalValue : newMetrics.totalValue;
    this.animateValue(prevTotalValue, newMetrics.totalValue, animationDuration, (val) => this.displayTotalValue = val);

    const prevUnrealizedPnl = this.previousSummaryMetrics ? this.previousSummaryMetrics.unrealizedPnlAbsolute : newMetrics.unrealizedPnlAbsolute;
    this.animateValue(prevUnrealizedPnl, newMetrics.unrealizedPnlAbsolute, animationDuration, (val) => this.displayUnrealizedPnlAbsolute = val);

  }

  toggleHistoryChartFullscreen(): void {
    this.isHistoryChartFullscreen = !this.isHistoryChartFullscreen;
    if (this.isHistoryChartFullscreen) {
      document.body.classList.add('chart-fullscreen-active');
      setTimeout(() => window.dispatchEvent(new Event('resize')), 300);
    } else {
      document.body.classList.remove('chart-fullscreen-active');
      // Optionally, dispatch resize again if needed when exiting fullscreen
      // setTimeout(() => window.dispatchEvent(new Event('resize')), 300);
    }
  }

  refreshPortfolioValues(): void {
    this.isRefreshingValues = true;
    let refreshObservable$: Observable<any>;
    let successMessage = '';
    let operationCompleted = false; // Flag to ensure refreshTrigger is called once

    if (this.portfolioId !== null) {
      refreshObservable$ = this.portfolioService.updatePortfolioValues(this.portfolioId);
      successMessage = 'Portfolio values updated successfully!';
    } else {
      refreshObservable$ = this.portfolioService.refreshAllUserPortfolioValues();
      successMessage = 'All your active investment values have been updated!';
    }

    refreshObservable$.subscribe({
      next: (response) => {
        operationCompleted = true;
        // For portfolio-specific updates, response is Portfolio | null.
        // For all-user updates, response is void | null (mapped to by the service).
        if (this.portfolioId !== null) { // Specific portfolio
          if (response) {
            this.snackBar.open(successMessage, 'Close', { duration: 3000 });
          } else {
            // response is null, but not an error - could be a 204 No Content or handled error in service
            this.snackBar.open('Portfolio values refreshed. No specific data returned.', 'Close', { duration: 3000 });
          }
        } else { // All user portfolios
          // response is void | null. If not null, it means success (even if void).
          // If null, it means the service might have handled an error and returned of(null).
          if (response !== null) { // Typically void, so this means no error was thrown to this level
             this.snackBar.open(successMessage, 'Close', { duration: 3000 });
          } else {
             this.snackBar.open('All investment values refreshed. Operation completed.', 'Close', { duration: 3000 });
             console.log('Refresh operation for all user values returned null, error likely handled in service or was a void success.');
          }
        }
      },
      error: (err) => {
        this.isRefreshingValues = false;
        operationCompleted = true; // Operation ended, albeit with an error
        console.error('Critical error during refreshPortfolioValues observable:', err);
        this.snackBar.open('An unexpected error occurred during the refresh.', 'Close', { duration: 3000 });
        this.refreshTrigger$.next(); // Still trigger a refresh, maybe some values changed partially or to show current state
      },
      complete: () => {
        this.isRefreshingValues = false;
        if (operationCompleted) { // Ensure 'next' or 'error' was called
            this.refreshTrigger$.next();
        }
        // If neither next nor error was called but observable completed (unlikely for typical HTTP calls),
        // we might still want to trigger:
        // if (!operationCompleted) { this.refreshTrigger$.next(); }
      }
    });
  }
}
