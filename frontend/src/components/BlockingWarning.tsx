import { useEffect, useState } from 'react';

/**
 * Hook que dispara un "pulso" repetido mientras `active` sea true. Se usa
 * junto a BlockingWarning para que un motivo de bloqueo no pase inadvertido:
 * vibra al aparecer y se repite cada `intervalMs` mientras siga vigente.
 */
export function useShakePulse(active: boolean, intervalMs = 4000) {
  const [nonce, setNonce] = useState(0);
  useEffect(() => {
    if (!active) return;
    setNonce((n) => n + 1);
    const id = setInterval(() => setNonce((n) => n + 1), intervalMs);
    return () => clearInterval(id);
  }, [active, intervalMs]);
  return nonce;
}

/**
 * Texto de bloqueo en rojo que vibra (clase .shake de index.css). El
 * `key={nonce}` fuerza un remount cada pulso, reiniciando la animación.
 * Único punto de verdad de por qué un submit sigue deshabilitado.
 */
export function BlockingWarning({ message }: { message: string }) {
  const nonce = useShakePulse(true);
  return (
    <p
      key={nonce}
      role="alert"
      className="shake"
      style={{ color: 'var(--danger)', fontSize: 13, fontWeight: 600, marginTop: -2 }}
    >
      {message}
    </p>
  );
}