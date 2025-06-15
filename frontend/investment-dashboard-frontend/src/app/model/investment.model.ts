import { Portfolio } from './portfolio.model';

export interface Investment {
  id?: number;
  portfolioId: number;
  portfolio?: Portfolio;  // Used for expanded data
  ticker: string;
  type: string;
  amount: number;
  purchasePrice: number;
  currentValue?: number;
  sellPrice?: number;
  lastUpdateDate?: Date;
  currency: string;
  status: 'ACTIVE' | 'SOLD' | 'DELETED';
  createdAt?: Date;
  updatedAt?: Date;
  totalCost?: number;
  percentProfit?: number | null;
  realizedPnl?: number | null;
  profitOrLoss?: number;
  customName?: string;
}

export interface CreateInvestmentRequest {
  portfolioId: number;
  ticker: string;
  type: string;
  amount: number;
  purchasePrice: number;
  currency: string;
  customName?: string;
}

export interface UpdateInvestmentRequest {
  amount?: number;
  purchasePrice?: number;
  customName?: string;
}

export interface SellInvestmentRequest {
  sellPrice: number;
}
