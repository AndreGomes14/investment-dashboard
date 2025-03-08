import { Portfolio } from './portfolio.model';
import { UserSetting } from './user-setting.model';

export interface User {
  id?: number;
  username: string;
  email: string;
  createdAt?: Date;
  updatedAt?: Date;
  settings?: UserSetting[];
  portfolios?: Portfolio[];
}

export interface UserCredentials {
  username: string;
  password: string;
}

export interface UserRegistration extends UserCredentials {
  email: string;
  confirmPassword: string;
}
