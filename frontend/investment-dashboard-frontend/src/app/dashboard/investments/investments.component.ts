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

import { PortfolioService } from '../../services/portfolio.service';
import { InvestmentService } from '../../services/investment.service';
import { Portfolio } from '../../model/portfolio.model';
import { Investment } from '../../model/investment.model';
import {CreatePortfolioDialogComponent} from '../portfolio/dialog/create-portfolio-dialog.component';
import {EditPortfolioDialogComponent} from '../portfolio/dialog/edit-portfolio-dialog.component';
import {ConfirmDeleteDialogComponent} from '../portfolio/dialog/confirm-delete-dialog.component';
import {AddInvestmentDialogComponent} from './dialog/add-investment-dialog.component';

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
    MatSelectModule
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

  constructor(
    private readonly portfolioService: PortfolioService,
    private readonly investmentService: InvestmentService,
    private readonly dialog: MatDialog,
    private readonly snackBar: MatSnackBar
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
    this.investmentService.getInvestmentsByPortfolioId(this.selectedPortfolioId)
      .pipe(finalize(() => this.isLoadingInvestments = false))
      .subscribe(investments => {
        this.investmentsForSelectedPortfolio = investments;
        if (investments) {
          console.log(`Loaded ${investments.length} investments for portfolio ${this.selectedPortfolioId}`);
        } else {
          console.log(`Failed to load investments or none exist for portfolio ${this.selectedPortfolioId}`);
        }
      });
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
      width: '500px', // Adjust width as needed
      data: {
        portfolioId: this.selectedPortfolioId,
        portfolioName: selectedPortfolio.name // Pass name for display in dialog
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      // Check if result is not null or undefined (user didn't cancel)
      if (result && this.selectedPortfolioId) {
        console.log('Add Investment Dialog closed with data:', result);

        // The result already contains portfolioId from the dialog, but we use the component's selectedPortfolioId for certainty
        // The service method expects the investment data separately from portfolioId
        const investmentData = {
          ticker: result.ticker,
          type: result.type,
          currency: result.currency,
          amount: result.amount,
          purchasePrice: result.purchasePrice
        };

        // Add a loading indicator for the creation process if desired
        // this.isLoadingInvestments = true;

        this.investmentService.createInvestment(this.selectedPortfolioId, investmentData)
          // .pipe(finalize(() => this.isLoadingInvestments = false)) // Use finalize if you add a loading indicator
          .subscribe(createdInvestment => {
            if (createdInvestment) {
              this.snackBar.open(`Investment '${createdInvestment.ticker}' added successfully!`, 'Close', { duration: 3000 });
              // Refresh the investment list for the currently selected portfolio
              this.loadInvestmentsForSelectedPortfolio();
            } else {
              // Error message is handled by the service
              console.log('Investment creation failed (service returned null).');
            }
          });
      } else {
        console.log('Add Investment Dialog was cancelled or returned no result.');
      }
    });
  }

  editInvestment(investment: Investment) {
    if (!this.selectedPortfolioId) {
      this.snackBar.open('Cannot edit investment: No portfolio selected.', 'Close', { duration: 3000 });
      return;
    }
    console.log(`Edit Investment clicked for:`, investment);
    console.log(`Portfolio ID: ${this.selectedPortfolioId}`);
    // TODO: Implement dialog/form logic for editing this specific investment
    // Example:
    // const dialogRef = this.dialog.open(EditInvestmentDialogComponent, { data: { investment: investment, portfolioId: this.selectedPortfolioId } });
    // dialogRef.afterClosed().subscribe(result => { ... });
  }

  deleteInvestment(investment: Investment) {
    if (!this.selectedPortfolioId) {
      this.snackBar.open('Cannot delete investment: No portfolio selected.', 'Close', { duration: 3000 });
      return;
    }
    console.log(`Delete Investment clicked for:`, investment);
    console.log(`Portfolio ID: ${this.selectedPortfolioId}`);

    // TODO: Open confirmation dialog
    // Example:
    // const dialogRef = this.dialog.open(ConfirmDeleteDialogComponent, {
    //   data: { title: 'Confirm Deletion', message: `Delete investment '${investment.ticker}'?` }
    // });
    // dialogRef.afterClosed().subscribe(confirmed => {
    //    if (confirmed) {
    //        this.investmentService.deleteInvestment(investment.id, this.selectedPortfolioId).subscribe(...);
    //    }
    // });
  }
}
