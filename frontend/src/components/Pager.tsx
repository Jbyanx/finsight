/**
 * Paginador circular compartido. No depende del tipo de contenido —
 * solo necesita los 4 campos que Spring Data siempre devuelve en un Page.
 * Un botón deshabilitado (opaco + cursor not-allowed) ya comunica por sí
 * solo que no hay más páginas en esa dirección; no hace falta texto extra.
 */
interface PaginationInfo {
  number: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export function Pager({ page, onChange }: { page: PaginationInfo; onChange: (n: number) => void }) {
  if (page.totalPages <= 1) return null;
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 18, marginTop: 24 }}>
      <PagerButton direction="prev" disabled={page.first} onClick={() => onChange(page.number - 1)} />
      <span style={{ fontSize: 13, color: 'var(--muted)', minWidth: 92, textAlign: 'center' }}>
        Página {page.number + 1} de {page.totalPages}
      </span>
      <PagerButton direction="next" disabled={page.last} onClick={() => onChange(page.number + 1)} />
    </div>
  );
}

function PagerButton({
  direction,
  disabled,
  onClick
}: {
  direction: 'prev' | 'next';
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={direction === 'prev' ? 'Página anterior' : 'Página siguiente'}
      style={{
        width: 40, height: 40, borderRadius: '50%',
        border: '1px solid var(--line)',
        background: 'var(--card)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.35 : 1,
        color: 'var(--ink)'
      }}
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        {direction === 'prev' ? <path d="M15 18l-6-6 6-6" /> : <path d="M9 18l6-6-6-6" />}
      </svg>
    </button>
  );
}