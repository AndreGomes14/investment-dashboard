import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { NgxChartsModule } from '@swimlane/ngx-charts';

import { FinancialPlanningService } from '../../services/financial-planning.service';
import {
  FinancialProfile,
  FinancialGoal,
  CreateFinancialProfileRequest,
  UpdateFinancialProfileRequest,
  CreateFinancialGoalRequest,
  UpdateFinancialGoalRequest,
  GoalType,
  GoalStatus,
  GOAL_TYPE_DISPLAY_NAMES,
  GOAL_STATUS_DISPLAY_NAMES
} from '../../model/financial-planning.model';

@Component({
  selector: 'app-planning',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatDialogModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatExpansionModule,
    MatProgressBarModule,
    MatChipsModule,
    MatTooltipModule,
    MatDatepickerModule,
    MatNativeDateModule,
    NgxChartsModule
  ],
  templateUrl: './planning.component.html',
  styleUrl: './planning.component.css'
})
export class PlanningComponent implements OnInit {
  loading = false;
  financialProfile: FinancialProfile | null = null;
  financialGoals: FinancialGoal[] = [];
  
  profileForm!: FormGroup;
  goalForm!: FormGroup;
  editingGoal: FinancialGoal | null = null;
  
  // UI state
  showProfileForm = false;
  showGoalForm = false;
  selectedTabIndex = 0;
  
  // Constants for templates
  goalTypes = Object.values(GoalType);
  goalStatuses = Object.values(GoalStatus);
  goalTypeDisplayNames = GOAL_TYPE_DISPLAY_NAMES;
  goalStatusDisplayNames = GOAL_STATUS_DISPLAY_NAMES;
  
  // Chart data
  goalProgressChartData: any[] = [];
  goalTimelineChartData: any[] = [];
  
  constructor(
    private financialPlanningService: FinancialPlanningService,
    private formBuilder: FormBuilder,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.initializeForms();
  }

  ngOnInit(): void {
    this.loadFinancialData();
  }

  private initializeForms(): void {
    this.profileForm = this.formBuilder.group({
      monthlyIncome: [0, [Validators.required, Validators.min(0.01)]],
      monthlyExpenses: [0, [Validators.required, Validators.min(0)]],
      currency: ['USD', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]]
    });

