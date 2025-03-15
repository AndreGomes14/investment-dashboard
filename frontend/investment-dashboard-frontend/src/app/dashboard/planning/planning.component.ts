import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: `
    <div class="planning-container">
      <h1>Financial Planning</h1>
      <mat-card>
        <mat-card-content>
          <h2>Your Financial Goals</h2>
          <!-- Add financial planning content here -->
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .planning-container {
      padding: 20px;
    }
    h1 {
      margin-bottom: 30px;
      color: #2c3e50;
    }
  `]
})
export class PlanningComponent {}
