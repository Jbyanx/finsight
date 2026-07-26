import { Link } from 'react-router-dom';
import type { ReactNode } from 'react';

interface Section {
  to: string;
  label: string;
  desc: string;
  icon: ReactNode;
}

const SECTIONS: Section[] = [
  { to: '/debts',    label: 'Deudas',   desc: 'Créditos y abonos',    icon: <DebtIcon /> },
  { to: '/invoices', label: 'Facturas', desc: 'Pagos recurrentes',     icon: <InvoiceIcon /> },
  { to: '/income',   label: 'Ingresos', desc: 'Fuentes y entradas',    icon: <IncomeIcon /> },
  { to: '/cash',     label: 'Efectivo', desc: 'Dinero en mano',        icon: <CashIcon /> },
  { to: '/profile',  label: 'Perfil',   desc: 'Cuenta y sesiones',     icon: <ProfileIcon /> },
];

export default function More() {
  return (
    <main style={{ padding: '24px 16px', display: 'flex', flexDirection: 'column', gap: 8 }}>
      <h1 style={{ fontSize: 20, fontWeight: 700, marginBottom: 8 }}>Más</h1>
      {SECTIONS.map(({ to, label, desc, icon }) => (
        <Link
          key={to}
          to={to}
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '16px',
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 12,
            textDecoration: 'none',
            color: 'inherit',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <span style={{ color: 'var(--muted)' }}>{icon}</span>
            <div>
              <div style={{ fontWeight: 600, fontSize: 15 }}>{label}</div>
              <div style={{ fontSize: 13, color: 'var(--muted)' }}>{desc}</div>
            </div>
          </div>
          <ChevronIcon />
        </Link>
      ))}
    </main>
  );
}

function ChevronIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--muted)" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M9 18l6-6-6-6"/>
    </svg>
  );
}

function DebtIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="13" r="8"/>
      <path d="M12 9v4l3 3"/>
      <path d="M9 2h6"/>
      <path d="M12 2v3"/>
    </svg>
  );
}

function InvoiceIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
      <path d="M14 2v6h6"/>
      <path d="M9 13h6"/>
      <path d="M9 17h4"/>
    </svg>
  );
}

function IncomeIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 2v20"/>
      <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
    </svg>
  );
}

function CashIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="6" width="20" height="14" rx="2"/>
      <path d="M6 6V4a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v2"/>
      <circle cx="12" cy="13" r="2"/>
      <path d="M2 13h3"/>
      <path d="M19 13h3"/>
    </svg>
  );
}

function ProfileIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
      <circle cx="12" cy="7" r="4"/>
    </svg>
  );
}