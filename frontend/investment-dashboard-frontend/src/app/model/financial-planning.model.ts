export interface FinancialProfile {
  id?: string;
  monthlyIncome: number;
  monthlyExpenses: number;
  currency: string;
  goals?: FinancialGoal[];
  createdAt?: Date;
  updatedAt?: Date;
  monthlySavings?: number;
}

export interface FinancialGoal {
  id?: string;
  goalName: string;
  goalDescription?: string;
  goalType: GoalType;
  targetAmount: number;
  currentSaved: number;
  targetDate: Date;
  status: GoalStatus;
  priority: number;
  createdAt?: Date;
  updatedAt?: Date;
  progressPercentage?: number;
  remainingAmount?: number;
  monthsToTarget?: number;
  requiredMonthlySaving?: number;
}

export enum GoalType {
  CAR = 'CAR',
  HOUSE = 'HOUSE',
  MOTORBIKE = 'MOTORBIKE',
  VACATION = 'VACATION',
  EMERGENCY_FUND = 'EMERGENCY_FUND',
  EDUCATION = 'EDUCATION',
  RETIREMENT = 'RETIREMENT',
  CASH_TARGET = 'CASH_TARGET',
  OTHER = 'OTHER'
}

export enum GoalStatus {
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  PAUSED = 'PAUSED',
  CANCELLED = 'CANCELLED'
}

export interface CreateFinancialProfileRequest {
  monthlyIncome: number;
  monthlyExpenses: number;
  currency: string;
}

export interface UpdateFinancialProfileRequest {
  monthlyIncome: number;
  monthlyExpenses: number;
}

export interface CreateFinancialGoalRequest {
  goalName: string;
  goalDescription?: string;
  goalType: GoalType;
  targetAmount: number;
  currentSaved?: number;
  targetDate: Date;
  priority?: number;
}

export interface UpdateFinancialGoalRequest {
  goalName: string;
  goalDescription?: string;
  goalType: GoalType;
  targetAmount: number;
  currentSaved: number;
  targetDate: Date;
  status: GoalStatus;
  priority: number;
}

export interface FinancialInsights {
  monthlySavings: number;
  savingsRate: number;
  totalGoalsValue: number;
  totalRequiredMonthlySaving: number;
  feasibilityAnalysis: FeasibilityAnalysis;
  goalsPriorityRecommendation: string[];
  emergencyFundRecommendation: EmergencyFundRecommendation;
}

export interface FeasibilityAnalysis {
  isFeasible: boolean;
  totalRequiredSaving: number;
  availableSavings: number;
  surplus: number;
  deficit?: number;
  recommendation?: string;
}

export interface EmergencyFundRecommendation {
  minimumRecommended: number;
  idealAmount: number;
  monthsOfExpensesCovered: number;
}

export const GOAL_TYPE_DISPLAY_NAMES: Record<GoalType, string> = {
  [GoalType.CAR]: 'Car',
  [GoalType.HOUSE]: 'House',
  [GoalType.MOTORBIKE]: 'Motorbike',
  [GoalType.VACATION]: 'Vacation',
  [GoalType.EMERGENCY_FUND]: 'Emergency Fund',
  [GoalType.EDUCATION]: 'Education',
  [GoalType.RETIREMENT]: 'Retirement',
  [GoalType.CASH_TARGET]: 'Cash Target',
  [GoalType.OTHER]: 'Other'
};

export const GOAL_STATUS_DISPLAY_NAMES: Record<GoalStatus, string> = {
  [GoalStatus.ACTIVE]: 'Active',
  [GoalStatus.COMPLETED]: 'Completed',
  [GoalStatus.PAUSED]: 'Paused',
  [GoalStatus.CANCELLED]: 'Cancelled'
};