/**
 * Bloque de error inline, consistente en toda la app.
 *
 * El TEXTO viene del backend (a través de errorMessage); este componente solo lo
 * enmarca con el color de peligro del sistema. Los errores de regla de dominio
 * van AQUÍ, junto a donde el usuario actuó — nunca como toast flotante.
 */
export function InlineAlert({ message }: { message: string }) {
  return (
    <p
      role="alert"
      style={{
        display: 'flex',
        alignItems: 'flex-start',
        gap: 8,
        color: 'var(--danger)',
        background: 'var(--danger-soft)',
        border: '1px solid var(--danger)',
        borderRadius: 10,
        padding: '10px 12px',
        fontSize: 13,
        lineHeight: 1.4
      }}
    >
      <AlertIcon />
      <span>{message}</span>
    </p>
  );
}

function AlertIcon() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      style={{ flexShrink: 0, marginTop: 1 }}
    >
      <circle cx="12" cy="12" r="10" />
      <line x1="12" y1="8" x2="12" y2="12" />
      <line x1="12" y1="16" x2="12.01" y2="16" />
    </svg>
  );
}
