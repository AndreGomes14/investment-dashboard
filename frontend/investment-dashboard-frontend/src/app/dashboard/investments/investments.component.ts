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
import { forkJoin, of } from 'rxjs';
import { map, catchError as rxCatchError } from 'rxjs/operators';

import { PortfolioService } from '../../services/portfolio.service';
import { InvestmentService } from '../../services/investment.service';
import { Portfolio } from '../../model/portfolio.model';
import { Investment } from '../../model/investment.model';
import {CreatePortfolioDialogComponent} from '../portfolio/dialog/create-portfolio-dialog.component';
import {EditPortfolioDialogComponent} from '../portfolio/dialog/edit-portfolio-dialog.component';
import {ConfirmDeleteDialogComponent} from '../portfolio/dialog/confirm-delete-dialog.component';
import {AddInvestmentDialogComponent} from './dialog/add-investment-dialog.component';
import {EditInvestmentDialogComponent, EditInvestmentDialogResult} from './dialog/edit-investment-dialog.component';
import { SellConfirmDialogComponent, SellConfirmDialogResult } from './dialog/sell-confirm-dialog.component';

// Define the interface for aggregated data
interface AggregatedInvestment {
  portfolioId: number;
  portfolioName: string;
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

interface AggregatedSoldInvestment {
  ticker: string;
  type: string;
  currency: string;
  totalAmount: number;
  averagePurchasePrice: number;
  averageSellPrice: number;
  realizedPnlAbsolute: number;
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
    MatExpansionModule,
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
  isUpdatingInvestmentValue: boolean = false;
  portfolios: Portfolio[] | null = null;
  selectedPortfolioId: number | 'all' | null = 'all';
  investmentsForSelectedPortfolio: Investment[] | null = null;
  soldInvestmentsForSelectedPortfolio: Investment[] | null = null;
  aggregatedInvestments: AggregatedInvestment[] | null = null;
  aggregatedSoldInvestments: AggregatedSoldInvestment[] | null = null;
  selectedTabIndex: number = 0;

  get hasAnyPortfolios(): boolean {
    return !!(this.portfolios && this.portfolios.length > 0);
  }

  get selectedPortfolioName(): string {
    if (this.selectedPortfolioId === 'all') {
      return 'All Portfolios';
    }
    if (!this.portfolios) {
      return 'Selected Portfolio';
    }
    const selectedPortfolio = this.portfolios.find(p => p.id === this.selectedPortfolioId);
    return selectedPortfolio ? selectedPortfolio.name : 'Selected Portfolio';
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
    this.selectedPortfolioId = 'all';
    this.investmentsForSelectedPortfolio = null;
    this.soldInvestmentsForSelectedPortfolio = null;
    this.aggregatedInvestments = null;
    this.aggregatedSoldInvestments = null;
    this.portfolioService.getUserPortfolios()
      .pipe(
        finalize(() => { if (showLoading) { this.isLoading = false; } })
      )
      .subscribe({
        next: (portfolios) => {
          this.portfolios = portfolios;
          this.loadDataForSelection();
        },
        error: (error) => {
          this.selectedPortfolioId = 'all';
          this.portfolios = [];
          this.loadDataForSelection();
        }
      });
  }

  onPortfolioSelected(): void {
    console.log('Portfolio selected ("all" means All): ', this.selectedPortfolioId);
    this.loadDataForSelection();
  }

