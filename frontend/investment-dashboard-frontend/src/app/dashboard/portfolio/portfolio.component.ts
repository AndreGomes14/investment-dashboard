import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { forkJoin } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { MatSnackBar } from '@angular/material/snack-bar';

import { PortfolioService } from '../../services/portfolio.service';
import { InvestmentService } from '../../services/investment.service';
import { Portfolio } from '../../model/portfolio.model';
import { Investment } from '../../model/investment.model';
import {RouterLink} from '@angular/router';

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
    RouterLink
  ],
  templateUrl: './portfolio.component.html',
  styleUrls: ['./portfolio.component.css']
})
export class PortfolioComponent implements OnInit {
  isLoading: boolean = true;
  hasPortfolioData: boolean = false;
  portfolios: Portfolio[] | null = null;
  investments: Investment[] | null = null;

  totalPortfolioValue: number = 0;
  totalGainLoss: number = 0;
  totalGainLossPercentage: number = 0;

  constructor(
    private readonly portfolioService: PortfolioService,
    private readonly investmentService: InvestmentService,
    private readonly snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(showLoading: boolean = true): void {
    if (showLoading) {
      this.isLoading = true;
    }
    // Reset metrics
    this.resetMetrics();

    forkJoin({
      portfolios: this.portfolioService.getUserPortfolios(),
      investments: this.investmentService.getAllInvestments()
    }).pipe(
      finalize(() => { if (showLoading) { this.isLoading = false; } })
    ).subscribe({
      next: (results) => {
        this.portfolios = results.portfolios;
        this.investments = results.investments;
        this.hasPortfolioData = !!this.portfolios && this.portfolios.length > 0;

        if (this.hasPortfolioData && this.investments) {
          console.log('Portfolio data loaded:', this.portfolios);
          console.log('Investments loaded:', this.investments);
          // Calculate metrics only if we have investments
          this.calculateMetrics();
        } else {
          console.log('No portfolio or no investments found.');
        }
      },
      error: (error) => {
        console.error('Error loading portfolio overview data:', error);
        this.snackBar.open('Failed to load portfolio data. Please try again later.', 'Close', { duration: 3000 });
        this.hasPortfolioData = false;
        this.portfolios = null;
        this.investments = null;
        this.resetMetrics();
      }
    });
  }

  resetMetrics(): void {
    this.totalPortfolioValue = 0;
    this.totalGainLoss = 0;
    this.totalGainLossPercentage = 0;
  }

  calculateMetrics(): void {
    if (!this.investments || this.investments.length === 0) {
      this.resetMetrics();
      return;
    }

    let totalInitialValue = 0;
    let currentTotalValue = 0;

    for (const investment of this.investments) {
      // Ensure values are numbers
      const purchasePrice = Number(investment.purchasePrice) || 0;
      const amount = Number(investment.amount) || 0;
      const currentValuePerUnit = Number(investment.currentValue) || purchasePrice; // Use purchase price if current value missing

      const initialValue = purchasePrice * amount;
      const currentValueTotal = currentValuePerUnit * amount;

      totalInitialValue += initialValue;
      currentTotalValue += currentValueTotal;
    }

    this.totalPortfolioValue = currentTotalValue;
    this.totalGainLoss = currentTotalValue - totalInitialValue;

    if (totalInitialValue > 0) {
      this.totalGainLossPercentage = (this.totalGainLoss / totalInitialValue) * 100;
    } else {
      this.totalGainLossPercentage = 0;
    }

    console.log('Calculated Metrics:', {
      totalValue: this.totalPortfolioValue,
      gainLoss: this.totalGainLoss,
      gainLossPercent: this.totalGainLossPercentage
    });
  }
}
