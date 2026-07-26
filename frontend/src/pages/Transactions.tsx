import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../lib/api';
import { Field } from './Login';
import { Select } from '../components/Select';
import { money } from '../lib/money';
import { errorMessage } from '../lib/errorMessage';
import { InlineAlert } from '../components/InlineAlert';
import { ConfirmDialog } from '../components/ConfirmDialog';
import type {
  Transaction, CreditCard, SavingsAccount, Pocket, Cash,
  Page, OriginType
} from '../types';

/* ============================================================
   Movimientos — historial paginado de transacciones.

   El eje del diseño: una transacción es un GASTO. La cifra
   protagonista es el monto y de dónde salió. El color NO decora
   el monto — el sistema reserva safe/warn/danger para salud
   financiera (utilización, deudas); un movimiento normal no es
   una crisis, así que va en --ink como el resto de cifras.

   Regla de QA: el botón de registrar NUNCA debe habilitarse si el
   origen elegido no puede cubrir el gasto — sin tarjetas, sin
   cuentas, o sin efectivo suficiente. La vista debe explicarse
   sola: mensaje en rojo + una vibración sutil que llama la
   atención sin ser intrusiva, repetida mientras el bloqueo siga
   vigente.
   ============================================================ */

const PAGE_SIZE = 10;

const CATEGORIES = [
  'Alimentación', 'Transporte', 'Salud', 'Educación',
  'Entretenimiento', 'Ropa', 'Hogar', 'Tecnología',
  'Gasolina', 'Compras', 'Servicios', 'Otro'
];

type FilterType = 'ALL' | 'SHARED' | 'CARD';

const FILTER_LABELS: Record<FilterType, string> = {
  ALL: 'Todos',
  SHARED: 'Compartidos',
  CARD: 'Por tarjeta'
};

const ORIGIN_LABELS: Record<OriginType, string> = {
  CREDIT_CARD: 'Tarjeta',
  SAVINGS: 'Cuenta',
  CASH: 'Efectivo'
};

/** POST transacción: espeja TransactionRequest. */
interface TransactionRequest {
  description: string;
  amount: number;
  category?: string;
  originType: OriginType;
  creditCardId?: string;
  savingsAccountId?: string;
  pocketId?: string;
  shared?: boolean;
  sharedWithName?: string;
  sharedAmount?: number;
  installments?: number;
}

