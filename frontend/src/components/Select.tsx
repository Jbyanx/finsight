import type { ReactNode } from 'react';

/**
 * Select con la misma piel que Field: eyebrow arriba, control debajo,
 * hint opcional. La flecha nativa se oculta (appearance: none) y se
 * reemplaza por un chevron propio para que no desentone con el resto
 * de inputs de la app. Deshabilitado: opaco y cursor not-allowed, la
 * vista debe explicarse sola sin texto adicional.
 */
export function Select({
  label,
  value,
  onChange,
  children,
  hint,
  disabled
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  children: ReactNode;
  hint?: string;
  disabled?: boolean;
}) {
  return (
    <label style={{ display: 'grid', gap: 6 }}>
      <span className="eyebrow">{label}</span>
      <div style={{ position: 'relative' }}>
        <select
          value={value}
          onChange={(e) => onChange(e.target.value)}
          disabled={disabled}
          style={{
            font: 'inherit',
            fontSize: 15,
            padding: '13px 36px 13px 14px',
            border: '1px solid var(--line)',
            borderRadius: 10,
            background: 'var(--card)',
            color: 'var(--ink)',
            width: '100%',
            appearance: 'none',
            WebkitAppearance: 'none',
            cursor: disabled ? 'not-allowed' : 'pointer',
            opacity: disabled ? 0.5 : 1
          }}
        >
          {children}
        </select>
        <svg
          width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--muted)"
          strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden
          style={{
            position: 'absolute', right: 12, top: '50%',
            transform: 'translateY(-50%)', pointerEvents: 'none'
          }}
        >
          <path d="M6 9l6 6 6-6" />
        </svg>
      </div>
      {hint && <span style={{ fontSize: 12, color: 'var(--muted)' }}>{hint}</span>}
    </label>
  );
}