import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../lib/api';
import { Field } from './Login';
import { Select } from '../components/Select';
import { BlockingWarning } from '../components/BlockingWarning';
import { money } from '../lib/money';
import { errorMessage } from '../lib/errorMessage';
import { InlineAlert } from '../components/InlineAlert';
import { ConfirmDialog } from '../components/ConfirmDialog';
import type { FixedDebt, SavingsAccount, Pocket, Cash } from '../types';

/* ============================================================
   Deudas fijas (créditos que se pagan en abonos).

   El eje del diseño: la deuda es lo contrario de una tarjeta. progressPercentage
   mide cuánto se ha PAGADO; llegar al 100% es BUENO (deuda saldada). Por eso la
   barra es verde y celebra el avance — NUNCA el código de peligro del 30%. La
   cifra protagonista es el saldo restante: "cuánto me falta".

   El pago de una deuda es dinero real que sale de algún lado. El backend
   soporta tres orígenes: CASH, SAVINGS, o ninguno (alguien más pagó fuera de
   la app — se registra solo contablemente). Pagar con tarjeta no está
   permitido: eso mueve la deuda de lugar, no la salda.
   ============================================================ */

/** POST deuda: espeja el request de alta. */
interface DebtRequest {
  institution: string;
  description: string;
  totalAmount: number;
  monthlyPayment: number;
  interestRate: number;
  paymentDueDay: number;
  remainingBalance: number;
}

/** PATCH deuda: institution, totalAmount y remainingBalance NO se editan. */
interface DebtPatchRequest {
  description: string;
  monthlyPayment: number;
  interestRate: number;
  paymentDueDay: number;
}

/** POST pago: espeja DebtPaymentRequest. originType solo CASH o SAVINGS. */
type PaymentOrigin = 'NONE' | 'CASH' | 'SAVINGS';
interface DebtPaymentBody {
  amount: number;
  originType?: 'CASH' | 'SAVINGS';
  savingsAccountId?: string;
  pocketId?: string;
}