    this.goalForm = this.formBuilder.group({
      goalName: ['', [Validators.required, Validators.maxLength(100)]],
      goalDescription: ['', [Validators.maxLength(500)]],
      goalType: [GoalType.OTHER, [Validators.required]],
      targetAmount: [0, [Validators.required, Validators.min(0.01)]],
      currentSaved: [0, [Validators.min(0)]],
      targetDate: [null, [Validators.required]],
      priority: [1, [Validators.required, Validators.min(1), Validators.max(10)]],
      status: [GoalStatus.ACTIVE]
    });
  }

  private loadFinancialData(): void {
    this.loading = true;
    
    // Load financial profile
    this.financialPlanningService.getFinancialProfile().subscribe(profile => {
      this.financialProfile = profile;
      if (profile) {
        this.populateProfileForm(profile);
        this.loadFinancialGoals();
      } else {
        this.showProfileForm = true;
        this.loading = false;
      }
    });
  }

  private loadFinancialGoals(): void {
    this.financialPlanningService.getFinancialGoals().subscribe(goals => {
      this.financialGoals = goals || [];
      this.updateChartData();
      this.loading = false;
    });
  }

  private populateProfileForm(profile: FinancialProfile): void {
    this.profileForm.patchValue({
      monthlyIncome: profile.monthlyIncome,
      monthlyExpenses: profile.monthlyExpenses,
      currency: profile.currency
    });
  }

  onSaveProfile(): void {
    if (this.profileForm.valid) {
      this.loading = true;
      const formValue = this.profileForm.value;
      
      const request: CreateFinancialProfileRequest = {
        monthlyIncome: formValue.monthlyIncome,
        monthlyExpenses: formValue.monthlyExpenses,
        currency: formValue.currency
      };

      this.financialPlanningService.createOrUpdateFinancialProfile(request).subscribe(profile => {
        if (profile) {
          this.financialProfile = profile;
          this.showProfileForm = false;
          this.snackBar.open('Financial profile saved successfully!', 'Close', { duration: 3000 });
          this.loadFinancialGoals();
        }
        this.loading = false;
      });
    }
  }

  onUpdateProfile(): void {
    if (this.profileForm.valid && this.financialProfile) {
      this.loading = true;
      const formValue = this.profileForm.value;
      
      const request: UpdateFinancialProfileRequest = {
        monthlyIncome: formValue.monthlyIncome,
        monthlyExpenses: formValue.monthlyExpenses
      };

      this.financialPlanningService.updateFinancialProfile(request).subscribe(profile => {
        if (profile) {
          this.financialProfile = profile;
          this.snackBar.open('Financial profile updated successfully!', 'Close', { duration: 3000 });
          this.loadFinancialGoals(); // Reload to get updated calculations
        }
        this.loading = false;
      });
    }
  }

  onCreateGoal(): void {
    if (this.goalForm.valid) {
      this.loading = true;
      const formValue = this.goalForm.value;
      
      const request: CreateFinancialGoalRequest = {
        goalName: formValue.goalName,
        goalDescription: formValue.goalDescription,
        goalType: formValue.goalType,
        targetAmount: formValue.targetAmount,
        currentSaved: formValue.currentSaved || 0,
        targetDate: formValue.targetDate,
        priority: formValue.priority
      };

      this.financialPlanningService.createFinancialGoal(request).subscribe(goal => {
        if (goal) {
          this.financialGoals.push(goal);
          this.updateChartData();
          this.resetGoalForm();
          this.showGoalForm = false;
          this.snackBar.open('Goal created successfully!', 'Close', { duration: 3000 });
        }
        this.loading = false;
      });
    }
  }

  onUpdateGoal(): void {
    if (this.goalForm.valid && this.editingGoal) {
      this.loading = true;
      const formValue = this.goalForm.value;
      
      const request: UpdateFinancialGoalRequest = {
        goalName: formValue.goalName,
        goalDescription: formValue.goalDescription,
        goalType: formValue.goalType,
        targetAmount: formValue.targetAmount,
        currentSaved: formValue.currentSaved,
        targetDate: formValue.targetDate,
        status: formValue.status,
        priority: formValue.priority
      };

      this.financialPlanningService.updateFinancialGoal(this.editingGoal.id!, request).subscribe(goal => {
        if (goal) {
          const index = this.financialGoals.findIndex(g => g.id === goal.id);
          if (index !== -1) {
            this.financialGoals[index] = goal;
            this.updateChartData();
          }
          this.resetGoalForm();
          this.editingGoal = null;
          this.showGoalForm = false;
          this.snackBar.open('Goal updated successfully!', 'Close', { duration: 3000 });
        }
        this.loading = false;
      });
    }
  }

  onEditGoal(goal: FinancialGoal): void {
    this.editingGoal = goal;
    this.goalForm.patchValue({
      goalName: goal.goalName,
      goalDescription: goal.goalDescription,
      goalType: goal.goalType,
      targetAmount: goal.targetAmount,
      currentSaved: goal.currentSaved,
      targetDate: new Date(goal.targetDate),
      priority: goal.priority,
      status: goal.status
    });
    this.showGoalForm = true;
  }

  onDeleteGoal(goal: FinancialGoal): void {
    if (confirm(`Are you sure you want to delete the goal "${goal.goalName}"?`)) {
      this.loading = true;
      this.financialPlanningService.deleteFinancialGoal(goal.id!).subscribe(success => {
        if (success) {
          this.financialGoals = this.financialGoals.filter(g => g.id !== goal.id);
          this.updateChartData();
        }
        this.loading = false;
      });
    }
  }

  resetGoalForm(): void {
    this.goalForm.reset({
      goalType: GoalType.OTHER,
      currentSaved: 0,
      priority: 1,
      status: GoalStatus.ACTIVE
    });
  }

  private updateChartData(): void {
    // Update progress chart data
    this.goalProgressChartData = this.financialGoals.map(goal => ({
      name: goal.goalName,
      value: goal.progressPercentage || 0
    }));

    // Update timeline chart data
    this.goalTimelineChartData = this.financialGoals.map(goal => ({
      name: goal.goalName,
      value: goal.monthsToTarget || 0
    }));
  }

  // Utility methods
  getMonthlySavings(): number {
    return this.financialProfile ? 
      (this.financialProfile.monthlyIncome - this.financialProfile.monthlyExpenses) : 0;
  }

  getSavingsRate(): number {
    if (!this.financialProfile || this.financialProfile.monthlyIncome === 0) {
      return 0;
    }
    return (this.getMonthlySavings() / this.financialProfile.monthlyIncome) * 100;
  }

  getTotalGoalsValue(): number {
    return this.financialGoals.reduce((sum, goal) => sum + goal.targetAmount, 0);
  }

  getActiveGoalsCount(): number {
    return this.financialGoals.filter(goal => goal.status === GoalStatus.ACTIVE).length;
  }

  formatCurrency(amount: number): string {
    return this.financialPlanningService.formatCurrency(
      amount, 
      this.financialProfile?.currency || 'USD'
    );
  }

  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString();
  }

  getGoalStatusColor(status: GoalStatus): string {
    switch (status) {
      case GoalStatus.ACTIVE: return 'primary';
      case GoalStatus.COMPLETED: return 'accent';
      case GoalStatus.PAUSED: return 'warn';
      case GoalStatus.CANCELLED: return '';
      default: return '';
    }
  }

  getProgressColor(progress: number): string {
    if (progress >= 100) return 'accent';
    if (progress >= 75) return 'primary';
    if (progress >= 50) return 'warn';
    return 'warn';
  }

  // Chart color scheme
  colorScheme = {
    domain: ['#5AA454', '#A10A28', '#C7B42C', '#AAAAAA', '#FF8C00', '#FF69B4', '#87CEEB']
  };
}
