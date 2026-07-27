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

function clampToMonth(year: number, month: number, day: number): Date {
  // día 0 del mes siguiente = último día de este mes. Evita que un cutOffDay
  // de 31 se desborde a marzo cuando el mes es febrero.
  const lastDay = new Date(year, month + 1, 0).getDate();
  return new Date(year, month, Math.min(day, lastDay));
}

/**
 * Próxima fecha real en que cae un día del mes (el corte, el pago...). Si ese
 * día ya pasó en el mes actual, salta al siguiente. La utilización que le
 * importa al usuario es la del PRÓXIMO corte, no uno que ya quedó congelado.
 */
export function nextOccurrence(day: number, from: Date = new Date()): Date {
  const today = new Date(from.getFullYear(), from.getMonth(), from.getDate());
  const candidate = clampToMonth(from.getFullYear(), from.getMonth(), day);
  return candidate >= today ? candidate : clampToMonth(from.getFullYear(), from.getMonth() + 1, day);
}

export function formatNextOccurrence(day: number): string {
  return nextOccurrence(day).toLocaleDateString('es-CO', { day: 'numeric', month: 'long' });
}