  loadDataForSelection(): void {
    this.isLoadingInvestments = true;
    this.investmentsForSelectedPortfolio = null;
    this.soldInvestmentsForSelectedPortfolio = null;
    this.aggregatedInvestments = null;
    this.aggregatedSoldInvestments = null;

    if (this.selectedPortfolioId !== 'all') {
      this.investmentService.getInvestmentsByPortfolioId(this.selectedPortfolioId as number)
        .pipe(finalize(() => this.isLoadingInvestments = false))
        .subscribe(allInvestments => {
          this.processInvestments(allInvestments);
        });
    } else {
      if (!this.portfolios || this.portfolios.length === 0) {
        console.log("No portfolios found to load investments from.");
        this.processInvestments([]);
        this.isLoadingInvestments = false;
        return;
      }

      const portfolioInvestmentRequests = this.portfolios
        .filter(portfolio => portfolio.id !== undefined)
        .map(portfolio =>
          this.investmentService.getInvestmentsByPortfolioId(portfolio.id!)
            .pipe(
              rxCatchError(err => {
                console.error(`Failed to load investments for portfolio ${portfolio.id}:`, err);
                return of([]);
              }),
              map(investments => investments || [])
            )
        );

      forkJoin(portfolioInvestmentRequests)
        .pipe(finalize(() => this.isLoadingInvestments = false))
        .subscribe((resultsArray: Investment[][]) => {
          const allInvestments = resultsArray.flat();
          console.log(`Loaded a total of ${allInvestments.length} investments across all portfolios.`);
          this.processInvestments(allInvestments);
        });
    }
  }