export default function Debts() {
  const [debts, setDebts] = useState<FixedDebt[] | null>(null);
  const [accounts, setAccounts] = useState<SavingsAccount[]>([]);
  const [cash, setCash] = useState<Cash | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<FixedDebt | null>(null);

  async function load() {
    setError(null);
    try {
      setDebts(await api.get<FixedDebt[]>('/api/v1/debts'));
    } catch (err) {
      setError(errorMessage(err, 'No pudimos cargar tus deudas.'));
    }
  }

  async function refreshOrigins() {
    const [a, k] = await Promise.all([
      api.get<SavingsAccount[]>('/api/v1/savings').catch(() => []),
      api.get<Cash>('/api/v1/cash').catch(() => null)
    ]);
    setAccounts(a);
    setCash(k);
  }

  useEffect(() => {
    load();
    refreshOrigins();
  }, []);

  function upsert(debt: FixedDebt) {
    setDebts((prev) => {
      if (!prev) return [debt];
      const idx = prev.findIndex((d) => d.id === debt.id);
      if (idx === -1) return [...prev, debt];
      const next = [...prev];
      next[idx] = debt;
      return next;
    });
  }

  async function confirmDelete(debt: FixedDebt) {
    setPendingDelete(null);
    setError(null);
    setDeletingId(debt.id);
    try {
      await api.del('/api/v1/debts/' + debt.id);
      setDebts((prev) => (prev ? prev.filter((d) => d.id !== debt.id) : prev));
    } catch (err) {
      setError(errorMessage(err, 'No pudimos eliminar la deuda.'));
    } finally {
      setDeletingId(null);
    }
  }

  if (error && !debts) {
    return (
      <main>
        <InlineAlert message={error} />
      </main>
    );
  }

  if (!debts) {
    return (
      <main>
        <p style={{ color: 'var(--muted)' }}>Cargando…</p>
      </main>
    );
  }

  return (
    <main>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <p className="eyebrow">Tus deudas</p>
        {!adding && (
          <button
            className="btn btn-ghost"
            onClick={() => setAdding(true)}
            style={{ width: 'auto', padding: '6px 10px', fontSize: 13, borderColor: 'var(--line)' }}
          >
            Agregar deuda
          </button>
        )}
      </header>

      {error && debts && (
        <div style={{ marginTop: 16 }}>
          <InlineAlert message={error} />
        </div>
      )}

      {adding && (
        <DebtForm
          onCancel={() => setAdding(false)}
          onSaved={(d) => {
            upsert(d);
            setAdding(false);
          }}
        />
      )}

      {!adding && debts.length === 0 && (
        <div className="card" style={{ marginTop: 22, color: 'var(--muted)', fontSize: 14, lineHeight: 1.5 }}>
          Todavía no tienes deudas registradas. Agrega la primera para seguir cuánto
          te falta y celebrar cuando la saldes.
        </div>
      )}

      <section style={{ marginTop: 22, display: 'grid', gap: 12 }}>
        {debts.map((d) => (
          <DebtCard
            key={d.id}
            debt={d}
            accounts={accounts}
            cash={cash}
            editing={editingId === d.id}
            deleting={deletingId === d.id}
            onEdit={() => setEditingId(d.id)}
            onStopEdit={() => setEditingId(null)}
            onSaved={(fresh) => {
              upsert(fresh);
              setEditingId(null);
            }}
            onPaid={(fresh) => {
              upsert(fresh);
              refreshOrigins();
            }}
            onDelete={() => setPendingDelete(d)}
          />
        ))}
      </section>

      {pendingDelete && (
        <ConfirmDialog
          title="Eliminar deuda"
          message={'¿Eliminar "' + (pendingDelete.description || pendingDelete.institution) + '"?'}
          confirmLabel="Eliminar deuda"
          tone="danger"
          onConfirm={() => confirmDelete(pendingDelete)}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </main>
  );
}

function DebtCard({
  debt,
  accounts,
  cash,
  editing,
  deleting,
  onEdit,
  onStopEdit,
  onSaved,
  onPaid,
  onDelete
}: {
  debt: FixedDebt;
  accounts: SavingsAccount[];
  cash: Cash | null;
  editing: boolean;
  deleting: boolean;
  onEdit: () => void;
  onStopEdit: () => void;
  onSaved: (debt: FixedDebt) => void;
  onPaid: (debt: FixedDebt) => void;
  onDelete: () => void;
}) {
  // El abono abre un input de MONTO debajo, patrón de Savings. No coexiste con la
  // edición: cada uno deshabilita al otro.
  const [paying, setPaying] = useState(false);

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div>
          <p style={{ fontWeight: 600, fontSize: 15 }}>{debt.description || debt.institution}</p>
          {debt.description && (
            <p className="eyebrow" style={{ marginTop: 2 }}>{debt.institution}</p>
          )}
        </div>
        {debt.paidOff && (
          <span
            className="amount"
            style={{
              fontSize: 12,
              fontWeight: 700,
              color: 'var(--safe)',
              background: 'var(--safe-soft)',
              border: '1px solid var(--safe)',
              borderRadius: 999,
              padding: '2px 10px',
              whiteSpace: 'nowrap'
            }}
          >
            ✓ Saldada
          </span>
        )}
      </div>

      {/* Cifra protagonista: cuánto falta. Si está saldada, mensaje positivo. */}
      <div style={{ marginTop: 14 }}>
        {debt.paidOff ? (
          <>
            <span className="eyebrow">Deuda saldada</span>
            <p className="amount" style={{ fontSize: 22, fontWeight: 800, marginTop: 2, color: 'var(--safe)' }}>
              ¡Pagaste todo!
            </p>
          </>
        ) : (
          <>
            <span className="eyebrow">Te falta</span>
            <p className="amount" style={{ fontSize: 28, fontWeight: 800, marginTop: 2 }}>
              {money(debt.remainingBalance)}
            </p>
          </>
        )}
      </div>

      <DebtProgressBar
        paid={debt.paidAmount}
        total={debt.totalAmount}
        progress={debt.progressPercentage}
      />

      <div style={{ display: 'flex', gap: 8, marginTop: 14, flexWrap: 'wrap' }}>
        {!debt.paidOff && (
          <button
            className="btn btn-ghost"
            onClick={() => setPaying((p) => !p)}
            disabled={deleting || editing}
            style={paying ? activeBtn : editing ? dimBtn : smallBtn}
          >
            Registrar pago
          </button>
        )}
        <button
          className="btn btn-ghost"
          onClick={() => (editing ? onStopEdit() : onEdit())}
          disabled={deleting || paying}
          style={editing ? activeBtn : paying ? dimBtn : smallBtn}
        >
          Editar
        </button>
        <button
          className="btn btn-ghost"
          onClick={onDelete}
          disabled={deleting}
          style={paying || editing ? dimBtn : { ...smallBtn, color: 'var(--danger)' }}
        >
          {deleting ? 'Eliminando…' : 'Eliminar'}
        </button>
      </div>

      {paying && !editing && (
        <DebtPaymentForm
          debt={debt}
          accounts={accounts}
          cash={cash}
          onCancel={() => setPaying(false)}
          onDone={(fresh) => {
            setPaying(false);
            onPaid(fresh);
          }}
        />
      )}

      {editing && (
        <DebtForm embedded initial={debt} onCancel={onStopEdit} onSaved={onSaved} />
      )}
    </div>
  );
}

/**
 * Barra de progreso de deuda. Como la meta de ahorro (GoalBar), NO como
 * UtilizationBar: aquí el 100% es lo bueno (deuda saldada), se llena en verde
 * sobrio y no hay umbral de peligro ni color de alerta.
 */
function DebtProgressBar({ paid, total, progress }: { paid: number; total: number; progress: number }) {
  const fill = Math.min(Math.max(progress, 0), 100);
  return (
    <div style={{ marginTop: 12 }}>
      <div style={{ position: 'relative', height: 10, borderRadius: 999, background: 'var(--line)' }}>
        <div
          style={{
            width: fill + '%',
            height: '100%',
            borderRadius: 999,
            background: 'var(--safe)',
            transition: 'width .4s ease'
          }}
        />
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }}>
        <span style={{ fontSize: 12, color: 'var(--muted)' }}>Pagado {money(paid)}</span>
        <span style={{ fontSize: 12, color: 'var(--muted)' }}>Total {money(total)}</span>
      </div>
    </div>
  );
}

