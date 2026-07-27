function clampToMonth(year: number, month: number, day: number): Date {
  // día 0 del mes siguiente = último día de este mes. Evita que un día 31
  // se desborde a marzo cuando el mes es febrero.
  const lastDay = new Date(year, month + 1, 0).getDate();
  return new Date(year, month, Math.min(day, lastDay));
}

/**
 * Próxima fecha real en que cae un día del mes (corte de tarjeta,
 * vencimiento de factura...). Si ese día ya pasó en el mes actual, salta
 * al siguiente — lo que le importa al usuario es la PRÓXIMA vez que
 * ocurre, no una fecha que ya quedó atrás.
 */
export function nextOccurrence(day: number, from: Date = new Date()): Date {
  const today = new Date(from.getFullYear(), from.getMonth(), from.getDate());
  const candidate = clampToMonth(from.getFullYear(), from.getMonth(), day);
  return candidate >= today ? candidate : clampToMonth(from.getFullYear(), from.getMonth() + 1, day);
}

export function formatNextOccurrence(day: number): string {
  return nextOccurrence(day).toLocaleDateString('es-CO', { day: 'numeric', month: 'long' });
}