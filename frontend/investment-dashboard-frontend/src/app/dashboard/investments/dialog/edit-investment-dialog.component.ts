import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { Investment } from '../../../model/investment.model'; // Adjust path if needed

// Data passed TO the dialog
export interface EditInvestmentDialogData {
  investment: Investment;
}

// Data returned FROM the dialog
export interface EditInvestmentDialogResult {
  amount: number;
  purchasePrice: number;
}

@Component({
  selector: 'app-edit-investment-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './edit-investment-dialog.component.html',
})
export class EditInvestmentDialogComponent implements OnInit {
  investmentForm: FormGroup;
  investment: Investment;

  constructor(
    private readonly fb: FormBuilder,
    public dialogRef: MatDialogRef<EditInvestmentDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EditInvestmentDialogData
  ) {
    this.investment = data.investment;
    this.investmentForm = this.fb.group({
      ticker: [{value: this.investment.ticker || '', disabled: true}], // Display only
      type: [{value: this.investment.type || '', disabled: true}],     // Display only
      currency: [{value: this.investment.currency || '', disabled: true}], // Display only
      amount: [this.investment.amount, [Validators.required, Validators.min(0.000001)]],
      purchasePrice: [this.investment.purchasePrice, [Validators.required, Validators.min(0)]]
    });
  }

  ngOnInit(): void { }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (this.investmentForm.valid) {
      const result: EditInvestmentDialogResult = {
        amount: this.investmentForm.value.amount,
        purchasePrice: this.investmentForm.value.purchasePrice
      };
      this.dialogRef.close(result);
    } else {
      console.error("Edit form is invalid", this.investmentForm.errors);
      // Optionally mark fields as touched to show errors
      this.investmentForm.markAllAsTouched();
    }
  }
}
