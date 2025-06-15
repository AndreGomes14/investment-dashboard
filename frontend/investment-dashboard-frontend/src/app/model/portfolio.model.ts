import { Investment } from './investment.model';

export interface Portfolio {
  id?: number;
  userId: number;
  name: string;
  description?: string;
  createdAt?: Date;
  updatedAt?: Date;
  investments?: Investment[];
}
