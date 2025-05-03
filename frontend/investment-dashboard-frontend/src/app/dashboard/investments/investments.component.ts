import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { finalize } from 'rxjs/operators';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { HttpClient } from '@angular/common/http';
import { saveAs } from 'file-saver';

import { PortfolioService } from '../../services/portfolio.service';
import { InvestmentService } from '../../services/investment.service';
import { Portfolio } from '../../model/portfolio.model';
import { Investment } from '../../model/investment.model';
import {CreatePortfolioDialogComponent} from '../portfolio/dialog/create-portfolio-dialog.component';
import {EditPortfolioDialogComponent} from '../portfolio/dialog/edit-portfolio-dialog.component';
import {ConfirmDeleteDialogComponent} from '../portfolio/dialog/confirm-delete-dialog.component';
import {AddInvestmentDialogComponent} from './dialog/add-investment-dialog.component';
import {EditInvestmentDialogComponent, EditInvestmentDialogResult} from './dialog/edit-investment-dialog.component';

// Define the interface for aggregated data
interface AggregatedInvestment {
  ticker: string;
  type: string;
  currency: string;
  totalAmount: number;
  averagePurchasePrice: number;
  totalPurchaseCost: number;
  totalCurrentValue: number | null;
  percentProfit: number | null;
  individualInvestments: Investment[];
}

@Component({
  selector: 'app-investments',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatDividerModule,
    MatFormFieldModule,
    MatSelectModule,
    MatExpansionModule
  ],
  templateUrl: './investments.component.html',
  styleUrls: ['./investments.component.css']
})
export class InvestmentsComponent implements OnInit {
  isLoading: boolean = true;
  isCreatingPortfolio: boolean = false;
  isUpdatingPortfolio: boolean = false;
  isDeletingPortfolio: boolean = false;
  isLoadingInvestments: boolean = false;
  hasPortfolios: boolean = false;
  portfolios: Portfolio[] | null = null;
  selectedPortfolioId: number | null = null;
  investmentsForSelectedPortfolio: Investment[] | null = null;
  aggregatedInvestments: AggregatedInvestment[] | null = null;

  get selectedPortfolioName(): string {
    if (!this.selectedPortfolioId || !this.portfolios) {
      return 'Selected Portfolio'; // Default text
    }
    const selectedPortfolio = this.portfolios.find(p => p.id === this.selectedPortfolioId);
    return selectedPortfolio ? selectedPortfolio.name : 'Selected Portfolio'; // Return name or default
  }

  constructor(
    private readonly portfolioService: PortfolioService,
    private readonly investmentService: InvestmentService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar,
    private readonly http: HttpClient
  ) {}

  ngOnInit(): void {
    this.checkPortfolios();
  }

  checkPortfolios(showLoading: boolean = true): void {
    if (showLoading) {
      this.isLoading = true;
    }
    this.selectedPortfolioId = null;
    this.investmentsForSelectedPortfolio = null;
    this.aggregatedInvestments = null;
    this.portfolioService.getUserPortfolios()
      .pipe(
        finalize(() => { if (showLoading) { this.isLoading = false; } })
      )
      .subscribe({
        next: (portfolios) => {
          this.portfolios = portfolios;
          this.hasPortfolios = !!(this.portfolios && this.portfolios.length > 0);
          if (this.hasPortfolios && this.portfolios) {
            if (this.portfolios.length === 1 && this.portfolios[0].id) {
              this.selectedPortfolioId = this.portfolios[0].id;
              this.loadInvestmentsForSelectedPortfolio();
            }
          } else {
            this.selectedPortfolioId = null;
          }
        },
        error: (error) => {
          this.hasPortfolios = false;
          this.selectedPortfolioId = null;
        }
      });
  }

  onPortfolioSelected(): void {
    console.log('Portfolio selected:', this.selectedPortfolioId);
    this.investmentsForSelectedPortfolio = null;
    if (this.selectedPortfolioId) {
      this.loadInvestmentsForSelectedPortfolio();
    }
  }

