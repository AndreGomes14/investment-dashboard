export interface ExternalApiCache {
  id?: number;
  ticker: string;
  type: string;
  currentValue?: number;
  lastUpdated: Date;
}