export default function Transactions() {
  const [page, setPage] = useState<Page<Transaction> | null>(null);
  const [cards, setCards] = useState<CreditCard[]>([]);
  const [accounts, setAccounts] = useState<SavingsAccount[]>([]);
  const [cash, setCash] = useState<Cash | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [filter, setFilter] = useState<FilterType>('ALL');
  const [selectedCardId, setSelectedCardId] = useState('');
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Transaction | null>(null);

  async function load(pageNumber = 0) {
    setError(null);
    try {
      let url = `/api/v1/transactions?page=${pageNumber}&size=${PAGE_SIZE}`;
      if (filter === 'SHARED') url = `/api/v1/transactions/shared?page=${pageNumber}&size=${PAGE_SIZE}`;
      if (filter === 'CARD' && selectedCardId) url = `/api/v1/transactions/card/${selectedCardId}?page=${pageNumber}&size=${PAGE_SIZE}`;
      const data = await api.get<Page<Transaction>>(url);
      setPage(data);
    } catch (err) {
      setError(errorMessage(err, 'No pudimos cargar tus movimientos.'));
    }
  }

  async function refreshOrigins() {
    const [c, a, k] = await Promise.all([
      api.get<CreditCard[]>('/api/v1/cards').catch(() => []),
      api.get<SavingsAccount[]>('/api/v1/savings').catch(() => []),
      api.get<Cash>('/api/v1/cash').catch(() => null)
    ]);
    setCards(c);
    setAccounts(a);
    setCash(k);
    if (c.length > 0 && !selectedCardId) setSelectedCardId(c[0].id);
  }

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter, selectedCardId]);

  useEffect(() => {
    refreshOrigins();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function onSaved() {
    setAdding(false);
    // Registrar un movimiento cambia el saldo del origen (tarjeta, cuenta o
    // efectivo): hay que refrescarlo para que la próxima validación del
    // formulario use el disponible real, no uno desactualizado.
    refreshOrigins();
    load(page?.number ?? 0);
  }

  async function confirmDelete(tx: Transaction) {
    setPendingDelete(null);
    setError(null);
    setDeletingId(tx.id);
    try {
      await api.del(`/api/v1/transactions/${tx.id}`);
      setPage((prev) => prev
        ? { ...prev, content: prev.content.filter((t) => t.id !== tx.id),
            totalElements: prev.totalElements - 1 }
        : prev
      );
      refreshOrigins();
    } catch (err) {
      setError(errorMessage(err, 'No pudimos eliminar el movimiento.'));
    } finally {
      setDeletingId(null);
    }
  }

  if (error && !page) {
    return (
      <main>
        <InlineAlert message={error} />
      </main>
    );
  }

  if (!page) {
    return (
      <main>
        <p style={{ color: 'var(--muted)' }}>Cargando…</p>
      </main>
    );
  }

  return (
    <main>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <p className="eyebrow">Tus movimientos</p>
        {!adding && (
          <button
            className="btn btn-ghost"
            onClick={() => setAdding(true)}
            style={{ width: 'auto', padding: '6px 10px', fontSize: 13, borderColor: 'var(--line)' }}
          >
            Registrar
          </button>
        )}
      </header>

      {error && page && (
        <div style={{ marginTop: 16 }}>
          <InlineAlert message={error} />
        </div>
      )}

      {adding && (
        <TransactionForm
          cards={cards}
          accounts={accounts}
          cash={cash}
          onCancel={() => setAdding(false)}
          onSaved={onSaved}
        />
      )}

      {/* Filtros — ocultos mientras se crea, para no competir por atención
          con el formulario. */}
      {!adding && (
        <div style={{ display: 'flex', gap: 8, marginTop: 20, flexWrap: 'wrap' }}>
          {(Object.keys(FILTER_LABELS) as FilterType[]).map((f) => (
            <button
              key={f}
              className="btn btn-ghost"
              onClick={() => setFilter(f)}
              style={filter === f ? activeBtn : smallBtn}
            >
              {FILTER_LABELS[f]}
            </button>
          ))}
        </div>
      )}

      {!adding && filter === 'CARD' && (
        cards.length > 0 ? (
          <div style={{ marginTop: 14 }}>
            <Select label="Tarjeta" value={selectedCardId} onChange={setSelectedCardId}>
              {cards.map((c) => (
                <option key={c.id} value={c.id}>{c.nickname || c.institution}</option>
              ))}
            </Select>
          </div>
        ) : (
          <p style={{ marginTop: 12, color: 'var(--muted)', fontSize: 13 }}>
            Todavía no tienes tarjetas registradas.
          </p>
        )
      )}

      {!adding && page.content.length === 0 && (
        <div className="card" style={{ marginTop: 22, color: 'var(--muted)', fontSize: 14, lineHeight: 1.5 }}>
          {filter === 'ALL'
            ? 'Todavía no tienes movimientos. Registra el primero para empezar a ver tu historial.'
            : 'Sin movimientos con este filtro.'}
        </div>
      )}

      {page.content.length > 0 && (
        <section style={{ marginTop: 22, display: 'grid', gap: 12 }}>
          {page.content.map((tx) => (
            <TransactionRow
              key={tx.id}
              tx={tx}
              deleting={deletingId === tx.id}
              onDelete={() => setPendingDelete(tx)}
            />
          ))}
        </section>
      )}

      {page.totalPages > 1 && (
        <Pager page={page} onChange={load} />
      )}

      {pendingDelete && (
        <ConfirmDialog
          title="Eliminar movimiento"
          message={`¿Eliminar "${pendingDelete.description}" por ${money(pendingDelete.amount)}? Esto devuelve el monto a su origen.`}
          confirmLabel="Eliminar movimiento"
          tone="danger"
          onConfirm={() => confirmDelete(pendingDelete)}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </main>
  );
}

/* ============================================================
   Paginación — pares de botones circulares con ícono. Un botón
   deshabilitado (opaco + cursor not-allowed) comunica por sí solo
   que no hay más páginas en esa dirección; no hace falta texto.
   ============================================================ */

function Pager({ page, onChange }: { page: Page<Transaction>; onChange: (n: number) => void }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 18, marginTop: 24 }}>
      <PagerButton direction="prev" disabled={page.first} onClick={() => onChange(page.number - 1)} />
      <span style={{ fontSize: 13, color: 'var(--muted)', minWidth: 92, textAlign: 'center' }}>
        Página {page.number + 1} de {page.totalPages}
      </span>
      <PagerButton direction="next" disabled={page.last} onClick={() => onChange(page.number + 1)} />
    </div>
  );
}

function PagerButton({
  direction,
  disabled,
  onClick
}: {
  direction: 'prev' | 'next';
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={direction === 'prev' ? 'Página anterior' : 'Página siguiente'}
      style={{
        width: 40, height: 40, borderRadius: '50%',
        border: '1px solid var(--line)',
        background: 'var(--card)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.35 : 1,
        color: 'var(--ink)'
      }}
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
        {direction === 'prev' ? <path d="M15 18l-6-6 6-6" /> : <path d="M9 18l6-6-6-6" />}
      </svg>
    </button>
  );
}

