/**
 * Formato de pesos colombianos: separador de miles con punto, sin decimales.
 * Espeja Money.format() del backend — misma convención en las dos capas.
 */
export function money(value: number | null | undefined): string {
  if (value == null) return '$0';
  return '$' + Math.round(value).toLocaleString('es-CO');
}

export type Health = 'safe' | 'warn' | 'danger';

/** La regla del 30% vive en el dominio; aquí solo la traducimos a color. */
export function healthOf(utilization: number): Health {
  if (utilization > 30) return 'danger';
  if (utilization >= 25) return 'warn';
  return 'safe';
}