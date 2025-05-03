import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { Investment } from '../../../model/investment.model'; // Corrected path

export interface SellConfirmDialogData {
  investment: Investment;
}

@Component({
  selector: 'app-sell-confirm-dialog',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Confirm Sell</h2>
    <mat-dialog-content>
      Are you sure you want to mark the investment in
      <strong>{{ data.investment.ticker }}</strong> ({{ data.investment.amount }} units)
      as SOLD? This action cannot be easily undone.
      <p><small>Note: This will change the status but not delete the record.</small></p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="onNoClick()">Cancel</button>
      <button mat-raised-button color="warn" [mat-dialog-close]="true" cdkFocusInitial>
        <mat-icon>sell</mat-icon> Confirm Sell
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    p small { color: grey; }
  `]
})
export class SellConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<SellConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SellConfirmDialogData
  ) {}

  onNoClick(): void {
    this.dialogRef.close();
  }
}