/* ============================================================
   Fila de transacción
   ============================================================ */

function TransactionRow({
  tx,
  deleting,
  onDelete
}: {
  tx: Transaction;
  deleting: boolean;
  onDelete: () => void;
}) {
  const date = new Date(tx.date).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' });

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div style={{ minWidth: 0 }}>
          <p style={{ fontWeight: 600, fontSize: 15, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {tx.description}
          </p>
          <p className="eyebrow" style={{ marginTop: 4 }}>
            {tx.originName || ORIGIN_LABELS[tx.originType]}
            {tx.pocketName ? ` · ${tx.pocketName}` : ''}
            {tx.category ? ` · ${tx.category}` : ''}
            {' · '}{date}
          </p>
        </div>
        <div style={{ textAlign: 'right', flexShrink: 0 }}>
          <p className="amount" style={{ fontSize: 17, fontWeight: 700 }}>
            {money(tx.amount)}
          </p>
          {tx.installments > 1 && (
            <p style={{ fontSize: 11, color: 'var(--muted)', marginTop: 2 }}>
              cuota {tx.currentInstallment}/{tx.installments}
            </p>
          )}
        </div>
      </div>

      {tx.shared && tx.sharedWithName && (
        <p style={{ marginTop: 10, fontSize: 13, color: 'var(--muted)' }}>
          Compartido con <strong style={{ color: 'var(--ink)' }}>{tx.sharedWithName}</strong>
          {tx.sharedAmount ? ` · cobrar ${money(tx.sharedAmount)}` : ''}
          {tx.paymentDeadline
            ? ` · límite ${new Date(tx.paymentDeadline).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' })}`
            : ''}
        </p>
      )}

      {tx.thirdParty && tx.thirdPartyOwnerName && (
        <p style={{ marginTop: 10, fontSize: 13, color: 'var(--muted)' }}>
          Gasto de <strong style={{ color: 'var(--ink)' }}>{tx.thirdPartyOwnerName}</strong>
        </p>
      )}

      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
        <button
          className="btn btn-ghost"
          onClick={onDelete}
          disabled={deleting}
          style={{ ...smallBtn, color: 'var(--danger)' }}
        >
          {deleting ? 'Eliminando…' : 'Eliminar'}
        </button>
      </div>
    </div>
  );
}

/* ============================================================
   Aviso que vibra — se dispara al aparecer y se repite cada
   4s mientras el bloqueo siga vigente, para que el usuario
   entienda por qué el botón sigue apagado sin tener que buscarlo.
   ============================================================ */

function useShakePulse(active: boolean, intervalMs = 4000) {
  const [nonce, setNonce] = useState(0);
  useEffect(() => {
    if (!active) return;
    setNonce((n) => n + 1);
    const id = setInterval(() => setNonce((n) => n + 1), intervalMs);
    return () => clearInterval(id);
  }, [active, intervalMs]);
  return nonce;
}

function BlockingWarning({ message }: { message: string }) {
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

/* ============================================================
   Formulario de alta

   Solo alta: una transacción no se edita — eliminas y vuelves a
   registrar, igual que el resto del dominio trata los movimientos
   como hechos consumados.
   ============================================================ */

function TransactionForm({
  cards,
  accounts,
  cash,
  onCancel,
  onSaved
}: {
  cards: CreditCard[];
  accounts: SavingsAccount[];
  cash: Cash | null;
  onCancel: () => void;
  onSaved: (tx: Transaction) => void;
}) {
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('');
  const [originType, setOriginType] = useState<OriginType>(
    cards.length > 0 ? 'CREDIT_CARD' : accounts.length > 0 ? 'SAVINGS' : 'CASH'
  );
  const [creditCardId, setCreditCardId] = useState(cards[0]?.id ?? '');
  const [savingsAccountId, setSavingsAccountId] = useState(accounts[0]?.id ?? '');
  const [pocketId, setPocketId] = useState('');
  const [installments, setInstallments] = useState('1');
  const [shared, setShared] = useState(false);
  const [sharedWithName, setSharedWithName] = useState('');
  const [sharedAmount, setSharedAmount] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedAccount = accounts.find((a) => a.id === savingsAccountId);
  const pockets: Pocket[] = selectedAccount?.pockets ?? [];

  const amountNum = Number(amount);
  const installmentsNum = Number(installments);
  const amountEntered = amount.trim() !== '' && amountNum >= 0.01;

  /* Motivo de bloqueo por origen — cubre exactamente los tres casos que un
     QA esperaría: sin tarjetas, sin cuentas, y sin efectivo suficiente. Es
     el único punto de verdad que decide si el submit puede habilitarse. */
  let originWarning: string | null = null;
  if (originType === 'CREDIT_CARD' && cards.length === 0) {
    originWarning = 'No tienes tarjetas registradas.';
  } else if (originType === 'SAVINGS' && accounts.length === 0) {
    originWarning = 'No tienes cuentas de ahorro registradas.';
  } else if (originType === 'CASH') {
    if (!cash || cash.balance <= 0) {
      originWarning = 'No tienes efectivo registrado.';
    } else if (amountEntered && amountNum > cash.balance) {
      originWarning = `Ese monto supera tu efectivo disponible (${money(cash.balance)}).`;
    }
  }

  const valid =
    description.trim() !== '' &&
    amountEntered &&
    originWarning === null &&
    (originType !== 'CREDIT_CARD' || creditCardId !== '') &&
    (originType !== 'SAVINGS' || savingsAccountId !== '') &&
    Number.isInteger(installmentsNum) && installmentsNum >= 1 &&
    (!shared || sharedWithName.trim() !== '');

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      const body: TransactionRequest = {
        description: description.trim(),
        amount: amountNum,
        category: category || undefined,
        originType,
        creditCardId: originType === 'CREDIT_CARD' ? creditCardId : undefined,
        savingsAccountId: originType === 'SAVINGS' ? savingsAccountId : undefined,
        pocketId: originType === 'SAVINGS' && pocketId ? pocketId : undefined,
        shared: shared || undefined,
        sharedWithName: shared ? sharedWithName.trim() : undefined,
        sharedAmount: shared && sharedAmount ? Number(sharedAmount) : undefined,
        installments: installmentsNum > 1 ? installmentsNum : undefined
      };
      const tx = await api.post<Transaction>('/api/v1/transactions', body);
      onSaved(tx);
    } catch (err) {
      setError(errorMessage(err, 'No pudimos registrar el movimiento.'));
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="card" style={{ marginTop: 22, display: 'grid', gap: 12 }}>
      <Field label="Descripción" type="text" value={description} onChange={setDescription}
             placeholder="¿En qué gastaste?" />
      <Field label="Monto" type="number" value={amount} onChange={setAmount}
             placeholder="50000" hint="Debe ser mayor a 0." />

      <div style={{ display: 'grid', gap: 6 }}>
        <span className="eyebrow">Origen</span>
        <div style={{ display: 'flex', gap: 8 }}>
          {(['CREDIT_CARD', 'SAVINGS', 'CASH'] as const).map((o) => (
            <button
              key={o}
              type="button"
              className="btn btn-ghost"
              onClick={() => setOriginType(o)}
              style={{ flex: 1, ...(originType === o ? activeBtn : smallBtn) }}
            >
              {ORIGIN_LABELS[o]}
            </button>
          ))}
        </div>
      </div>

      {originType === 'CREDIT_CARD' && cards.length > 0 && (
        <Select label="Tarjeta" value={creditCardId} onChange={setCreditCardId}>
          {cards.map((c) => (
            <option key={c.id} value={c.id}>
              {(c.nickname || c.institution)} — disponible {money(c.availableAmount)}
            </option>
          ))}
        </Select>
      )}

      {originType === 'SAVINGS' && accounts.length > 0 && (
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

      {originType === 'CASH' && cash && cash.balance > 0 && (
        <p style={{ fontSize: 13, color: 'var(--muted)' }}>
          Disponible en efectivo: <strong style={{ color: 'var(--ink)' }}>{money(cash.balance)}</strong>
        </p>
      )}

      {/* Único punto donde se explica un bloqueo por origen — vibra para
          que no pase inadvertido, y se repite mientras siga vigente. */}
      {originWarning && <BlockingWarning message={originWarning} />}

      <Select label="Categoría (opcional)" value={category} onChange={setCategory}>
        <option value="">Sin categoría</option>
        {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
      </Select>

      <Field label="Cuotas" type="number" value={installments} onChange={setInstallments}
             placeholder="1" hint="1 si es de contado." />

      <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, cursor: 'pointer' }}>
        <input type="checkbox" checked={shared} onChange={(e) => setShared(e.target.checked)} />
        Es un gasto compartido
      </label>

      {shared && (
        <>
          <Field label="¿Con quién?" type="text" value={sharedWithName} onChange={setSharedWithName}
                 placeholder="Nombre de la persona" />
          <Field label="Monto a cobrar (opcional)" type="number" value={sharedAmount} onChange={setSharedAmount}
                 placeholder="50000" />
        </>
      )}

      {error && <InlineAlert message={error} />}

      <button className="btn" type="submit" disabled={busy || !valid}
              style={{ marginTop: 4, opacity: busy || !valid ? 0.5 : 1 }}>
        {busy ? 'Guardando…' : 'Registrar movimiento'}
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