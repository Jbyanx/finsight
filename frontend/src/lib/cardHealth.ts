import type { CreditCard } from '../types';

/**
 * Cuánto abonar para volver al 30% "seguro" de utilización — mismo cálculo
 * que hace el backend en CardAlertResponse.recommendedPayment. Se computa
 * en el cliente porque ya tenemos creditLimit/usedAmount en cada tarjeta;
 * evita una llamada extra a /financial/summary solo para este dato.
 */
export function recommendedPayment(card: CreditCard): number {
  const idealUsed = card.creditLimit * 0.3;
  const diff = card.usedAmount - idealUsed;
  return diff > 0 ? diff : 0;
}