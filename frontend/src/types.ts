/** Espeja los DTOs del backend. */

export interface User {
  id: string;
  email: string;
  name: string;
  pictureUrl: string | null;
  provider: 'local' | 'google';
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
  availableAmount: number;
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

export interface FixedDebt {
  id: string;
  institution: string;
  description: string;
  totalAmount: number;
  monthlyPayment: number;
  interestRate: number;
  paymentDueDay: number;
  remainingBalance: number;
  paidAmount: number;
  /** Cuánto se ha PAGADO (paidAmount / totalAmount). 100% = saldada. */
  progressPercentage: number;
  paidOff: boolean;
  createdAt: string;
}

export interface SavingsAccount {
  id: string;
  institution: string;
  nickname?: string;
  balance: number;
  sharedTotal: number;
  ownBalance: number;
  availableBalance: number;
  shared: boolean;
  sharedOwnerName?: string;
  pockets: Pocket[];
}

export interface FinancialSummary {
  totalCreditLimit: number;
  totalUsedAmount: number;
  totalAvailableCredit: number;
  overallUtilizationPercentage: number;
  cardAlerts: unknown[];
  totalSavingsBalance: number;
  totalCashBalance: number;
  totalAvailableBalance: number;
  totalRemainingDebt: number;
  totalMonthlyDebtPayment: number;
  estimatedMonthlyIncome: number;
  estimatedMonthlyOperationalExpenses: number;
  estimatedNetIncome: number;
  upcomingInvoices: unknown[];
}