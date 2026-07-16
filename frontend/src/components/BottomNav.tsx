import { NavLink } from 'react-router-dom';
import type { ReactNode } from 'react';

/**
 * Navegación inferior estilo app móvil. Vive fija al fondo, dentro del ancho de
 * 480px del contenedor, y respeta el área segura del teléfono. El destino activo
 * se marca con --ink; el resto en --muted. Sin color decorativo.
 */
export function BottomNav() {
  return (
    <>
      {/* Reserva el alto de la barra para que nada quede tapado detrás de ella. */}
      <div aria-hidden style={{ height: 'calc(64px + env(safe-area-inset-bottom))' }} />

      <nav
        style={{
          position: 'fixed',
          bottom: 0,
          left: '50%',
          transform: 'translateX(-50%)',
          width: '100%',
          maxWidth: 480,
          display: 'flex',
          background: 'var(--card)',
          borderTop: '1px solid var(--line)',
          paddingBottom: 'env(safe-area-inset-bottom)',
          zIndex: 10
        }}
      >
        {/* TODO: evaluar menú "Más" al superar 5 destinos: en móvil ya satura. */}
        <Tab to="/" label="Inicio" icon={<HomeIcon />} />
        <Tab to="/cards" label="Tarjetas" icon={<CardIcon />} />
        <Tab to="/savings" label="Ahorros" icon={<SavingsIcon />} />
        <Tab to="/debts" label="Deudas" icon={<DebtIcon />} />
        <Tab to="/telegram" label="Telegram" icon={<SendIcon />} />
      </nav>
    </>
  );
}

function Tab({ to, label, icon }: { to: string; label: string; icon: ReactNode }) {
  return (
    <NavLink
      to={to}
      end={to === '/'}
      style={({ isActive }) => ({
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 4,
        padding: '10px 0',
        textDecoration: 'none',
        color: isActive ? 'var(--ink)' : 'var(--muted)'
      })}
    >
      {icon}
      <span style={{ fontSize: 11, fontWeight: 600, letterSpacing: '0.02em' }}>{label}</span>
    </NavLink>
  );
}

const svg = {
  width: 22,
  height: 22,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const
};

function HomeIcon() {
  return (
    <svg {...svg} aria-hidden>
      <path d="M3 10.5 12 3l9 7.5" />
      <path d="M5 9.5V21h14V9.5" />
    </svg>
  );
}

function CardIcon() {
  return (
    <svg {...svg} aria-hidden>
      <rect x="2.5" y="5" width="19" height="14" rx="2.5" />
      <path d="M2.5 9.5h19" />
    </svg>
  );
}

function SavingsIcon() {
  return (
    <svg {...svg} aria-hidden>
      <path d="M3 12a6 6 0 0 1 6-6h5a5 5 0 0 1 5 5 3 3 0 0 1-1.5 2.6V17h-3v-2H9v2H6v-2.3A6 6 0 0 1 3 12Z" />
      <path d="M3 11H2" />
      <path d="M14.5 9.5h.01" />
    </svg>
  );
}

function DebtIcon() {
  return (
    <svg {...svg} aria-hidden>
      <path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z" />
      <path d="M14 3v5h5" />
      <path d="M9 13h6" />
      <path d="M9 17h6" />
    </svg>
  );
}

function SendIcon() {
  return (
    <svg {...svg} aria-hidden>
      <path d="M21 4 3 11l6 2 2 6 10-15Z" />
      <path d="M9 13l3.5-3.5" />
    </svg>
  );
}
