import { ApiError } from './api';

/**
 * Humaniza el mensaje de un error del backend.
 *
 * El backend es la fuente de verdad: su ErrorResponse trae { status, error,
 * message } pero NO un código de error. Mientras no lo tenga, este mapa es un
 * PUENTE: traduce a español claro los mensajes conocidos que llegan en inglés o
 * en tono técnico. Cualquier mensaje sin match se devuelve TAL CUAL — nunca
 * ocultamos información que el backend quiso dar.
 *
 * El match es por PREFIJO porque varios mensajes terminan con un dato variable
 * (ej. "Deposit exceeds available account balance: 150000").
 *
 * Mantener el mapa pequeño y acotado a lo alcanzable desde estas pantallas.
 */
const KNOWN: ReadonlyArray<readonly [string, string]> = [
  // Bolsillos: depositar / retirar / crear con saldo
  ['Deposit exceeds available account balance',
    'No puedes agregar esa cantidad: supera el disponible de la cuenta.'],
  ['Withdrawal exceeds pocket balance',
    'No puedes retirar esa cantidad: supera el saldo del bolsillo.'],
  ['Pocket balance exceeds available account balance',
    'El saldo del bolsillo supera el disponible de la cuenta.'],
  // Validación de monto (Bean Validation del backend)
  ['Amount must be greater than zero', 'El monto debe ser mayor a cero.'],
  ['Amount must be greater than 0', 'El monto debe ser mayor a cero.'],
  ['Amount cannot be negative', 'El monto no puede ser negativo.'],
  ['Amount is required', 'Ingresa un monto.'],
  // Deudas: abono
  ['Payment amount exceeds remaining balance',
    'El pago supera lo que te falta de la deuda.'],
  // Tarjetas / cuentas (transacciones)
  ['Insufficient credit limit on card', 'No hay cupo suficiente en la tarjeta.'],
  ['Insufficient available balance on account',
    'No hay saldo disponible suficiente en la cuenta.'],
  ['Insufficient balance in pocket', 'No hay saldo suficiente en el bolsillo.']
];

/**
 * Devuelve un mensaje en español listo para mostrar. Si `err` no es un ApiError
 * (p. ej. un fallo de red), se usa `fallback`, específico de cada acción.
 */
export function errorMessage(err: unknown, fallback = 'Algo salió mal. Intenta de nuevo.'): string {
  if (!(err instanceof ApiError)) return fallback;

  const msg = err.message?.trim();
  if (!msg) return fallback;

  for (const [prefix, human] of KNOWN) {
    if (msg.startsWith(prefix)) return human;
  }
  // Sin match: el mensaje del backend tal cual (ya suele venir en español claro).
  return msg;
}
