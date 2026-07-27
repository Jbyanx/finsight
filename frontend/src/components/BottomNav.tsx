import { NavLink } from 'react-router-dom';

const NAV_ITEMS = [
  { to: '/',             label: 'Inicio',      icon: <HomeIcon /> },
  { to: '/cards',        label: 'Tarjetas',    icon: <CardIcon /> },
  { to: '/transactions', label: 'Movimientos', icon: <TxIcon /> },
  { to: '/savings',      label: 'Cuentas',      icon: <SavingsIcon /> },
  { to: '/more',         label: 'Más',         icon: <MoreIcon /> },
];

export function BottomNav() {
  return (
    <>
      <nav style={{
        position: 'fixed',
        bottom: 0,
        left: '50%',
        transform: 'translateX(-50%)',
        width: '100%',
        maxWidth: 480,
        background: 'var(--card)',
        borderTop: '1px solid var(--line)',
        display: 'flex',
        paddingBottom: 'env(safe-area-inset-bottom)',
        zIndex: 100,
      }}>
        {NAV_ITEMS.map(({ to, label, icon }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            style={({ isActive }) => ({
              flex: 1,
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              gap: 2,
              padding: '8px 0',
              fontSize: 10,
              fontWeight: 500,
              textDecoration: 'none',
              color: isActive ? 'var(--ink)' : 'var(--muted)',
            })}
          >
            {icon}
            {label}
          </NavLink>
        ))}
      </nav>
      <div style={{ height: 60 }} />
    </>
  );
}

function HomeIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9.5L12 3l9 6.5V20a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9.5z"/>
      <path d="M9 21V12h6v9"/>
    </svg>
  );
}

function CardIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="5" width="20" height="14" rx="2"/>
      <path d="M2 10h20"/>
      <path d="M6 14h4"/>
    </svg>
  );
}

function TxIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M7 16V8"/>
      <path d="M4 11l3-3 3 3"/>
      <path d="M17 8v8"/>
      <path d="M14 13l3 3 3-3"/>
    </svg>
  );
}

function SavingsIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M19 11c0 4.4-3.6 8-8 8H4.5C3.1 19 2 17.9 2 16.5v-1c0-.8.4-1.5 1-2C2.4 13 2 12.3 2 11.5v-1C2 9.1 3.1 8 4.5 8H5V7a7 7 0 0 1 14 0v1h.5C20.9 8 22 9.1 22 10.5v1c0 .8-.4 1.5-1 2 .6.5 1 1.2 1 2v1c0 1.4-1.1 2.5-2.5 2.5H19"/>
      <ellipse cx="12" cy="13" rx="3" ry="3.5"/>
      <circle cx="10" cy="12.5" r=".4" fill="currentColor"/>
      <circle cx="14" cy="12.5" r=".4" fill="currentColor"/>
      <path d="M10 15.5c.6.3 1.3.5 2 .5s1.4-.2 2-.5"/>
    </svg>
  );
}

function MoreIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
      <circle cx="5" cy="12" r="1.5" fill="currentColor" stroke="none"/>
      <circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none"/>
      <circle cx="19" cy="12" r="1.5" fill="currentColor" stroke="none"/>
    </svg>
  );
}