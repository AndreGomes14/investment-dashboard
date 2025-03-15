import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-investments',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatTableModule, MatButtonModule],
  template: `
    <div class="investments-container">
      <h1>Investment Management</h1>
      <mat-card>
        <mat-card-content>
          <h2>Your Investment Portfolio</h2>
          <!-- Add your investment management content here -->
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .investments-container {
      padding: 20px;
    }
    h1 {
      margin-bottom: 30px;
      color: #2c3e50;
    }
  `]
})
export class InvestmentsComponent {}
