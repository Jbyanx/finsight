import { healthOf, money } from '../lib/money';

/**
 * LA LÍNEA DEL 30% — el elemento firma de Finsight.
 *
 * No es una barra de progreso. Una barra de progreso celebra llegar al 100%;
 * aquí el 100% es la ruina. Por eso el umbral del 30% —la regla central del
 * dominio— se dibuja como un objeto FÍSICO: una marca dura que no hay que
 * cruzar. La barra no mide avance: mide cercanía al peligro.
 *
 * El color no decora, ES el dato. Deberías poder leer tu estado sin leer el
 * número.
 */
export function UtilizationBar({
  used,
  limit,
  available,
  utilization
}: {
  used: number;
  limit: number;
  available: number;
  utilization: number;
}) {
  const health = healthOf(utilization);
  const color = 'var(--' + health + ')';
  const fill = Math.min(utilization, 100);

  return (
    <div>
      <div
        style={{
          position: 'relative',
          height: 10,
          borderRadius: 999,
          background: 'var(--line)'
        }}
      >
        <div
          style={{
            width: fill + '%',
            height: '100%',
            borderRadius: 999,
            background: color,
            transition: 'width .4s ease, background .4s ease'
          }}
        />

        {/* El umbral. Es una marca, no un fondo: se cruza o no se cruza. */}
        <div
          aria-hidden
          style={{
            position: 'absolute',
            left: '30%',
            top: -4,
            width: 2,
            height: 18,
            background: 'var(--ink)',
            borderRadius: 1
          }}
        />
      </div>

      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'baseline',
          marginTop: 10
        }}
      >
        <span className="amount" style={{ fontSize: 13, color: 'var(--muted)' }}>
          {money(used)} <span style={{ opacity: 0.5 }}>/ {money(limit)}</span>
        </span>
        <span className="amount" style={{ fontSize: 15, fontWeight: 700, color: color }}>
          {utilization.toFixed(1)}%
        </span>
      </div>

      {/* El disponible —"cuánto puedo gastar"— siempre a la vista: es la plata,
          el % es contexto. */}
      <p className="amount" style={{ fontSize: 13, marginTop: 6 }}>
        <span style={{ color: 'var(--muted)' }}>Disponible </span>
        <span style={{ fontWeight: 600 }}>{money(available)}</span>
      </p>
    </div>
  );
}