  processInvestments(allInvestments: Investment[] | null): void {
    if (!allInvestments) {
      console.log("Investment list is null or undefined after fetch.");
      this.investmentsForSelectedPortfolio = [];
      this.soldInvestmentsForSelectedPortfolio = [];
      this.aggregatedInvestments = [];
      this.aggregatedSoldInvestments = [];
      return;
    }

    const activeInvestments = allInvestments.filter(inv => inv.status === 'ACTIVE');
    const soldInvestments = allInvestments.filter(inv => inv.status === 'SOLD');

    activeInvestments.forEach((inv: any) => {
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
    soldInvestments.forEach((inv: any) => {
      if (inv.sellPrice !== null && inv.sellPrice !== undefined &&
        inv.purchasePrice !== null && inv.purchasePrice !== undefined &&
        inv.amount !== null && inv.amount !== undefined) {
        inv.realizedPnl = (inv.sellPrice - inv.purchasePrice) * inv.amount;
      } else {
        inv.realizedPnl = null;
      }
    });

    this.investmentsForSelectedPortfolio = activeInvestments;
    this.soldInvestmentsForSelectedPortfolio = soldInvestments;

    if (activeInvestments && activeInvestments.length > 0) {
      const portfolioIdentifier = this.selectedPortfolioId;
      console.log(`Aggregating ${activeInvestments.length} ACTIVE investments for portfolio ${portfolioIdentifier}.`);
      this.aggregateInvestments(activeInvestments);
      console.log(`Aggregated into ${this.aggregatedInvestments?.length} positions.`);
    } else {
      console.log(`No ACTIVE investments to aggregate for the current selection.`);
      this.aggregatedInvestments = [];
    }
    console.log(`Found ${soldInvestments.length} SOLD investments for the current selection.`);

    // Aggregate sold
    if (soldInvestments && soldInvestments.length > 0) {
      this.aggregateSoldInvestments(soldInvestments);
    } else {
      this.aggregatedSoldInvestments = [];
    }
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
        this.portfolioService.updatePortfolio(this.selectedPortfolioId as number, result)
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
        this.portfolioService.deletePortfolio(this.selectedPortfolioId as number)
          .pipe(finalize(() => this.isDeletingPortfolio = false))
          .subscribe(success => {
            if (success) {
              this.snackBar.open(`Portfolio '${portfolioName}' deleted successfully.`, 'Close', { duration: 3000 });
              this.selectedPortfolioId = 'all';
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

        // Build the payload to send to backend
        const investmentData: any = {
          ticker: result.ticker, // For 'Other', this might be blank and will be filled by backend using name
          type: result.type,
          currency: result.currency,
          amount: result.amount,
          purchasePrice: result.purchasePrice
        };

        // When the investment type is 'Other', include the custom asset name (maps to `name` in backend DTO)
        if (result.type === 'Other') {
          investmentData.name = result.customName;
          // If the user provided a current value for the custom asset, pass it along (backend may ignore if unsupported)
          if (result.currentValue !== undefined && result.currentValue !== null) {
            investmentData.currentValue = result.currentValue;
          }
        }

        this.investmentService.createInvestment(this.selectedPortfolioId as number, investmentData)
          .subscribe(createdInvestment => {
            if (createdInvestment) {
              this.snackBar.open(`Investment '${createdInvestment.ticker}' added successfully!`, 'Close', { duration: 3000 });
              this.loadDataForSelection();
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
    if (!investment || !investment.id) {
      this.snackBar.open('Cannot edit investment: Invalid investment data.', 'Close', { duration: 3000 });
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

        this.investmentService.updateInvestment(investmentIdStr, result as any)
          .subscribe(updatedInvestment => {
            if (updatedInvestment) {
              this.snackBar.open(`Investment '${updatedInvestment.ticker}' updated successfully!`, 'Close', { duration: 3000 });
              this.loadDataForSelection();
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
    if (!investment || !investment.id) {
      this.snackBar.open('Cannot delete investment: Invalid investment data.', 'Close', { duration: 3000 });
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
        console.log(`Marking investment DELETED: ${investment.id}`);

        this.investmentService.deleteInvestment(String(investment.id)).subscribe({
          next: (success) => {
            if (success) {
              this.snackBar.open(`Investment '${investment.ticker}' marked as deleted.`, 'Close', { duration: 3000 });
              this.loadDataForSelection();
            } else {
              console.error(`Failed to mark investment ${investment.id} as deleted.`);
              this.snackBar.open(`Failed to mark investment as deleted.`, 'Close', { duration: 3000 });
            }
          },
          error: (err) => {
            console.error(`Error deleting investment ${investment.id}:`, err);
            this.snackBar.open(`Error marking investment as deleted. ${err.message || ''}`, 'Close', { duration: 5000 });
          }
        });
      } else {
        console.log(`Deletion cancelled for investment ID: ${investment.id}`);
      }
    });
  }

  sellInvestment(investment: Investment): void {
    if (!investment || !investment.id) {
      this.snackBar.open('Cannot sell investment: Invalid data.', 'Close', { duration: 3000 });
      return;
    }

    const dialogRef = this.dialog.open(SellConfirmDialogComponent, {
      width: '400px',
      data: { investment: investment }
    });

    dialogRef.afterClosed().subscribe((result: SellConfirmDialogResult | undefined) => {
      if (result && result.sellPrice !== undefined) {
        console.log(`Sell confirmed for investment ID: ${investment.id} at price ${result.sellPrice}`);

        const sellData = { sellPrice: result.sellPrice };

        this.investmentService.sellInvestment(String(investment.id), sellData)
          .subscribe({
            next: (soldInvestment) => {
              if (soldInvestment) {
                this.snackBar.open(`Investment '${soldInvestment.ticker}' marked as SOLD.`, 'Close', { duration: 3000 });
                this.loadDataForSelection();
              } else {
                console.error(`Sell operation failed for investment ${investment.id} (service returned null/unexpected).`);
              }
            },
            error: (err) => {
              console.error(`Error marking investment as SOLD. ${err.message || ''}`);
            }
          });
      } else {
        console.log(`Sell dialog closed without confirmation or valid price for investment ID: ${investment.id}`);
      }
    });
  }

  exportData(): void {
    if (this.isAllSelected()) {
      const url = `/api/portfolios/investments/export-all`;
      const filename = `investments_all.xlsx`;
      this.http.get(url, { responseType: 'blob' }).subscribe(blob => {
        if (blob.size > 0) {
          saveAs(blob, filename);
          this.snackBar.open('Exported all portfolios successfully.', 'Close', { duration: 3000 });
        } else {
          this.snackBar.open('Export failed: empty file.', 'Close', { duration: 3000 });
        }
      }, err => {
        this.snackBar.open('Export failed.', 'Close', { duration: 3000 });
      });
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
            } catch (e) { /* ignore */ }
            this.snackBar.open(message, 'Close', { duration: 5000 });
          };
          reader.onerror = () => this.snackBar.open(message, 'Close', { duration: 5000 });
          reader.readAsText(error.error);
        } else {
          message = error.message || message;
          this.snackBar.open(message, 'Close', { duration: 5000 });
        }
      }
    });
  }

  private aggregateInvestments(investments: Investment[]): void {
    const groups = new Map<string, AggregatedInvestment>();
    const portfolioNameCache = new Map<number, string>();

    for (const investment of investments) {
      if (!investment.ticker) continue;

      const pId: any = (investment.portfolioId ?? (investment.portfolio ? investment.portfolio.id : null));
      const tickerKey = (investment.ticker || '').trim().toUpperCase();
      const key = `${pId}-${tickerKey}`; // distinct per portfolio

      let group = groups.get(key);

      if (!group) {
        const pName = this.getPortfolioNameById(pId, portfolioNameCache);

        group = {
          portfolioId: pId,
          portfolioName: pName,
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

  private aggregateSoldInvestments(investments: Investment[]): void {
    const groups = new Map<string, AggregatedSoldInvestment>();

    for (const inv of investments) {
      if (!inv.ticker) continue;

      const key = inv.ticker.trim().toUpperCase();
      let group = groups.get(key);

      if (!group) {
        group = {
          ticker: inv.ticker,
          type: inv.type || 'N/A',
          currency: inv.currency || 'N/A',
          totalAmount: 0,
          averagePurchasePrice: 0,
          averageSellPrice: 0,
          realizedPnlAbsolute: 0,
          individualInvestments: []
        };
        groups.set(key, group);
      }

      const amount = inv.amount ?? 0;
      group.totalAmount += amount;

      // weighted averages
      group.averagePurchasePrice += (inv.purchasePrice ?? 0) * amount;
      if (inv.sellPrice !== null && inv.sellPrice !== undefined) {
        group.averageSellPrice += inv.sellPrice * amount;
      }

      group.realizedPnlAbsolute += inv.realizedPnl ?? 0;
      group.individualInvestments.push(inv);
    }

    // finalize averages
    this.aggregatedSoldInvestments = Array.from(groups.values()).map(g => {
      if (g.totalAmount > 0) {
        g.averagePurchasePrice = g.averagePurchasePrice / g.totalAmount;
        g.averageSellPrice = g.averageSellPrice / g.totalAmount;
      }
      return g;
    });
  }

  getPortfolioNameById(id: any, cache?: Map<any, string>): string {
    if (cache && cache.has(id)) return cache.get(id)!;
    const name = this.portfolios?.find(p => String(p.id) === String(id))?.name || 'Unknown';
    if (cache) cache.set(id, name);
    return name;
  }

  promptAndUpdateValue(investment: Investment): void {
    const newValueString = window.prompt(`Enter new current value for ${investment.ticker}:`, investment.currentValue?.toString() || '0');
    if (newValueString === null) {
      this.snackBar.open('Update cancelled.', 'Close', { duration: 2000 });
      return; // User cancelled
    }

    const newValue = parseFloat(newValueString);
    if (isNaN(newValue) || newValue < 0) {
      this.snackBar.open('Invalid value entered. Please enter a positive number.', 'Close', { duration: 3000, panelClass: ['error-snackbar'] });
      return;
    }

    if (!investment.id) {
      this.snackBar.open('Investment ID is missing. Cannot update.', 'Close', { duration: 3000, panelClass: ['error-snackbar'] });
      return;
    }

    this.isUpdatingInvestmentValue = true;
    this.investmentService.manuallyUpdateInvestmentCurrentValue(investment.id, newValue)
      .pipe(finalize(() => this.isUpdatingInvestmentValue = false))
      .subscribe({
        next: (updatedInvestment: Investment) => {
          this.snackBar.open(`Value for ${updatedInvestment.ticker} updated successfully.`, 'Close', { duration: 3000 });
          this.loadDataForSelection();
        },
        error: (err: any) => {
          console.error('Error updating investment value:', err);
          const errorMessage = err.error?.message || 'Failed to update investment value. Please try again.';
          this.snackBar.open(errorMessage, 'Close', { duration: 5000, panelClass: ['error-snackbar'] });
        }
      });
  }

  private isAllSelected(): boolean {
    return this.selectedPortfolioId === 'all';
  }

  onTabChange(event: any): void {
    this.selectedTabIndex = event.index;
  }
}
