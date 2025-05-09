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
  percentProfit?: number | null;
  realizedPnl?: number | null;
}
