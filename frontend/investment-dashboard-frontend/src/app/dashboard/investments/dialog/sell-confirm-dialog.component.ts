import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Investment } from '../../../model/investment.model';

export interface SellConfirmDialogData {
  investment: Investment;
}

export interface SellConfirmDialogResult {
  sellPrice: number;
}

@Component({
  selector: 'app-sell-confirm-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './sell-confirm-dialog.component.html',
  styleUrls: ['./sell-confirm-dialog.component.css']
})
export class SellConfirmDialogComponent implements OnInit {
  sellForm: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    public dialogRef: MatDialogRef<SellConfirmDialogComponent, SellConfirmDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: SellConfirmDialogData
  ) {
    this.sellForm = this.fb.group({
      sellPrice: [this.data.investment.currentValue ?? null, [Validators.required, Validators.min(0)]]
    });
  }

  ngOnInit(): void { }

  onNoClick(): void {
    this.dialogRef.close();
  }

  onConfirmSell(): void {
    if (this.sellForm.valid) {
      this.dialogRef.close({ sellPrice: this.sellForm.value.sellPrice });
    }
  }
}
