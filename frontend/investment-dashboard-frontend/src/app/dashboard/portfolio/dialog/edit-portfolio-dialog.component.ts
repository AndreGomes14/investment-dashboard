import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { Portfolio } from '../../../model/portfolio.model';

export interface EditPortfolioDialogData {
  portfolio: Portfolio;
}

@Component({
  selector: 'app-edit-portfolio-dialog',
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
  templateUrl: './edit-portfolio-dialog.component.html',
  styleUrls: ['./create-portfolio-dialog.component.css']
})
export class EditPortfolioDialogComponent implements OnInit {
  portfolioForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<EditPortfolioDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: EditPortfolioDialogData
  ) {
    this.portfolioForm = this.fb.group({
      name: ['', Validators.required],
      description: ['']
    });
  }

  ngOnInit(): void {
    if (this.data && this.data.portfolio) {
      this.portfolioForm.patchValue({
        name: this.data.portfolio.name,
        description: this.data.portfolio.description || ''
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSave(): void {
    if (this.portfolioForm.valid) {
      this.dialogRef.close(this.portfolioForm.value);
    }
  }
}
