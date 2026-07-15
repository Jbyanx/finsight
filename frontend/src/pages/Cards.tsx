import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api, ApiError } from '../lib/api';
import { Field } from './Login';
import { UtilizationBar } from '../components/UtilizationBar';
import type { CreditCard } from '../types';

/** Espeja CreditCardRequest del backend. nickname es opcional (puede ir ""). */
interface CreditCardRequest {
  institution: string;
  nickname: string;
  creditLimit: number;
  cutOffDay: number;
  paymentDueDay: number;
}

export default function Cards() {
  const [cards, setCards] = useState<CreditCard[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);

  async function load() {
    setError(null);
    try {
      const data = await api.get<CreditCard[]>('/api/v1/cards');
      setCards(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos cargar tus tarjetas.');
    }
  }

  useEffect(() => {
    load();
  }, []);

  if (error && !cards) {
    return (
      <main>
        <div className="card" style={{ borderColor: 'var(--danger)', color: 'var(--danger)' }}>
          {error}
        </div>
      </main>
    );
  }

  if (!cards) {
    return (
      <main>
        <p style={{ color: 'var(--muted)' }}>Cargando…</p>
      </main>
    );
  }

  return (
    <main>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <p className="eyebrow">Tus tarjetas</p>
        {!adding && (
          <button
            className="btn btn-ghost"
            onClick={() => setAdding(true)}
            style={{ width: 'auto', padding: '6px 10px', fontSize: 13, borderColor: 'var(--line)' }}
          >
            Agregar tarjeta
          </button>
        )}
      </header>

      {adding && (
        <CardForm
          onCancel={() => setAdding(false)}
          onCreated={(card) => {
            setCards((prev) => (prev ? [...prev, card] : [card]));
            setAdding(false);
          }}
        />
      )}

      {!adding && cards.length === 0 && (
        <div
          className="card"
          style={{ marginTop: 22, color: 'var(--muted)', fontSize: 14, lineHeight: 1.5 }}
        >
          Todavía no tienes tarjetas. Agrega la primera para ver tu utilización y
          saber con qué pagar hoy.
        </div>
      )}

      {cards.length > 0 && (
        <section style={{ marginTop: 22, display: 'grid', gap: 12 }}>
          {cards.map((c) => (
            <div key={c.id} className="card">
              <p style={{ fontWeight: 600, fontSize: 15, marginBottom: 4 }}>
                {c.nickname || c.institution}
              </p>
              <p className="eyebrow" style={{ marginBottom: 12 }}>
                Corte el {c.cutOffDay} · pago el {c.paymentDueDay}
              </p>
              <UtilizationBar
                used={c.usedAmount}
                limit={c.creditLimit}
                utilization={c.utilizationPercentage}
              />
            </div>
          ))}
        </section>
      )}
    </main>
  );
}

/**
 * Formulario de alta. Valida en el front lo mismo que el backend: institución no
 * vacía, límite >= 0.01, y los dos días entre 1 y 31. El submit queda inhabilitado
 * hasta que todo cumple.
 */
function CardForm({
  onCancel,
  onCreated
}: {
  onCancel: () => void;
  onCreated: (card: CreditCard) => void;
}) {
  const [institution, setInstitution] = useState('');
  const [nickname, setNickname] = useState('');
  const [creditLimit, setCreditLimit] = useState('');
  const [cutOffDay, setCutOffDay] = useState('');
  const [paymentDueDay, setPaymentDueDay] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const limitNum = Number(creditLimit);
  const cutNum = Number(cutOffDay);
  const dueNum = Number(paymentDueDay);

  const validDay = (n: number) => Number.isInteger(n) && n >= 1 && n <= 31;
  const valid =
    institution.trim() !== '' &&
    creditLimit.trim() !== '' &&
    limitNum >= 0.01 &&
    validDay(cutNum) &&
    validDay(dueNum);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      const body: CreditCardRequest = {
        institution: institution.trim(),
        nickname: nickname.trim(),
        creditLimit: limitNum,
        cutOffDay: cutNum,
        paymentDueDay: dueNum
      };
      const card = await api.post<CreditCard>('/api/v1/cards', body);
      onCreated(card);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos crear la tarjeta.');
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="card" style={{ marginTop: 22, display: 'grid', gap: 12 }}>
      <Field label="Institución" type="text" value={institution} onChange={setInstitution}
             placeholder="Bancolombia" />
      <Field label="Apodo (opcional)" type="text" value={nickname} onChange={setNickname}
             placeholder="Mi tarjeta de viajes" />
      <Field label="Cupo" type="number" value={creditLimit} onChange={setCreditLimit}
             placeholder="2000000" hint="Debe ser mayor a 0." />
      <Field label="Día de corte" type="number" value={cutOffDay} onChange={setCutOffDay}
             placeholder="15" hint="Entre 1 y 31." />
      <Field label="Día de pago" type="number" value={paymentDueDay} onChange={setPaymentDueDay}
             placeholder="5" hint="Entre 1 y 31." />

      {error && (
        <p role="alert" style={{ color: 'var(--danger)', background: 'var(--danger-soft)',
             border: '1px solid var(--danger)', borderRadius: 10, padding: '10px 12px',
             fontSize: 13, lineHeight: 1.4 }}>
          {error}
        </p>
      )}

      <button className="btn" type="submit" disabled={busy || !valid}
              style={{ marginTop: 4, opacity: busy || !valid ? 0.5 : 1 }}>
        {busy ? 'Guardando…' : 'Guardar tarjeta'}
      </button>
      <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}
              style={{ borderColor: 'var(--line)' }}>
        Cancelar
      </button>
    </form>
  );
}
