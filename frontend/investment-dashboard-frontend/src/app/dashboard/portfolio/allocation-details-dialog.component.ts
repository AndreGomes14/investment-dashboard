import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogModule, MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

interface AllocationDetail {
  type: string;
  value: number;
  percentage: number;
  profitPercentage: number;
  profitValue: number;
}

@Component({
  selector: 'app-allocation-details-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatTableModule,
    MatSortModule,
    MatIconModule,
    MatButtonModule
  ],
  template: `
    <div class="dialog-container">
      <h2 mat-dialog-title>Asset Allocation Details</h2>
      <mat-dialog-content>
        <table mat-table [dataSource]="allocationDetails" matSort (matSortChange)="sortData($event)" class="allocation-table">
          <!-- Type Column -->
          <ng-container matColumnDef="type">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Asset Type</th>
            <td mat-cell *matCellDef="let element">{{element.type}}</td>
          </ng-container>

          <!-- Value Column -->
          <ng-container matColumnDef="value">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Value</th>
            <td mat-cell *matCellDef="let element">{{element.value | currency:'USD':'symbol':'1.2-2'}}</td>
          </ng-container>

          <!-- Percentage Column -->
          <ng-container matColumnDef="percentage">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Allocation %</th>
            <td mat-cell *matCellDef="let element">{{element.percentage | number:'1.2-2'}}%</td>
          </ng-container>

          <!-- Profit Column -->
          <ng-container matColumnDef="profit">
            <th mat-header-cell *matHeaderCellDef mat-sort-header>Profit/Loss</th>
            <td mat-cell *matCellDef="let element">
              <span [class.positive]="element.profitValue >= 0" [class.negative]="element.profitValue < 0">
                {{element.profitValue | currency:'USD':'symbol':'1.2-2'}}
                ({{element.profitPercentage | number:'1.2-2'}}%)
              </span>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
        </table>
      </mat-dialog-content>
      <mat-dialog-actions align="end">
        <button mat-raised-button color="primary" mat-dialog-close class="close-button">
          <mat-icon>close</mat-icon>
          Close
        </button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .dialog-container {
      padding: 20px;
    }

    .allocation-table {
      width: 100%;
      margin: 20px 0;
    }

    .positive {
      color: #27ae60;
    }

    .negative {
      color: #e74c3c;
    }

    mat-dialog-content {
      min-width: 600px;
    }

    .close-button {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 24px;
      font-weight: 500;
    }

    .close-button mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    th.mat-header-cell {
      font-weight: 600;
      color: #2c3e50;
    }

    td.mat-cell {
      padding: 12px 16px;
    }

    tr.mat-row:hover {
      background-color: #f8f9fa;
    }

    .mat-sort-header-container {
      align-items: center;
    }
  `]
})
export class AllocationDetailsDialogComponent {
  displayedColumns: string[] = ['type', 'value', 'percentage', 'profit'];
  allocationDetails: AllocationDetail[] = [];

  constructor(
    public dialogRef: MatDialogRef<AllocationDetailsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {
      allocationData: { name: string; value: number }[];
      investments: any[];
      totalValue: number;
    }
  ) {
    this.calculateAllocationDetails();
  }

  private calculateAllocationDetails(): void {
    const typeMap = new Map<string, { value: number; cost: number }>();

    // First, calculate total value and cost per type
    this.data.investments.forEach(investment => {
      const type = investment.type;
      const currentValue = (investment.currentValue ?? 0) * investment.amount;
      const cost = investment.totalCost ?? 0;

      if (!typeMap.has(type)) {
        typeMap.set(type, { value: 0, cost: 0 });
      }
      const current = typeMap.get(type)!;
      current.value += currentValue;
      current.cost += cost;
    });

    // Then create the details array
    this.allocationDetails = Array.from(typeMap.entries()).map(([type, data]) => ({
      type,
      value: data.value,
      percentage: (data.value / this.data.totalValue) * 100,
      profitValue: data.value - data.cost,
      profitPercentage: data.cost > 0 ? ((data.value - data.cost) / data.cost) * 100 : 0
    }));

    // Default sort by type alphabetically
    this.sortData({ active: 'type', direction: 'asc' });
  }

  sortData(sort: Sort): void {
    if (!sort.active || sort.direction === '') {
      return;
    }

    this.allocationDetails = [...this.allocationDetails].sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case 'type':
          return this.compare(a.type, b.type, isAsc);
        case 'value':
          return this.compare(a.value, b.value, isAsc);
        case 'percentage':
          return this.compare(a.percentage, b.percentage, isAsc);
        case 'profit':
          return this.compare(a.profitValue, b.profitValue, isAsc);
        default:
          return 0;
      }
    });
  }

  private compare(a: number | string, b: number | string, isAsc: boolean): number {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
  }
} 