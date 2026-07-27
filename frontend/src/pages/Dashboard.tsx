import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import { useAuth } from '../auth/AuthContext';
import { money, healthOf } from '../lib/money';
import { UtilizationBar } from '../components/UtilizationBar';
import { recommendedPayment, formatNextOccurrence } from '../lib/cardHealth';
import type { CreditCard, FinancialSummary } from '../types';

/**
 * Esta pantalla tiene UN trabajo: responder "¿cómo estoy y con qué pago hoy?"
 * en tres segundos, en un teléfono. Todo lo que no sirva a esa pregunta va en
 * otra pantalla.
 */
export default function Dashboard() {
  const { user, logout } = useAuth();
  const [summary, setSummary] = useState<FinancialSummary | null>(null);
  const [cards, setCards] = useState<CreditCard[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([
      api.get<FinancialSummary>('/api/v1/financial/summary'),
      api.get<CreditCard[]>('/api/v1/cards/by-utilization')
    ])
      .then(([s, c]) => {
        setSummary(s);
        setCards(c);
      })
      .catch((e) => setError(e.message));
  }, []);

  if (error) {
    return (
      <div className="card" style={{ borderColor: 'var(--danger)', color: 'var(--danger)' }}>
        No pudimos cargar tus datos. {error}
      </div>
    );
  }

  if (!summary || !cards) {
    return <p style={{ color: 'var(--muted)' }}>Cargando…</p>;
  }

  const best = cards[0]; // ya vienen ordenadas por utilización ascendente
const health = healthOf(summary.overallUtilizationPercentage);
  const firstName = user?.name ? user.name.split(' ')[0] : '';

  return (
    <main>
      <header
        style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
      >
        <p className="eyebrow">Hola, {firstName}</p>
        <button
          className="btn btn-ghost"
          onClick={logout}
          style={{
            width: 'auto',
            padding: '6px 10px',
            fontSize: 13,
            borderColor: 'var(--line)'
          }}
        >
          Salir
        </button>
      </header>

      {/* El titular es DINERO, no el porcentaje: lo usado entre tarjetas es la
          cifra grande; el % es contexto de salud a un lado. Y el disponible
          —"cuánto puedo gastar"— siempre a la vista. */}
      <section style={{ marginTop: 26 }}>
        <p className="eyebrow">Usado en tarjetas</p>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'baseline',
            gap: 12,
            marginTop: 6
          }}
        >
          <h1
            className="amount"
            style={{
              fontFamily: 'var(--display)',
              fontSize: 44,
              fontWeight: 800,
              letterSpacing: '-0.04em',
              lineHeight: 1
            }}
          >
            {money(summary.totalUsedAmount)}
          </h1>
          <span
            className="amount"
            style={{ fontSize: 22, fontWeight: 700, color: 'var(--' + health + ')' }}
          >
            {summary.overallUtilizationPercentage.toFixed(1)}%
          </span>
        </div>
        <p style={{ fontSize: 15, marginTop: 12 }}>
          <span style={{ color: 'var(--muted)' }}>Disponible </span>
          <span className="amount" style={{ fontWeight: 700, fontSize: 16 }}>
            {money(summary.totalAvailableCredit)}
          </span>
        </p>
        <p style={{ color: 'var(--muted)', fontSize: 14, marginTop: 8 }}>
          {health === 'safe' && 'Tu historial crediticio está protegido.'}
          {health === 'warn' && 'Te estás acercando al 30%. Conviene abonar pronto.'}
          {health === 'danger' && 'Pasaste el 30%. Esto puede afectar tu historial.'}
        </p>
      </section>

      {/* La respuesta a "¿con qué pago hoy?" — la razón de ser de la app. */}
      {best && (
        <section
          className="card"
          style={{
            marginTop: 22,
            borderColor: 'var(--' + healthOf(best.utilizationPercentage) + ')'
          }}
        >
          <p className="eyebrow">Paga con esta</p>
          <p
            style={{
              fontFamily: 'var(--display)',
              fontSize: 24,
              fontWeight: 600,
              marginTop: 4,
              marginBottom: 14
            }}
          >
            {best.nickname || best.institution}
          </p>
          <UtilizationBar
            used={best.usedAmount}
            limit={best.creditLimit}
            available={best.availableAmount}
            utilization={best.utilizationPercentage}
          />
          <p className="amount" style={{ fontSize: 13, color: 'var(--muted)', marginTop: 12 }}>
            Corte el {best.cutOffDay}
          </p>
          {best.utilizationStatus !== 'SAFE' && (
            <p style={{ marginTop: 8, fontSize: 13, color: 'var(--danger)', fontWeight: 600 }}>
              Paga {money(recommendedPayment(best))} antes del corte ({formatNextOccurrence(best.cutOffDay)})
              para no facturar por encima del 30% de tu cupo.
            </p>
          )}
        </section>
      )}

      <section style={{ marginTop: 26 }}>
        <p className="eyebrow" style={{ marginBottom: 12 }}>
          Tus tarjetas
        </p>
        <div style={{ display: 'grid', gap: 12 }}>
          {cards.map((c) => (
            <div key={c.id} className="card">
              <p style={{ fontWeight: 600, fontSize: 15, marginBottom: 12 }}>
                {c.nickname || c.institution}
              </p>
              <UtilizationBar
                used={c.usedAmount}
                limit={c.creditLimit}
                available={c.availableAmount}
                utilization={c.utilizationPercentage}
              />
              {c.utilizationStatus !== 'SAFE' && (
                <p style={{ marginTop: 8, fontSize: 13, color: 'var(--danger)', fontWeight: 600 }}>
                  Paga {money(recommendedPayment(c))} antes del corte ({formatNextOccurrence(c.cutOffDay)})
                  para no facturar por encima del 30% de tu cupo.
                </p>
              )}
            </div>
          ))}
          {cards.length === 0 && (
            <Link to="/cards" className="card" style={{
              color: 'var(--muted)', fontSize: 14, textDecoration: 'none', display: 'block'
            }}>
              Todavía no tienes tarjetas.{' '}
              <span style={{ color: 'var(--ink)', fontWeight: 600 }}>Agrega una</span> para ver
              tu utilización.
            </Link>
          )}
        </div>
      </section>

      <section style={{ marginTop: 26, display: 'grid', gap: 12 }}>
        <Row label="Efectivo" value={money(summary.totalCashBalance)} />
        <Row label="Ahorro" value={money(summary.totalSavingsBalance)} />
        <Row label="Deuda" value={money(summary.totalRemainingDebt)} danger to="/debts" />
      </section>

    </main>
  );
}

function Row({ label, value, danger, to }: { label: string; value: string; danger?: boolean; to?: string }) {
  const style = {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'baseline',
    paddingBottom: 12,
    borderBottom: '1px solid var(--line)'
  } as const;
  const inner = (
    <>
      <span style={{ color: 'var(--muted)', fontSize: 14 }}>{label}</span>
      <span
        className="amount"
        style={{
          fontSize: 16,
          fontWeight: 500,
          color: danger ? 'var(--danger)' : 'var(--ink)'
        }}
      >
        {value}
      </span>
    </>
  );
  if (to) {
    return (
      <Link to={to} style={{ ...style, textDecoration: 'none', color: 'inherit' }}>
        {inner}
      </Link>
    );
  }
  return <div style={style}>{inner}</div>;
}