  loadInvestmentsForSelectedPortfolio(): void {
    if (!this.selectedPortfolioId) return;

    this.isLoadingInvestments = true;
    this.investmentsForSelectedPortfolio = null;
    this.aggregatedInvestments = null;

    this.investmentService.getInvestmentsByPortfolioId(this.selectedPortfolioId)
      .pipe(finalize(() => this.isLoadingInvestments = false))
      .subscribe(investments => {
        // Add percentProfit dynamically to each investment object
        if (investments) {
          investments.forEach((inv: any) => {
            const totalPurchaseCost = (inv.amount ?? 0) * (inv.purchasePrice ?? 0);
            const totalCurrentValue = inv.currentValue !== null ? (inv.amount ?? 0) * inv.currentValue : null;

            if (totalCurrentValue !== null && totalPurchaseCost > 0) {
              inv.percentProfit = (totalCurrentValue - totalPurchaseCost) / totalPurchaseCost;
            } else if (totalCurrentValue !== null && totalPurchaseCost === 0 && totalCurrentValue > 0) {
              inv.percentProfit = Infinity;
            } else if (totalCurrentValue !== null && totalPurchaseCost === 0 && totalCurrentValue === 0) {
              inv.percentProfit = 0;
            } else {
              inv.percentProfit = null;
            }
          });
        }

        this.investmentsForSelectedPortfolio = investments;
        if (investments && investments.length > 0) {
          console.log(`Loaded ${investments.length} individual investments for portfolio ${this.selectedPortfolioId}. Aggregating...`);
          this.aggregateInvestments(investments);
          console.log(`Aggregated into ${this.aggregatedInvestments?.length} positions.`);
        } else if (investments) {
          console.log(`No investments exist for portfolio ${this.selectedPortfolioId}`);
          this.aggregatedInvestments = [];
        } else {
          console.log(`Failed to load investments for portfolio ${this.selectedPortfolioId}`);
        }
      });
  }

  private aggregateInvestments(investments: Investment[]): void {
    const groups = new Map<string, AggregatedInvestment>();

    for (const investment of investments) {
      if (!investment.ticker) continue;

      const key = (investment.ticker || '').trim().toUpperCase();
      let group = groups.get(key);

      if (!group) {
        group = {
          ticker: investment.ticker,
          type: investment.type || 'N/A',
          currency: investment.currency || 'N/A',
          totalAmount: 0,
          averagePurchasePrice: 0,
          totalPurchaseCost: 0,
          totalCurrentValue: 0,
          percentProfit: null,
          individualInvestments: []
        };
        groups.set(key, group);
      }

      const purchaseCost = (investment.amount ?? 0) * (investment.purchasePrice ?? 0);
      group.totalAmount += (investment.amount ?? 0);
      group.totalPurchaseCost += purchaseCost;

      if (investment.currentValue != null) {
        const currentVal = (investment.amount ?? 0) * investment.currentValue;
        group.totalCurrentValue = (group.totalCurrentValue ?? 0) + currentVal;
      } else {
        group.totalCurrentValue = null;
      }

      group.individualInvestments.push(investment);
    }

    this.aggregatedInvestments = Array.from(groups.values()).map(group => {
      if (group.totalAmount > 0) {
        group.averagePurchasePrice = group.totalPurchaseCost / group.totalAmount;
      } else {
        group.averagePurchasePrice = 0;
      }
      return group;
    });

    // Calculate percentProfit after average price
    this.aggregatedInvestments.forEach(group => {
      if (group.totalCurrentValue !== null && group.totalPurchaseCost > 0) {
        group.percentProfit = ((group.totalCurrentValue - group.totalPurchaseCost) / group.totalPurchaseCost);
      } else if (group.totalCurrentValue !== null && group.totalPurchaseCost === 0 && group.totalCurrentValue > 0) {
        // Handle case where cost is zero but value is positive (e.g., free shares)
        group.percentProfit = Infinity;
      } else if (group.totalCurrentValue !== null && group.totalPurchaseCost === 0 && group.totalCurrentValue === 0) {
        group.percentProfit = 0; // Cost and value are zero
      } else {
        // Cannot calculate if currentValue is null or if cost is negative/invalid (though cost shouldn't be negative here)
        group.percentProfit = null;
      }
    });

    this.aggregatedInvestments.sort((a, b) => a.ticker.localeCompare(b.ticker));
  }

