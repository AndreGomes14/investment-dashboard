import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink, ActivatedRoute, ParamMap } from '@angular/router';
import { Observable, switchMap, of, tap, map } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NgxChartsModule, Color, ScaleType } from '@swimlane/ngx-charts';

import { PortfolioService, PortfolioSummaryResponse, PortfolioSummaryMetrics, InvestmentPerformance } from '../../services/portfolio.service';
import { Investment } from '../../model/investment.model';

interface ChartDataPoint {
  name: string;
  value: number;
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
    RouterLink,
    NgxChartsModule
  ],
  templateUrl: './portfolio.component.html',
  styleUrls: ['./portfolio.component.css']
})
export class PortfolioComponent implements OnInit {
  summaryData$: Observable<PortfolioSummaryResponse | null> = of(null);
  allocationChartData$: Observable<ChartDataPoint[]> = of([]);
  allocationColorScheme: Color = {
    name: 'portfolioAllocation',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#3949ab', '#43a047', '#ff9800', '#e53935', '#5e35b1', '#039be5']
  };

  constructor(
    private readonly portfolioService: PortfolioService,
    private readonly snackBar: MatSnackBar,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.summaryData$ = this.route.paramMap.pipe(
      tap(() => console.log("Route params changed, fetching summary...")),
      switchMap((params: ParamMap) => {
        const portfolioId = params.get('id');
        if (portfolioId) {
          console.log(`PortfolioComponent: Loading summary for specific portfolio ID: ${portfolioId}`);
          const numericId = Number(portfolioId);
          if (isNaN(numericId)) {
            console.error("Invalid portfolio ID in route parameter.");
            return of(null);
          }
          return this.portfolioService.getPortfolioSummary(numericId);
        } else {
          console.log(`PortfolioComponent: Loading overall summary.`);
          return this.portfolioService.getOverallSummary();
        }
      }),
      tap(summaryData => {
        if (summaryData) {
          console.log('PortfolioComponent: Summary data received:', summaryData);
        } else {
          console.log('PortfolioComponent: Received null summary data from service.');
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
  }
}
