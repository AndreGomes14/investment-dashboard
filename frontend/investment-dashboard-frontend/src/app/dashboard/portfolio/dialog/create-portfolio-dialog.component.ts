import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms'; // Import Reactive Forms
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

export interface CreatePortfolioDialogData {
}

@Component({
  selector: 'app-create-portfolio-dialog',
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
  templateUrl: './create-portfolio-dialog.component.html',
  styleUrls: ['./create-portfolio-dialog.component.css']
})
export class CreatePortfolioDialogComponent {
  portfolioForm: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    public dialogRef: MatDialogRef<CreatePortfolioDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreatePortfolioDialogData
  ) {
    this.portfolioForm = this.fb.group({
      name: ['', Validators.required],
      description: [''] // Optional field
    });
  }

  onCancel(): void {
    this.dialogRef.close(); // Close without sending data
  }

  onSave(): void {
    if (this.portfolioForm.valid) {
      this.dialogRef.close(this.portfolioForm.value); // Close and send form data
    }
  }
}
