/** Espeja los DTOs del backend — generado desde OpenAPI v1. */

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

/* ── Tarjetas ── */

export type UtilizationStatus = 'SAFE' | 'YELLOW' | 'RED';

export interface CreditCard {
  id: string;
  institution: string;
  nickname?: string;
  creditLimit: number;
  usedAmount: number;
  availableAmount: number;
  utilizationPercentage: number;
  utilizationStatus: UtilizationStatus;
  cutOffDay: number;
  paymentDueDay: number;
  createdAt: string;
}

/* ── Cuentas y bolsillos ── */

export interface Pocket {
  id: string;
  name: string;
  balance: number;
  goalAmount?: number;
  progressPercentage?: number;
  shared: boolean;
  sharedOwnerName?: string;
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
  createdAt: string;
}

/* ── Efectivo ── */

export interface Cash {
  id: string;
  balance: number;
  updatedAt: string;
}

/* ── Transacciones ── */

export type OriginType = 'CREDIT_CARD' | 'SAVINGS' | 'CASH';
export type TransactionStatus = 'PENDING' | 'COMPLETED';

export interface Transaction {
  id: string;
  description: string;
  amount: number;
  category?: string;
  date: string;
  originType: OriginType;
  originName?: string;
  pocketName?: string;
  thirdParty: boolean;
  thirdPartyOwnerName?: string;
  shared: boolean;
  sharedWithName?: string;
  sharedAmount?: number;
  paymentDeadline?: string;
  installments: number;
  currentInstallment: number;
  status: TransactionStatus;
  createdAt: string;
}

/* ── Ingresos ── */

export type IncomeType = 'FIXED' | 'VARIABLE';
export type IncomeFrequency = 'PER_SHIFT' | 'WEEKLY' | 'MONTHLY';
export type DestinationType = 'CASH' | 'SAVINGS' | 'POCKET';

export interface IncomeSource {
  id: string;
  description: string;
  type: IncomeType;
  estimatedAmount?: number;
  frequency?: IncomeFrequency;
  occurrencesPerMonth?: number;
  createdAt: string;
}

export interface IncomeEntry {
  id: string;
  amount: number;
  description?: string;
  incomeSourceDescription?: string;
  destinationType: DestinationType;
  destinationName?: string;
  receivedAt: string;
  createdAt: string;
}

export interface OperationalExpense {
  id: string;
  description: string;
  frequency: IncomeFrequency;
  occurrencesPerMonth?: number;
  estimatedAmount: number;
  incomeSourceDescription?: string;
  createdAt: string;
}

/* ── Facturas ── */

export type InvoiceFrequency = 'MONTHLY' | 'BIMONTHLY' | 'ANNUAL';
export type PaymentMethod = 'CREDIT_CARD' | 'CASH' | 'TRANSFER';
export type PaymentStatus = 'PENDING' | 'PAID' | 'OVERDUE';
export type VoucherType = 'IMAGE' | 'PDF';

export interface Invoice {
  id: string;
  description: string;
  amount: number;
  frequency: InvoiceFrequency;
  dueDay: number;
  paymentMethod?: PaymentMethod;
  creditCardName?: string;
  own: boolean;
  responsibleName?: string;
  payerNote?: string;
  createdAt: string;
}

export interface InvoicePayment {
  id: string;
  amountPaid: number;
  paidAt?: string;
  periodMonth: number;
  periodYear: number;
  status: PaymentStatus;
  notes?: string;
  voucherUrl?: string;
  voucherType?: VoucherType;
  createdAt: string;
}

/* ── Deudas ── */

export interface FixedDebt {
  id: string;
  institution: string;
  description?: string;
  totalAmount: number;
  monthlyPayment: number;
  interestRate?: number;
  paymentDueDay: number;
  remainingBalance: number;
  paidAmount: number;
  progressPercentage: number;
  paidOff: boolean;
  createdAt: string;
}

/* ── Resumen financiero ── */

export interface CardAlert {
  cardId: string;
  institution: string;
  nickname?: string;
  utilizationPercentage: number;
  status: UtilizationStatus;
  recommendedPayment: number;
}

export interface InvoiceAlert {
  invoiceId: string;
  description: string;
  amount: number;
  dueDay: number;
  own: boolean;
  responsibleName?: string;
}

export interface FinancialSummary {
  totalCreditLimit: number;
  totalUsedAmount: number;
  totalAvailableCredit: number;
  overallUtilizationPercentage: number;
  cardAlerts: CardAlert[];
  totalSavingsBalance: number;
  totalCashBalance: number;
  totalAvailableBalance: number;
  totalRemainingDebt: number;
  totalMonthlyDebtPayment: number;
  estimatedMonthlyIncome: number;
  estimatedMonthlyOperationalExpenses: number;
  estimatedNetIncome: number;
  upcomingInvoices: InvoiceAlert[];
}

/* ── Sesiones ── */

export interface Session {
  id: string;
  deviceInfo?: string;
  type: string;
  createdAt: string;
  expiresAt: string;
}

/* ── Paginación ── */

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;   // página actual (0-based)
  size: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}