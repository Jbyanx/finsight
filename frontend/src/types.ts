/** Espeja los DTOs del backend. */

export interface User {
  id: string;
  email: string;
  name: string;
  pictureUrl?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface CreditCard {
  id: string;
  institution: string;
  nickname?: string;
  creditLimit: number;
  usedAmount: number;
  utilizationPercentage: number;
  availableCredit: number;
  cutOffDay: number;
  paymentDueDay: number;
}

export interface Pocket {
  id: string;
  name: string;
  balance: number;
  goalAmount?: number;
  progressPercentage?: number;
  shared: boolean;
  sharedOwnerName?: string;
}

export interface SavingsAccount {
  id: string;
  institution: string;
  nickname?: string;
  balance: number;
  sharedTotal: number;
  ownBalance: number;
  availableBalance: number;
  pockets: Pocket[];
}

export interface FinancialSummary {
  totalCashBalance: number;
  totalSavingsBalance: number;
  totalCreditUsed: number;
  totalCreditLimit: number;
  overallUtilization: number;
  totalDebt: number;
  netIncome: number;
}