import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-portfolio',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatGridListModule, MatIconModule],
  template: `
    <div class="portfolio-container">
      <h1>Portfolio Overview</h1>

      <mat-grid-list cols="2" rowHeight="350px" gutterSize="20px">
        <mat-grid-tile>
          <mat-card class="dashboard-card">
            <mat-card-header>
              <mat-card-title>Total Portfolio Value</mat-card-title>
              <mat-card-subtitle>Current Status</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="value-container">
                <span class="total-value">$250,000</span>
                <span class="change positive">
                  <mat-icon>trending_up</mat-icon>
                  +2.5%
                </span>
              </div>
            </mat-card-content>
          </mat-card>
        </mat-grid-tile>

        <mat-grid-tile>
          <mat-card class="dashboard-card">
            <mat-card-header>
              <mat-card-title>Asset Allocation</mat-card-title>
              <mat-card-subtitle>Current Distribution</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <!-- Placeholder for chart -->
              <div class="chart-placeholder">
                Asset allocation chart will be displayed here
              </div>
            </mat-card-content>
          </mat-card>
        </mat-grid-tile>

        <mat-grid-tile>
          <mat-card class="dashboard-card">
            <mat-card-header>
              <mat-card-title>Top Holdings</mat-card-title>
              <mat-card-subtitle>Best Performing Assets</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="holdings-list">
                <div class="holding-item">
                  <span>AAPL</span>
                  <span class="positive">+3.2%</span>
                </div>
                <div class="holding-item">
                  <span>MSFT</span>
                  <span class="positive">+2.8%</span>
                </div>
                <div class="holding-item">
                  <span>GOOGL</span>
                  <span class="positive">+2.1%</span>
                </div>
              </div>
            </mat-card-content>
          </mat-card>
        </mat-grid-tile>

        <mat-grid-tile>
          <mat-card class="dashboard-card">
            <mat-card-header>
              <mat-card-title>Recent Activity</mat-card-title>
              <mat-card-subtitle>Latest Transactions</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              <div class="activity-list">
                <div class="activity-item">
                  <span>Bought AAPL</span>
                  <span class="date">Mar 14, 2024</span>
                </div>
                <div class="activity-item">
                  <span>Sold TSLA</span>
                  <span class="date">Mar 13, 2024</span>
                </div>
                <div class="activity-item">
                  <span>Dividend Received</span>
                  <span class="date">Mar 12, 2024</span>
                </div>
              </div>
            </mat-card-content>
          </mat-card>
        </mat-grid-tile>
      </mat-grid-list>
    </div>
  `,
  styles: [`
    .portfolio-container {
      padding: 20px;
    }

    h1 {
      margin-bottom: 30px;
      color: #2c3e50;
    }

    .dashboard-card {
      width: 100%;
      height: 100%;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    .value-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 200px;
    }

    .total-value {
      font-size: 3em;
      font-weight: bold;
      color: #2c3e50;
    }

    .change {
      display: flex;
      align-items: center;
      margin-top: 10px;
    }

    .positive {
      color: #27ae60;
    }

    .negative {
      color: #e74c3c;
    }

    .chart-placeholder {
      height: 200px;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f8f9fa;
      border-radius: 4px;
    }

    .holdings-list, .activity-list {
      margin-top: 20px;
    }

    .holding-item, .activity-item {
      display: flex;
      justify-content: space-between;
      padding: 10px 0;
      border-bottom: 1px solid #eee;
    }

    .date {
      color: #7f8c8d;
      font-size: 0.9em;
    }

    mat-card-header {
      margin-bottom: 20px;
    }

    mat-card-title {
      font-size: 1.2em;
      color: #2c3e50;
    }

    mat-card-subtitle {
      color: #7f8c8d;
    }
  `]
})
export class PortfolioComponent {}
