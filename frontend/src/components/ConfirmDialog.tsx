import { useEffect } from 'react';

export type ConfirmTone = 'default' | 'danger';

/**
 * Modal de confirmación accesible que reemplaza a window.confirm.
 *
 * Mobile first: se centra dentro del ancho de la app (480px) sobre un overlay
 * atenuado. Cierra con Escape y con tap en el overlay. El botón de confirmar
 * toma el color de la acción (danger para borrados/desvinculación). Al abrir se
 * enfoca "Cancelar": ante una acción destructiva, el default seguro no destruye.
 */
export function ConfirmDialog({
  title,
  message,
  confirmLabel,
  tone = 'default',
  onConfirm,
  onCancel
}: {
  title: string;
  message: string;
  confirmLabel: string;
  tone?: ConfirmTone;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  // Escape cierra el modal desde cualquier foco.
  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onCancel();
    }
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [onCancel]);

  const accent = tone === 'danger' ? 'var(--danger)' : 'var(--ink)';

  return (
    <div
      onClick={onCancel}
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 50,
        background: 'rgba(20, 22, 26, 0.45)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 18
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        aria-describedby="confirm-dialog-message"
        className="card"
        onClick={(e) => e.stopPropagation()}
        style={{ width: '100%', maxWidth: 420, display: 'grid', gap: 14 }}
      >
        <div style={{ display: 'grid', gap: 6 }}>
          <p
            id="confirm-dialog-title"
            style={{ fontFamily: 'var(--display)', fontSize: 20, fontWeight: 700 }}
          >
            {title}
          </p>
          <p
            id="confirm-dialog-message"
            style={{ color: 'var(--muted)', fontSize: 14, lineHeight: 1.5 }}
          >
            {message}
          </p>
        </div>

        <div style={{ display: 'flex', gap: 10 }}>
          <button
            type="button"
            className="btn"
            onClick={onConfirm}
            style={{ background: accent, borderColor: accent, color: 'var(--paper)' }}
          >
            {confirmLabel}
          </button>
          <button
            type="button"
            className="btn btn-ghost"
            onClick={onCancel}
            autoFocus
            style={{ borderColor: 'var(--line)' }}
          >
            Cancelar
          </button>
        </div>
      </div>
    </div>
  );
}
