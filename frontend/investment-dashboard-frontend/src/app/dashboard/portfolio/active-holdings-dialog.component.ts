import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { Investment } from '../../model/investment.model';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-active-holdings-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './active-holdings-dialog.component.html',
  styleUrls: ['./active-holdings-dialog.component.css']
})
export class ActiveHoldingsDialogComponent {
  constructor(
    @Inject(MAT_DIALOG_DATA) public data: { holdings: Investment[] },
    private dialogRef: MatDialogRef<ActiveHoldingsDialogComponent>
  ) {}

  close(): void {
    this.dialogRef.close();
  }
} 