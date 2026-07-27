import type { ChangeEvent } from 'react';

/**
 * Input de dinero: signo $ fijo a la izquierda, separador de miles en vivo
 * mientras el usuario escribe o borra. El valor que sale por onChange es
 * SIEMPRE el número limpio sin puntos — los formularios que ya validan con
 * Number(amount) y arman el body de la API no cambian ni una línea; este
 * componente es un reemplazo visual de Field para montos, nada más.
 *
 * Solo pesos colombianos por ahora — sin decimales de uso diario. El día
 * que haya soporte multimoneda, algunas (USD, EUR) sí necesitan decimales
 * y ahí probablemente haga falta la coma como separador decimal (el
 * formato que usa Nu: "100,00"). Meter esa distinción hoy, con una sola
 * moneda en toda la app, sería resolver un problema que no existe todavía.
 *
 * type="text" (no "number") a propósito: así evitamos el bug del navegador
 * donde la rueda del mouse cambia el valor de un input numérico enfocado —
 * ese comportamiento nativo simplemente no existe en un input de texto.
 */
export function MoneyField({
  label,
  value,
  onChange,
  placeholder,
  hint
}: {
  label: string;
  value: string;
  onChange: (raw: string) => void;
  placeholder?: string;
  hint?: string;
}) {
  const digitsOnly = value.replace(/\D/g, '');
  const display = digitsOnly === '' ? '' : Number(digitsOnly).toLocaleString('es-CO');

  function handleChange(e: ChangeEvent<HTMLInputElement>) {
    onChange(e.target.value.replace(/\D/g, ''));
  }

  return (
    <label style={{ display: 'grid', gap: 6 }}>
      <span className="eyebrow">{label}</span>
      <div style={{ position: 'relative' }}>
        <span
          aria-hidden
          style={{
            position: 'absolute', left: 14, top: '50%', transform: 'translateY(-50%)',
            fontFamily: 'var(--mono)', fontSize: 16, color: 'var(--muted)', pointerEvents: 'none'
          }}
        >
          $
        </span>
        <input
          type="text"
          inputMode="numeric"
          value={display}
          onChange={handleChange}
          placeholder={placeholder}
          style={{
            font: 'inherit',
            fontFamily: 'var(--mono)', // cifras tabulares — mismo criterio que .amount
            fontSize: 16, // menos de 16px hace que iOS haga zoom al enfocar
            padding: '13px 14px 13px 28px', // espacio extra a la izquierda para el $
            border: '1px solid var(--line)',
            borderRadius: 10,
            background: 'var(--card)',
            color: 'var(--ink)',
            width: '100%'
          }}
        />
      </div>
      {hint && <span style={{ fontSize: 12, color: 'var(--muted)' }}>{hint}</span>}
    </label>
  );
}