/**
 * Registrar un abono, con de dónde sale la plata.
 *
 * Tres orígenes posibles:
 *   - "Nadie" (originType null): alguien pagó fuera de la app, el pago
 *     solo se registra contablemente en la deuda.
 *   - Efectivo: valida contra el saldo real de cash, igual que en
 *     Transacciones — sin eso, el botón quedaría habilitado con plata
 *     que no existe.
 *   - Cuenta (+ bolsillo opcional): valida contra el disponible de la
 *     cuenta elegida.
 *
 * Tarjeta NO es una opción aquí: el backend la rechaza porque pagar una
 * deuda con tarjeta mueve la deuda de lugar, no la salda.
 */
function DebtPaymentForm({
  debt,
  accounts,
  cash,
  onCancel,
  onDone
}: {
  debt: FixedDebt;
  accounts: SavingsAccount[];
  cash: Cash | null;
  onCancel: () => void;
  onDone: (debt: FixedDebt) => void;
}) {
  const [amount, setAmount] = useState('');
  const [origin, setOrigin] = useState<PaymentOrigin>('NONE');
  const [savingsAccountId, setSavingsAccountId] = useState(accounts[0]?.id ?? '');
  const [pocketId, setPocketId] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedAccount = accounts.find((a) => a.id === savingsAccountId);
  const pockets: Pocket[] = selectedAccount?.pockets ?? [];

  const amountNum = Number(amount);
  const amountEntered = amount.trim() !== '' && amountNum > 0;
  const exceedsDebt = amountEntered && amountNum > debt.remainingBalance;

  /* Motivo de bloqueo — el mismo lenguaje que en Transacciones: sin
     efectivo suficiente, sin cuentas registradas, o el monto excede lo
     que falta de la deuda. Único punto de verdad para habilitar el submit. */
  let originWarning: string | null = null;
  if (exceedsDebt) {
    originWarning = `Ese monto supera lo que falta (${money(debt.remainingBalance)}).`;
  } else if (origin === 'CASH') {
    if (!cash || cash.balance <= 0) {
      originWarning = 'No tienes efectivo registrado.';
    } else if (amountEntered && amountNum > cash.balance) {
      originWarning = `Ese monto supera tu efectivo disponible (${money(cash.balance)}).`;
    }
  } else if (origin === 'SAVINGS') {
    if (accounts.length === 0) {
      originWarning = 'No tienes cuentas de ahorro registradas.';
    } else if (selectedAccount && amountEntered && amountNum > selectedAccount.availableBalance) {
      originWarning = `Ese monto supera el disponible de la cuenta (${money(selectedAccount.availableBalance)}).`;
    }
  }

  const valid =
    amountEntered &&
    originWarning === null &&
    (origin !== 'SAVINGS' || savingsAccountId !== '');

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      const body: DebtPaymentBody = {
        amount: amountNum,
        originType: origin === 'NONE' ? undefined : origin,
        savingsAccountId: origin === 'SAVINGS' ? savingsAccountId : undefined,
        pocketId: origin === 'SAVINGS' && pocketId ? pocketId : undefined
      };
      const fresh = await api.post<FixedDebt>('/api/v1/debts/' + debt.id + '/payments', body);
      onDone(fresh);
    } catch (err) {
      setError(errorMessage(err, 'No pudimos registrar el pago.'));
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={onSubmit}
      style={{ marginTop: 10, display: 'grid', gap: 10, border: '1px solid var(--line)',
               borderRadius: 10, padding: 12 }}
    >
      <p style={{ fontWeight: 600, fontSize: 14 }}>¿Cuánto quieres abonar?</p>
      <Field
        label="Monto"
        type="number"
        value={amount}
        onChange={setAmount}
        placeholder="200000"
        hint={'Te falta ' + money(debt.remainingBalance) + '.'}
      />

      <div style={{ display: 'grid', gap: 6 }}>
        <span className="eyebrow">¿De dónde sale?</span>
        <div style={{ display: 'flex', gap: 6 }}>
          {(['NONE', 'CASH', 'SAVINGS'] as const).map((o) => (
            <button
              key={o}
              type="button"
              className="btn btn-ghost"
              onClick={() => setOrigin(o)}
              style={{ flex: 1, ...(origin === o ? activeBtn : smallBtn) }}
            >
              {ORIGIN_OPTION_LABELS[o]}
            </button>
          ))}
        </div>
        {origin === 'NONE' && (
          <span style={{ fontSize: 12, color: 'var(--muted)' }}>
            Pagó alguien más fuera de la app: solo se registra en la deuda.
          </span>
        )}
      </div>

      {origin === 'CASH' && cash && cash.balance > 0 && (
        <p style={{ fontSize: 13, color: 'var(--muted)' }}>
          Disponible en efectivo: <strong style={{ color: 'var(--ink)' }}>{money(cash.balance)}</strong>
        </p>
      )}

      {origin === 'SAVINGS' && accounts.length > 0 && (
        <>
          <Select
            label="Cuenta"
            value={savingsAccountId}
            onChange={(v) => { setSavingsAccountId(v); setPocketId(''); }}
          >
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {(a.nickname || a.institution)} — {money(a.availableBalance)}
              </option>
            ))}
          </Select>

          {pockets.length > 0 && (
            <Select label="Bolsillo (opcional)" value={pocketId} onChange={setPocketId}>
              <option value="">Ninguno</option>
              {pockets.map((p) => (
                <option key={p.id} value={p.id}>{p.name} — {money(p.balance)}</option>
              ))}
            </Select>
          )}
        </>
      )}

      {originWarning && <BlockingWarning message={originWarning} />}

      {error && <InlineAlert message={error} />}

      <div style={{ display: 'flex', gap: 8 }}>
        <button
          className="btn"
          type="submit"
          disabled={busy || !valid}
          style={{ width: 'auto', padding: '8px 16px', fontSize: 13, opacity: busy || !valid ? 0.5 : 1 }}
        >
          {busy ? 'Procesando…' : 'Registrar pago'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy} style={smallBtn}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

