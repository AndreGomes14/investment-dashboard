import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

interface AllocationData {
  allocation: { [currency: string]: number };
}

@Component({
  selector: 'app-currency-allocation-details-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatTableModule, MatButtonModule, MatIconModule],
  template: `
    <h1 mat-dialog-title>Currency Allocation Details</h1>
    <div mat-dialog-content>
      <table mat-table [dataSource]="tableData" class="mat-elevation-z1" style="width:100%">
        <ng-container matColumnDef="currency">
          <th mat-header-cell *matHeaderCellDef>Currency</th>
          <td mat-cell *matCellDef="let row">{{row.currency}}</td>
        </ng-container>
        <ng-container matColumnDef="percentage">
          <th mat-header-cell *matHeaderCellDef>Allocation %</th>
          <td mat-cell *matCellDef="let row">{{row.percentage | number:'1.2-2'}}%</td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
        <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
      </table>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-raised-button color="primary" (click)="dialogRef.close()">
        <mat-icon>close</mat-icon>
        Close
      </button>
    </div>
  `,
})
export class CurrencyAllocationDetailsDialogComponent {
  displayedColumns = ['currency', 'percentage'];
  tableData: { currency: string; percentage: number }[] = [];

  constructor(
    public dialogRef: MatDialogRef<CurrencyAllocationDetailsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AllocationData
  ) {
    this.tableData = Object.entries(data.allocation)
      .map(([currency, pct]) => ({ currency, percentage: pct }))
      .sort((a, b) => b.percentage - a.percentage);
  }
} 