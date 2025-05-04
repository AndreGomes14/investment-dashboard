import { Component, OnInit, OnDestroy, NgZone, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink, ActivatedRoute, ParamMap } from '@angular/router';
import { Subscription, switchMap, of, tap, finalize } from 'rxjs';
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
export class PortfolioComponent implements OnInit, OnDestroy {
  isLoading: boolean = true;
  portfolioSummaryData: PortfolioSummaryResponse | null = null;
  allocationChartData: ChartDataPoint[] = [];
  allocationColorScheme: Color = {
    name: 'portfolioAllocation',
    selectable: true,
    group: ScaleType.Ordinal,
    domain: ['#3949ab', '#43a047', '#ff9800', '#e53935', '#5e35b1', '#039be5']
  };
  private routeSubscription: Subscription | undefined;

  get summaryMetrics(): PortfolioSummaryMetrics | null {
    return this.portfolioSummaryData?.summary ?? null;
  }
  get activeHoldings(): Investment[] | null {
    return this.portfolioSummaryData?.activeInvestments ?? null;
  }
  get soldHoldings(): Investment[] | null {
    return this.portfolioSummaryData?.soldInvestments ?? null;
  }
  get hasPortfolioData(): boolean {
    return !!this.portfolioSummaryData;
  }

  constructor(
    private readonly portfolioService: PortfolioService,
    private readonly snackBar: MatSnackBar,
    private readonly route: ActivatedRoute,
    private readonly zone: NgZone,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.routeSubscription = this.route.paramMap.pipe(
      tap(() => {
        this.isLoading = true;
        this.portfolioSummaryData = null;
      }),
      switchMap((params: ParamMap) => {
        const portfolioId = params.get('id');
        if (portfolioId) {
          console.log(`PortfolioComponent: Loading summary for specific portfolio ID: ${portfolioId}`);
          return this.portfolioService.getPortfolioSummary(Number(portfolioId));
        } else {
          console.log(`PortfolioComponent: Loading overall summary.`);
          return this.portfolioService.getOverallSummary();
        }
      }),
      finalize(() => {
        console.log("Finalizing summary load, ensuring isLoading=false within NgZone");
        this.zone.run(() => {
          this.isLoading = false;
          this.cdr.detectChanges();
        });
      })
    ).subscribe({
      next: (summaryData) => {
        this.portfolioSummaryData = summaryData;
        if (!summaryData) {
          console.log('PortfolioComponent: Received null summary data.');
          this.allocationChartData = [];
        } else {
          console.log('PortfolioComponent: Summary data loaded:', this.portfolioSummaryData);
          this.formatAllocationDataForChart();
        }
      },
      error: (error) => {
        console.error('Error loading portfolio summary data in component:', error);
        this.portfolioSummaryData = null;
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSubscription?.unsubscribe();
  }

  private formatAllocationDataForChart(): void {
    if (!this.summaryMetrics?.assetAllocationByValue) {
      this.allocationChartData = [];
      return;
    }

    this.allocationChartData = Object.entries(this.summaryMetrics.assetAllocationByValue)
      .map(([key, value]) => ({
        name: key,
        value: value
      }));
    console.log('Formatted Chart Data:', this.allocationChartData);
  }
}