const ORIGIN_OPTION_LABELS: Record<PaymentOrigin, string> = {
  NONE: 'Nadie (solo registrar)',
  CASH: 'Efectivo',
  SAVINGS: 'Cuenta'
};

/**
 * Formulario único para crear y editar. Sin `initial` es alta (POST); con
 * `initial` es edición (PATCH), donde institution, totalAmount y remainingBalance
 * se muestran fijos porque el backend no los deja cambiar. `embedded` lo dibuja
 * dentro de la tarjeta (edición) en vez de como card suelta (alta).
 *
 * Valida lo mismo que el backend: totalAmount y monthlyPayment >= 0.01,
 * paymentDueDay entre 1 y 31, remainingBalance >= 0. interestRate y description
 * son opcionales.
 */
function DebtForm({
  initial,
  embedded,
  onCancel,
  onSaved
}: {
  initial?: FixedDebt;
  embedded?: boolean;
  onCancel: () => void;
  onSaved: (debt: FixedDebt) => void;
}) {
  const editing = initial != null;

  const [institution, setInstitution] = useState(initial?.institution ?? '');
  const [description, setDescription] = useState(initial?.description ?? '');
  const [totalAmount, setTotalAmount] = useState(initial ? String(initial.totalAmount) : '');
  const [monthlyPayment, setMonthlyPayment] = useState(initial ? String(initial.monthlyPayment) : '');
  const [interestRate, setInterestRate] = useState(
    initial?.interestRate != null ? String(initial.interestRate) : ''
  );
  const [paymentDueDay, setPaymentDueDay] = useState(initial ? String(initial.paymentDueDay) : '');
  const [remainingBalance, setRemainingBalance] = useState(
    initial ? String(initial.remainingBalance) : ''
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const totalNum = Number(totalAmount);
  const monthlyNum = Number(monthlyPayment);
  const dueNum = Number(paymentDueDay);
  const remainingNum = remainingBalance.trim() === '' ? totalNum : Number(remainingBalance);

  const validDay = (n: number) => Number.isInteger(n) && n >= 1 && n <= 31;
  const valid =
    (editing || institution.trim() !== '') &&
    totalAmount.trim() !== '' && totalNum >= 0.01 &&
    monthlyPayment.trim() !== '' && monthlyNum >= 0.01 &&
    validDay(dueNum) &&
    remainingNum >= 0;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      let debt: FixedDebt;
      if (editing) {
        const body: DebtPatchRequest = {
          description: description.trim(),
          monthlyPayment: monthlyNum,
          interestRate: interestRate.trim() === '' ? 0 : Number(interestRate),
          paymentDueDay: dueNum
        };
        debt = await api.patch<FixedDebt>('/api/v1/debts/' + initial.id, body);
      } else {
        const body: DebtRequest = {
          institution: institution.trim(),
          description: description.trim(),
          totalAmount: totalNum,
          monthlyPayment: monthlyNum,
          interestRate: interestRate.trim() === '' ? 0 : Number(interestRate),
          paymentDueDay: dueNum,
          remainingBalance: remainingNum
        };
        debt = await api.post<FixedDebt>('/api/v1/debts', body);
      }
      onSaved(debt);
    } catch (err) {
      setError(errorMessage(err, 'No pudimos guardar la deuda.'));
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
        <Field label="Institución" type="text" value={institution} onChange={setInstitution}
               placeholder="Banco de Bogotá" />
      )}
      <Field label="Descripción (opcional)" type="text" value={description} onChange={setDescription}
             placeholder="Crédito moto" />

      {editing ? (
        <div style={{ display: 'grid', gap: 6 }}>
          <span className="eyebrow">Monto total</span>
          <p style={{ fontWeight: 600, fontSize: 15 }}>{money(totalNum)}</p>
        </div>
      ) : (
        <Field label="Monto total" type="number" value={totalAmount} onChange={setTotalAmount}
               placeholder="8000000" hint="Debe ser mayor a 0." />
      )}

      <Field label="Cuota mensual" type="number" value={monthlyPayment} onChange={setMonthlyPayment}
             placeholder="141889" hint="Debe ser mayor a 0." />
      <Field label="Tasa de interés (opcional)" type="number" value={interestRate} onChange={setInterestRate}
             placeholder="0" />
      <Field label="Día de pago" type="number" value={paymentDueDay} onChange={setPaymentDueDay}
             placeholder="16" hint="Entre 1 y 31." />

      {!editing && (
        <Field label="Saldo restante (opcional)" type="number" value={remainingBalance}
               onChange={setRemainingBalance} placeholder="8000000"
               hint="Déjalo vacío si aún no has pagado nada." />
      )}

      {error && <InlineAlert message={error} />}

      <button className="btn" type="submit" disabled={busy || !valid}
              style={{ marginTop: 4, opacity: busy || !valid ? 0.5 : 1 }}>
        {busy ? 'Guardando…' : editing ? 'Guardar cambios' : 'Guardar deuda'}
      </button>
      <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}
              style={{ borderColor: 'var(--line)' }}>
        Cancelar
      </button>
    </form>
  );
}

// Mismos estilos de botón que Cards/Savings, por coherencia entre pantallas.
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