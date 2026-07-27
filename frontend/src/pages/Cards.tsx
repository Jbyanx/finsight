import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../lib/api';
import { Field } from './Login';
import { UtilizationBar } from '../components/UtilizationBar';
import { InlineAlert } from '../components/InlineAlert';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { errorMessage } from '../lib/errorMessage';
import { money } from '../lib/money';
import { recommendedPayment, formatNextOccurrence } from '../lib/cardHealth';
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
  const [pendingDelete, setPendingDelete] = useState<CreditCard | null>(null);

  async function load() {
    setError(null);
    try {
      const data = await api.get<CreditCard[]>('/api/v1/cards');
      setCards(data);
    } catch (err) {
      setError(errorMessage(err, 'No pudimos cargar tus tarjetas.'));
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

  async function confirmDelete(card: CreditCard) {
    setPendingDelete(null);
    setError(null);
    setDeletingId(card.id);
    try {
      await api.del('/api/v1/cards/' + card.id);
      setCards((prev) => (prev ? prev.filter((c) => c.id !== card.id) : prev));
    } catch (err) {
      setError(errorMessage(err, 'No pudimos eliminar la tarjeta.'));
    } finally {
      setDeletingId(null);
    }
  }

  if (error && !cards) {
    return (
      <main>
        <InlineAlert message={error} />
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
        <div style={{ marginTop: 16 }}>
          <InlineAlert message={error} />
        </div>
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
          {cards.map((c) => {
            const editing = editingId === c.id;
            const busy = deletingId === c.id;
            return (
              <div key={c.id} className="card">
                <p style={{ fontWeight: 600, fontSize: 15, marginBottom: 4 }}>
                  {c.nickname || c.institution}
                </p>
                <p className="eyebrow" style={{ marginBottom: 12 }}>
                  Corte el {c.cutOffDay} · pago el {c.paymentDueDay}
                </p>
                {/* Mientras se edita, el formulario ocupa el lugar de la barra. */}
                {!editing && (
                  <>
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
                  </>
                )}
                {/* Editar abierto → botón ACTIVO (relleno --ink) y Eliminar
                    atenuado, mismo patrón que Savings. El activo cierra al tocar. */}
                <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
                  <button
                    className="btn btn-ghost"
                    onClick={() => setEditingId(editing ? null : c.id)}
                    disabled={busy}
                    style={editing ? activeBtn : smallBtn}
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-ghost"
                    onClick={() => setPendingDelete(c)}
                    disabled={busy}
                    style={editing ? dimBtn : { ...smallBtn, color: 'var(--danger)' }}
                  >
                    {busy ? 'Eliminando…' : 'Eliminar'}
                  </button>
                </div>
                {editing && (
                  <CardForm
                    embedded
                    initial={c}
                    onCancel={() => setEditingId(null)}
                    onSaved={(card) => {
                      upsert(card);
                      setEditingId(null);
                    }}
                  />
                )}
              </div>
            );
          })}
        </section>
      )}

      {pendingDelete && (
        <ConfirmDialog
          title="Eliminar tarjeta"
          message={'¿Eliminar "' + (pendingDelete.nickname || pendingDelete.institution) + '"?'}
          confirmLabel="Eliminar tarjeta"
          tone="danger"
          onConfirm={() => confirmDelete(pendingDelete)}
          onCancel={() => setPendingDelete(null)}
        />
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
  embedded,
  onCancel,
  onSaved
}: {
  initial?: CreditCard;
  embedded?: boolean;
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
      setError(errorMessage(err, 'No pudimos guardar la tarjeta.'));
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={onSubmit}
      className={embedded ? undefined : 'card'}
      style={
        embedded
          ? { marginTop: 14, paddingTop: 14, borderTop: '1px solid var(--line)',
              display: 'grid', gap: 12 }
          : { marginTop: 22, display: 'grid', gap: 12 }
      }
    >
      {editing ? (
        <div style={{ display: 'grid', gap: 6 }}>
          <span className="eyebrow">Institución</span>
          <p style={{ fontWeight: 600, fontSize: 15 }}>{institution}</p>
        </div>
      ) : (
        <>
          <p style={{ fontSize: 12, color: 'var(--muted)', lineHeight: 1.4 }}>
            Esto es para tarjetas de <strong style={{ color: 'var(--ink)' }}>crédito</strong> —
            con cupo, corte y utilización. Si es una tarjeta débito o cuenta bancaria,
            regístrala en <strong style={{ color: 'var(--ink)' }}>Cuentas</strong>.
          </p>
          <Field label="Institución" type="text" value={institution} onChange={setInstitution}
                placeholder="Bancolombia" />
        </>
      )}
      <Field label="Apodo (opcional)" type="text" value={nickname} onChange={setNickname}
             placeholder="Mi tarjeta de viajes" />
      <Field label="Cupo" type="number" value={creditLimit} onChange={setCreditLimit}
             placeholder="2000000" hint="Debe ser mayor a 0." />
      <Field label="Día de corte" type="number" value={cutOffDay} onChange={setCutOffDay}
             placeholder="15" hint="Entre 1 y 31." />
      <Field label="Día de pago" type="number" value={paymentDueDay} onChange={setPaymentDueDay}
             placeholder="5" hint="Entre 1 y 31." />

      {error && <InlineAlert message={error} />}

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

// Mismos estilos de botón que Savings, por coherencia entre pantallas.
const smallBtn = {
  width: 'auto',
  padding: '6px 12px',
  fontSize: 13,
  borderColor: 'var(--line)'
} as const;

/** Botón en estado activo: relleno --ink, texto claro. La acción abierta. */
const activeBtn = {
  ...smallBtn,
  background: 'var(--ink)',
  color: 'var(--paper)',
  borderColor: 'var(--ink)'
} as const;

/** Botón atenuado: recede mientras hay otra acción abierta. */
const dimBtn = {
  ...smallBtn,
  color: 'var(--muted)',
  opacity: 0.55
} as const;
