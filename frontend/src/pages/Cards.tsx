import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api, ApiError } from '../lib/api';
import { Field } from './Login';
import { UtilizationBar } from '../components/UtilizationBar';
import type { CreditCard } from '../types';

/** Alta: espeja CreditCardRequest. institution solo se define al crear. */
interface CreditCardRequest {
  institution: string;
  nickname: string;
  creditLimit: number;
  cutOffDay: number;
  paymentDueDay: number;
}

/** Edición: espeja CreditCardPatchRequest. institution NO es editable. */
interface CreditCardPatchRequest {
  nickname: string;
  creditLimit: number;
  cutOffDay: number;
  paymentDueDay: number;
}

export default function Cards() {
  const [cards, setCards] = useState<CreditCard[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

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

  function upsert(card: CreditCard) {
    setCards((prev) => {
      if (!prev) return [card];
      const idx = prev.findIndex((c) => c.id === card.id);
      if (idx === -1) return [...prev, card];
      const next = [...prev];
      next[idx] = card;
      return next;
    });
  }

  async function remove(card: CreditCard) {
    if (!confirm('¿Eliminar esta tarjeta?')) return;
    setError(null);
    setDeletingId(card.id);
    try {
      await api.del('/api/v1/cards/' + card.id);
      setCards((prev) => (prev ? prev.filter((c) => c.id !== card.id) : prev));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos eliminar la tarjeta.');
    } finally {
      setDeletingId(null);
    }
  }

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

      {error && cards && (
        <p role="alert" style={{ marginTop: 16, color: 'var(--danger)',
             background: 'var(--danger-soft)', border: '1px solid var(--danger)',
             borderRadius: 10, padding: '10px 12px', fontSize: 13, lineHeight: 1.4 }}>
          {error}
        </p>
      )}

      {adding && (
        <CardForm
          onCancel={() => setAdding(false)}
          onSaved={(card) => {
            upsert(card);
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
          {cards.map((c) =>
            editingId === c.id ? (
              <CardForm
                key={c.id}
                initial={c}
                onCancel={() => setEditingId(null)}
                onSaved={(card) => {
                  upsert(card);
                  setEditingId(null);
                }}
              />
            ) : (
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
                <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
                  <button
                    className="btn btn-ghost"
                    onClick={() => setEditingId(c.id)}
                    disabled={deletingId === c.id}
                    style={{ width: 'auto', padding: '6px 12px', fontSize: 13,
                             borderColor: 'var(--line)' }}
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-ghost"
                    onClick={() => remove(c)}
                    disabled={deletingId === c.id}
                    style={{ width: 'auto', padding: '6px 12px', fontSize: 13,
                             borderColor: 'var(--line)', color: 'var(--danger)' }}
                  >
                    {deletingId === c.id ? 'Eliminando…' : 'Eliminar'}
                  </button>
                </div>
              </div>
            )
          )}
        </section>
      )}
    </main>
  );
}

/**
 * Formulario único para crear y editar. Sin `initial` es alta (POST, con
 * institución); con `initial` es edición (PATCH), donde la institución se muestra
 * fija porque el backend no la deja cambiar.
 *
 * Valida lo mismo que el backend: límite >= 0.01 y los dos días entre 1 y 31
 * (más institución no vacía al crear). El submit queda inhabilitado hasta cumplir.
 */
function CardForm({
  initial,
  onCancel,
  onSaved
}: {
  initial?: CreditCard;
  onCancel: () => void;
  onSaved: (card: CreditCard) => void;
}) {
  const editing = initial != null;

  const [institution, setInstitution] = useState(initial?.institution ?? '');
  const [nickname, setNickname] = useState(initial?.nickname ?? '');
  const [creditLimit, setCreditLimit] = useState(
    initial ? String(initial.creditLimit) : ''
  );
  const [cutOffDay, setCutOffDay] = useState(initial ? String(initial.cutOffDay) : '');
  const [paymentDueDay, setPaymentDueDay] = useState(
    initial ? String(initial.paymentDueDay) : ''
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const limitNum = Number(creditLimit);
  const cutNum = Number(cutOffDay);
  const dueNum = Number(paymentDueDay);

  const validDay = (n: number) => Number.isInteger(n) && n >= 1 && n <= 31;
  const valid =
    (editing || institution.trim() !== '') &&
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
      let card: CreditCard;
      if (editing) {
        const body: CreditCardPatchRequest = {
          nickname: nickname.trim(),
          creditLimit: limitNum,
          cutOffDay: cutNum,
          paymentDueDay: dueNum
        };
        card = await api.patch<CreditCard>('/api/v1/cards/' + initial.id, body);
      } else {
        const body: CreditCardRequest = {
          institution: institution.trim(),
          nickname: nickname.trim(),
          creditLimit: limitNum,
          cutOffDay: cutNum,
          paymentDueDay: dueNum
        };
        card = await api.post<CreditCard>('/api/v1/cards', body);
      }
      onSaved(card);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos guardar la tarjeta.');
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="card" style={{ marginTop: 22, display: 'grid', gap: 12 }}>
      {editing ? (
        <div style={{ display: 'grid', gap: 6 }}>
          <span className="eyebrow">Institución</span>
          <p style={{ fontWeight: 600, fontSize: 15 }}>{institution}</p>
        </div>
      ) : (
        <Field label="Institución" type="text" value={institution} onChange={setInstitution}
               placeholder="Bancolombia" />
      )}
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
        {busy ? 'Guardando…' : editing ? 'Guardar cambios' : 'Guardar tarjeta'}
      </button>
      <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}
              style={{ borderColor: 'var(--line)' }}>
        Cancelar
      </button>
    </form>
  );
}