  createPortfolio() {
    const dialogRef = this.dialog.open(CreatePortfolioDialogComponent, {
      width: '400px',
      data: {}
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.isCreatingPortfolio = true;
        this.portfolioService.createPortfolio(result)
          .pipe(finalize(() => this.isCreatingPortfolio = false))
          .subscribe(createdPortfolio => {
            if (createdPortfolio && createdPortfolio.id) {
              this.snackBar.open(`Portfolio '${createdPortfolio.name}' created successfully!`, 'Close', { duration: 3000 });
              this.portfolios = this.portfolios ? [...this.portfolios, createdPortfolio] : [createdPortfolio];
              this.hasPortfolios = true;
              this.selectedPortfolioId = createdPortfolio.id;
              this.onPortfolioSelected();
            }
          });
      }
    });
  }

  editPortfolio() {
    if (!this.selectedPortfolioId || !this.portfolios) {
      this.snackBar.open('Please select a portfolio to edit.', 'Close', { duration: 3000 });
      return;
    }
    const selectedPortfolio = this.portfolios.find(p => p.id === this.selectedPortfolioId);
    if (!selectedPortfolio) {
      this.snackBar.open('Selected portfolio not found. Please refresh.', 'Close', { duration: 3000 });
      return;
    }

    const dialogRef = this.dialog.open(EditPortfolioDialogComponent, {
      width: '400px',
      data: { portfolio: selectedPortfolio }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && this.selectedPortfolioId && this.portfolios) {
        this.isUpdatingPortfolio = true;
        this.portfolioService.updatePortfolio(this.selectedPortfolioId, result)
          .pipe(finalize(() => this.isUpdatingPortfolio = false))
          .subscribe(updatedPortfolio => {
            if (updatedPortfolio) {
              this.snackBar.open(`Portfolio '${updatedPortfolio.name}' updated successfully!`, 'Close', { duration: 3000 });
              if (this.portfolios) {
                const index = this.portfolios.findIndex(p => p.id === this.selectedPortfolioId);
                if (index !== -1) {
                  this.portfolios[index] = updatedPortfolio;
                  this.portfolios = [...this.portfolios];
                }
              }
            }
          });
      }
    });
  }

  deletePortfolio() {
    if (!this.selectedPortfolioId || !this.portfolios) {
      this.snackBar.open('Please select a portfolio to delete.', 'Close', { duration: 3000 });
      return;
    }
    const selectedPortfolio = this.portfolios.find(p => p.id === this.selectedPortfolioId);
    const portfolioName = selectedPortfolio ? selectedPortfolio.name : 'the selected portfolio';

    const dialogRef = this.dialog.open(ConfirmDeleteDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete '${portfolioName}' and all its associated investments? This action cannot be undone.`,
        confirmText: 'Delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && this.selectedPortfolioId) {
        console.log(`Deletion confirmed for portfolio ID: ${this.selectedPortfolioId}`);
        this.isDeletingPortfolio = true;
        this.portfolioService.deletePortfolio(this.selectedPortfolioId)
          .pipe(finalize(() => this.isDeletingPortfolio = false))
          .subscribe(success => {
            if (success) {
              this.snackBar.open(`Portfolio '${portfolioName}' deleted successfully.`, 'Close', { duration: 3000 });
              this.selectedPortfolioId = null;
              this.checkPortfolios(false);
            }
          });
      }
    });
  }

  createInvestment() {
    if (!this.selectedPortfolioId || !this.portfolios) {
      this.snackBar.open('Please select a portfolio first.', 'Close', { duration: 3000 });
      return;
    }

    const selectedPortfolio = this.portfolios.find(p => p.id === this.selectedPortfolioId);
    if (!selectedPortfolio) {
      this.snackBar.open('Selected portfolio not found. Please refresh.', 'Close', { duration: 3000 });
      return;
    }

    console.log(`Create Investment clicked for portfolio: ${this.selectedPortfolioId} (${selectedPortfolio.name})`);

    const dialogRef = this.dialog.open(AddInvestmentDialogComponent, {
      width: '500px',
      data: {
        portfolioId: this.selectedPortfolioId,
        portfolioName: selectedPortfolio.name
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && this.selectedPortfolioId) {
        console.log('Add Investment Dialog closed with data:', result);

        const investmentData = {
          ticker: result.ticker,
          type: result.type,
          currency: result.currency,
          amount: result.amount,
          purchasePrice: result.purchasePrice
        };

        this.investmentService.createInvestment(this.selectedPortfolioId, investmentData)
          .subscribe(createdInvestment => {
            if (createdInvestment) {
              this.snackBar.open(`Investment '${createdInvestment.ticker}' added successfully!`, 'Close', { duration: 3000 });
              this.loadInvestmentsForSelectedPortfolio();
            } else {
              console.log('Investment creation failed (service returned null).');
            }
          });
      } else {
        console.log('Add Investment Dialog was cancelled or returned no result.');
      }
    });
  }

  editInvestment(investment: Investment) {
    if (!this.selectedPortfolioId || !investment || !investment.id) {
      this.snackBar.open('Cannot edit investment: Invalid selection or investment data.', 'Close', { duration: 3000 });
      return;
    }
    console.log(`Edit Investment clicked for:`, investment);

    const dialogRef = this.dialog.open(EditInvestmentDialogComponent, {
      width: '450px',
      data: { investment: investment }
    });

    dialogRef.afterClosed().subscribe((result: EditInvestmentDialogResult | undefined) => {
      if (result && investment.id) {
        console.log('Edit Investment Dialog closed with data:', result);
        const investmentIdStr = String(investment.id);

        this.investmentService.updateInvestment(investmentIdStr, result)
          .subscribe(updatedInvestment => {
            if (updatedInvestment) {
              this.snackBar.open(`Investment '${updatedInvestment.ticker}' updated successfully!`, 'Close', { duration: 3000 });
              this.loadInvestmentsForSelectedPortfolio();
            } else {
              console.error('Investment update failed (service returned null).');
            }
          });
      } else {
        console.log('Edit Investment Dialog was cancelled or returned no result.');
      }
    });
  }

  deleteInvestment(investment: Investment) {
    if (!this.selectedPortfolioId || !investment || !investment.id) {
      this.snackBar.open('Cannot delete investment: Invalid selection or investment data.', 'Close', { duration: 3000 });
      return;
    }
    console.log(`Delete Investment requested for:`, investment);

    const dialogRef = this.dialog.open(ConfirmDeleteDialogComponent, {
      width: '400px',
      data: {
        title: 'Confirm Deletion',
        message: `Are you sure you want to delete the investment in '${investment.ticker || 'N/A'}'? This action cannot be undone.`,
        confirmText: 'Delete'
      }
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed && investment.id) {
        console.log(`Deletion confirmed for investment ID: ${investment.id}`);

        this.investmentService.deleteInvestment(String(investment.id)).subscribe(success => {
          if (success) {
            this.snackBar.open(`Investment '${investment.ticker}' deleted successfully.`, 'Close', { duration: 3000 });
            this.loadInvestmentsForSelectedPortfolio();
          } else {
            console.error(`Failed to delete investment ${investment.id}`);
          }
        });
      } else {
        console.log(`Deletion cancelled for investment ID: ${investment.id}`);
      }
    });
  }

  exportData(): void {
    if (!this.selectedPortfolioId) {
      this.snackBar.open('Please select a portfolio to export.', 'Close', { duration: 3000 });
      return;
    }

    const exportUrl = `/api/portfolios/${this.selectedPortfolioId}/investments/export`;
    const filename = `investments_${this.selectedPortfolioId}.xlsx`;

    this.http.get(exportUrl, {
      responseType: 'blob'
    }).subscribe({
      next: (blob) => {
        if (blob.size === 0) {
          this.snackBar.open('Export failed: Received empty file.', 'Close', { duration: 3000 });
          console.error('Export failed: Received empty blob.');
          return;
        }
        saveAs(blob, filename);
        this.snackBar.open('Export started successfully.', 'Close', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error exporting data:', error);
        let message = 'Error exporting data. Please check backend logs.';
        if (error.error instanceof Blob) {
          const reader = new FileReader();
          reader.onload = () => {
            try {
              const errJson = JSON.parse(reader.result as string);
              message = errJson.message || message;
            } catch (e) { /* Ignore parsing error */ }
            this.snackBar.open(message, 'Close', { duration: 5000 });
          };
          reader.onerror = () => {
            this.snackBar.open(message, 'Close', { duration: 5000 });
          };
          reader.readAsText(error.error);
        } else {
          message = error.message || message;
          this.snackBar.open(message, 'Close', { duration: 5000 });
        }
      }
    });
  }
}
