import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../lib/api';
import { Field } from './Login';
import { Select } from '../components/Select';
import { InlineAlert } from '../components/InlineAlert';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { money } from '../lib/money';
import { errorMessage } from '../lib/errorMessage';
import type {
  Invoice, InvoicePayment, CreditCard,
  InvoiceFrequency, PaymentMethod, PaymentStatus, VoucherType, Page
} from '../types';

/* ============================================================
   Facturas recurrentes — servicios públicos, planes, suscripciones.

   El eje de diseño: una factura es una PROMESA mensual (o bimestral,
   o anual), no un gasto puntual — por eso vive separada de
   Transacciones. "own" define de quién es la responsabilidad y es
   fijo desde que se crea, igual que la institución de una tarjeta:
   no tiene sentido que una factura "cambie de dueño" a mitad de
   camino. El monto SÍ se edita — a diferencia de una deuda, el
   precio de una suscripción puede subir sin que eso invalide los
   pagos ya hechos.
   ============================================================ */

const PAGE_SIZE = 10;

const FREQUENCY_LABELS: Record<InvoiceFrequency, string> = {
  MONTHLY: 'Mensual',
  BIMONTHLY: 'Bimestral',
  ANNUAL: 'Anual'
};

const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CREDIT_CARD: 'Tarjeta',
  CASH: 'Efectivo',
  TRANSFER: 'Transferencia'
};

const STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: 'Pendiente',
  PAID: 'Pagado',
  OVERDUE: 'Atrasado'
};

const MONTH_NAMES = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
];

const smallBtn = {
  width: 'auto',
  padding: '6px 12px',
  fontSize: 13,
  borderColor: 'var(--line)'
} as const;

const activeBtn = {
  ...smallBtn,
  background: 'var(--ink)',
  color: 'var(--paper)',
  borderColor: 'var(--ink)'
} as const;

const dimBtn = {
  ...smallBtn,
  color: 'var(--muted)',
  opacity: 0.55
} as const;

/* ============================================================
   Página principal
   ============================================================ */

export default function Invoices() {
  const [invoices, setInvoices] = useState<Invoice[] | null>(null);
  const [cards, setCards] = useState<CreditCard[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<Invoice | null>(null);

  async function load() {
    setError(null);
    try {
      setInvoices(await api.get<Invoice[]>('/api/v1/invoices'));
    } catch (err) {
      setError(errorMessage(err, 'No pudimos cargar tus facturas.'));
    }
  }

  useEffect(() => {
    load();
    api.get<CreditCard[]>('/api/v1/cards').then(setCards).catch(() => {});
  }, []);

  function upsert(invoice: Invoice) {
    setInvoices((prev) => {
      if (!prev) return [invoice];
      const idx = prev.findIndex((i) => i.id === invoice.id);
      if (idx === -1) return [...prev, invoice];
      const next = [...prev];
      next[idx] = invoice;
      return next;
    });
  }

  async function confirmDelete(invoice: Invoice) {
    setPendingDelete(null);
    setError(null);
    setDeletingId(invoice.id);
    try {
      await api.del('/api/v1/invoices/' + invoice.id);
      setInvoices((prev) => (prev ? prev.filter((i) => i.id !== invoice.id) : prev));
    } catch (err) {
      setError(errorMessage(err, 'No pudimos eliminar la factura.'));
    } finally {
      setDeletingId(null);
    }
  }

  if (error && !invoices) {
    return (
      <main>
        <InlineAlert message={error} />
      </main>
    );
  }

  if (!invoices) {
    return (
      <main>
        <p style={{ color: 'var(--muted)' }}>Cargando…</p>
      </main>
    );
  }

  return (
    <main>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <p className="eyebrow">Tus facturas</p>
        {!adding && (
          <button
            className="btn btn-ghost"
            onClick={() => setAdding(true)}
            style={{ width: 'auto', padding: '6px 10px', fontSize: 13, borderColor: 'var(--line)' }}
          >
            Agregar factura
          </button>
        )}
      </header>

      {error && invoices && (
        <div style={{ marginTop: 16 }}>
          <InlineAlert message={error} />
        </div>
      )}

      {adding && (
        <InvoiceForm
          cards={cards}
          onCancel={() => setAdding(false)}
          onSaved={(i) => { upsert(i); setAdding(false); }}
        />
      )}

      {!adding && invoices.length === 0 && (
        <div className="card" style={{ marginTop: 22, color: 'var(--muted)', fontSize: 14, lineHeight: 1.5 }}>
          Sin facturas registradas. Sirve tanto para servicios públicos como
          para suscripciones — Netflix es una factura recurrente igual que el
          recibo de la luz.
        </div>
      )}

      <section style={{ marginTop: 22, display: 'grid', gap: 12 }}>
        {invoices.map((inv) => (
          <InvoiceCard
            key={inv.id}
            invoice={inv}
            cards={cards}
            editing={editingId === inv.id}
            deleting={deletingId === inv.id}
            onEdit={() => setEditingId(inv.id)}
            onStopEdit={() => setEditingId(null)}
            onSaved={(fresh) => { upsert(fresh); setEditingId(null); }}
            onDelete={() => setPendingDelete(inv)}
          />
        ))}
      </section>

      {pendingDelete && (
        <ConfirmDialog
          title="Eliminar factura"
          message={'¿Eliminar "' + pendingDelete.description + '"? También se pierde su historial de pagos.'}
          confirmLabel="Eliminar factura"
          tone="danger"
          onConfirm={() => confirmDelete(pendingDelete)}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </main>
  );
}

/* ============================================================
   Tarjeta de factura — con acciones para editar, pagar o
   ver el historial de pagos, mutuamente excluyentes.
   ============================================================ */

function InvoiceCard({
  invoice,
  cards,
  editing,
  deleting,
  onEdit,
  onStopEdit,
  onSaved,
  onDelete
}: {
  invoice: Invoice;
  cards: CreditCard[];
  editing: boolean;
  deleting: boolean;
  onEdit: () => void;
  onStopEdit: () => void;
  onSaved: (invoice: Invoice) => void;
  onDelete: () => void;
}) {
  const [paying, setPaying] = useState(false);
  const [viewingHistory, setViewingHistory] = useState(false);

  const anyOpen = paying || editing || viewingHistory;

  return (
    <div className="card">
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div style={{ minWidth: 0 }}>
          <p style={{ fontWeight: 600, fontSize: 15 }}>{invoice.description}</p>
          <p className="eyebrow" style={{ marginTop: 4 }}>
            {FREQUENCY_LABELS[invoice.frequency]} · vence el {invoice.dueDay}
            {invoice.paymentMethod ? ` · ${PAYMENT_METHOD_LABELS[invoice.paymentMethod]}` : ''}
            {invoice.creditCardName ? ` (${invoice.creditCardName})` : ''}
          </p>
        </div>
        <p className="amount" style={{ fontSize: 17, fontWeight: 700, flexShrink: 0 }}>
          {money(invoice.amount)}
        </p>
      </div>

      {!invoice.own && invoice.responsibleName && (
        <p style={{ marginTop: 10, fontSize: 13, color: 'var(--muted)' }}>
          Responsable: <strong style={{ color: 'var(--ink)' }}>{invoice.responsibleName}</strong>
        </p>
      )}

      {invoice.payerNote && (
        <p style={{ marginTop: 6, fontSize: 13, color: 'var(--muted)' }}>
          {invoice.payerNote}
        </p>
      )}

      <div style={{ display: 'flex', gap: 8, marginTop: 14, flexWrap: 'wrap' }}>
        <button
          className="btn btn-ghost"
          onClick={() => setPaying((p) => !p)}
          disabled={deleting || (anyOpen && !paying)}
          style={paying ? activeBtn : anyOpen ? dimBtn : smallBtn}
        >
          Registrar pago
        </button>
        <button
          className="btn btn-ghost"
          onClick={() => setViewingHistory((v) => !v)}
          disabled={deleting || (anyOpen && !viewingHistory)}
          style={viewingHistory ? activeBtn : anyOpen ? dimBtn : smallBtn}
        >
          Ver pagos
        </button>
        <button
          className="btn btn-ghost"
          onClick={() => (editing ? onStopEdit() : onEdit())}
          disabled={deleting || (anyOpen && !editing)}
          style={editing ? activeBtn : anyOpen ? dimBtn : smallBtn}
        >
          Editar
        </button>
        <button
          className="btn btn-ghost"
          onClick={onDelete}
          disabled={deleting || anyOpen}
          style={anyOpen ? dimBtn : { ...smallBtn, color: 'var(--danger)' }}
        >
          {deleting ? 'Eliminando…' : 'Eliminar'}
        </button>
      </div>

      {paying && (
        <PaymentForm
          invoice={invoice}
          onCancel={() => setPaying(false)}
          onDone={() => setPaying(false)}
        />
      )}

      {viewingHistory && <PaymentHistory invoice={invoice} />}

      {editing && (
        <InvoiceForm embedded initial={invoice} cards={cards} onCancel={onStopEdit} onSaved={onSaved} />
      )}
    </div>
  );
}

/* ============================================================
   Registrar pago de un período — no valida saldo porque no
   descuenta de ningún origen todavía: es un registro de que
   "esta factura quedó cubierta este mes", con voucher opcional.
   ============================================================ */

interface InvoicePaymentBody {
  amountPaid?: number;
  periodMonth: number;
  periodYear: number;
  notes?: string;
  voucherUrl?: string;
  voucherType?: VoucherType;
}

function PaymentForm({
  invoice,
  onCancel,
  onDone
}: {
  invoice: Invoice;
  onCancel: () => void;
  onDone: () => void;
}) {
  const now = new Date();
  const [periodMonth, setPeriodMonth] = useState(String(now.getMonth() + 1));
  const [periodYear, setPeriodYear] = useState(String(now.getFullYear()));
  const [amountPaid, setAmountPaid] = useState('');
  const [notes, setNotes] = useState('');
  const [voucherUrl, setVoucherUrl] = useState('');
  const [voucherType, setVoucherType] = useState<VoucherType | ''>('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const valid =
    Number.isInteger(Number(periodMonth)) && Number(periodMonth) >= 1 && Number(periodMonth) <= 12 &&
    periodYear.trim() !== '' && Number.isInteger(Number(periodYear));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      const body: InvoicePaymentBody = {
        amountPaid: amountPaid.trim() === '' ? undefined : Number(amountPaid),
        periodMonth: Number(periodMonth),
        periodYear: Number(periodYear),
        notes: notes.trim() || undefined,
        voucherUrl: voucherUrl.trim() || undefined,
        voucherType: voucherUrl.trim() && voucherType ? voucherType : undefined
      };
      await api.post<InvoicePayment>('/api/v1/invoices/' + invoice.id + '/payments', body);
      onDone();
    } catch (err) {
      setError(errorMessage(err, 'No pudimos registrar el pago.'));
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={onSubmit}
      style={{ marginTop: 14, paddingTop: 14, borderTop: '1px solid var(--line)',
               display: 'grid', gap: 10 }}
    >
      <div style={{ display: 'flex', gap: 8 }}>
        <div style={{ flex: 2 }}>
          <Select label="Mes" value={periodMonth} onChange={setPeriodMonth}>
            {MONTH_NAMES.map((m, i) => (
              <option key={m} value={i + 1}>{m}</option>
            ))}
          </Select>
        </div>
        <div style={{ flex: 1 }}>
          <Field label="Año" type="number" value={periodYear} onChange={setPeriodYear}
                 placeholder={String(now.getFullYear())} />
        </div>
      </div>

      <Field
        label="Monto pagado (opcional)"
        type="number"
        value={amountPaid}
        onChange={setAmountPaid}
        placeholder={String(invoice.amount)}
        hint={`Si lo dejas vacío, se registra ${money(invoice.amount)}.`}
      />

      <Field label="Notas (opcional)" type="text" value={notes} onChange={setNotes}
             placeholder="Pagado en el banco" />

      <Field label="Comprobante (opcional)" type="text" value={voucherUrl} onChange={setVoucherUrl}
             placeholder="URL de la captura o foto" />

      {voucherUrl.trim() !== '' && (
        <Select label="Tipo de comprobante" value={voucherType} onChange={(v) => setVoucherType(v as VoucherType | '')}>
          <option value="">Sin especificar</option>
          <option value="IMAGE">Imagen</option>
          <option value="PDF">PDF</option>
        </Select>
      )}

      {error && <InlineAlert message={error} />}

      <div style={{ display: 'flex', gap: 8 }}>
        <button
          className="btn"
          type="submit"
          disabled={busy || !valid}
          style={{ width: 'auto', padding: '8px 16px', fontSize: 13, opacity: busy || !valid ? 0.5 : 1 }}
        >
          {busy ? 'Guardando…' : 'Registrar pago'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy} style={smallBtn}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

/* ============================================================
   Historial de pagos de una factura — paginado.
   ============================================================ */

function PaymentHistory({ invoice }: { invoice: Invoice }) {
  const [page, setPage] = useState<Page<InvoicePayment> | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function load(pageNumber = 0) {
    setError(null);
    try {
      const data = await api.get<Page<InvoicePayment>>(
        `/api/v1/invoices/${invoice.id}/payments?page=${pageNumber}&size=${PAGE_SIZE}`
      );
      setPage(data);
    } catch (err) {
      setError(errorMessage(err, 'No pudimos cargar el historial.'));
    }
  }

  useEffect(() => {
    load(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div style={{ marginTop: 14, paddingTop: 14, borderTop: '1px solid var(--line)' }}>
      {error && <InlineAlert message={error} />}

      {!page && !error && (
        <p style={{ fontSize: 13, color: 'var(--muted)' }}>Cargando…</p>
      )}

      {page && page.content.length === 0 && (
        <p style={{ fontSize: 13, color: 'var(--muted)' }}>Sin pagos registrados todavía.</p>
      )}

      {page && page.content.length > 0 && (
        <div style={{ display: 'grid', gap: 8 }}>
          {page.content.map((p) => (
            <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
              <span style={{ fontSize: 13 }}>
                {MONTH_NAMES[p.periodMonth - 1]} {p.periodYear}
                <span style={{ color: 'var(--muted)' }}> · {STATUS_LABELS[p.status]}</span>
              </span>
              <span className="amount" style={{ fontSize: 14, fontWeight: 600 }}>{money(p.amountPaid)}</span>
            </div>
          ))}
        </div>
      )}

      {page && page.totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 14, marginTop: 12 }}>
          <button className="btn btn-ghost" disabled={page.first} onClick={() => load(page.number - 1)}
                  style={{ ...smallBtn, opacity: page.first ? 0.4 : 1 }}>
            Anterior
          </button>
          <span style={{ fontSize: 12, color: 'var(--muted)' }}>
            {page.number + 1} de {page.totalPages}
          </span>
          <button className="btn btn-ghost" disabled={page.last} onClick={() => load(page.number + 1)}
                  style={{ ...smallBtn, opacity: page.last ? 0.4 : 1 }}>
            Siguiente
          </button>
        </div>
      )}
    </div>
  );
}

/* ============================================================
   Formulario de alta / edición.

   own, responsibleName y creditCardId solo se definen al crear —
   el backend no los deja cambiar después (InvoicePatchRequest no
   los incluye), igual que institution en Tarjetas y Deudas.
   ============================================================ */

interface InvoiceRequestBody {
  description: string;
  amount: number;
  frequency: InvoiceFrequency;
  dueDay: number;
  paymentMethod?: PaymentMethod;
  creditCardId?: string;
  own?: boolean;
  responsibleName?: string;
  payerNote?: string;
}

interface InvoicePatchBody {
  description?: string;
  amount?: number;
  frequency?: InvoiceFrequency;
  dueDay?: number;
  paymentMethod?: PaymentMethod;
  payerNote?: string;
}

function InvoiceForm({
  initial,
  embedded,
  cards,
  onCancel,
  onSaved
}: {
  initial?: Invoice;
  embedded?: boolean;
  cards: CreditCard[];
  onCancel: () => void;
  onSaved: (invoice: Invoice) => void;
}) {
  const editing = initial != null;

  const [description, setDescription] = useState(initial?.description ?? '');
  const [amount, setAmount] = useState(initial ? String(initial.amount) : '');
  const [frequency, setFrequency] = useState<InvoiceFrequency>(initial?.frequency ?? 'MONTHLY');
  const [dueDay, setDueDay] = useState(initial ? String(initial.dueDay) : '');
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod | ''>(initial?.paymentMethod ?? '');
  const [creditCardId, setCreditCardId] = useState(cards[0]?.id ?? '');
  const [own, setOwn] = useState(initial?.own ?? true);
  const [responsibleName, setResponsibleName] = useState(initial?.responsibleName ?? '');
  const [payerNote, setPayerNote] = useState(initial?.payerNote ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const amountNum = Number(amount);
  const dueNum = Number(dueDay);
  const validDay = Number.isInteger(dueNum) && dueNum >= 1 && dueNum <= 31;

  // Al editar una factura que ya usaba tarjeta, no ofrecemos cambiar A tarjeta
  // porque el patch no acepta creditCardId — solo se puede editar lejos de ahí.
  const wasCreditCard = editing && initial?.paymentMethod === 'CREDIT_CARD';

  const valid =
    description.trim() !== '' &&
    amount.trim() !== '' && amountNum >= 0.01 &&
    validDay &&
    (own || responsibleName.trim() !== '') &&
    (paymentMethod !== 'CREDIT_CARD' || wasCreditCard || creditCardId !== '');

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      let invoice: Invoice;
      if (editing) {
        const body: InvoicePatchBody = {
          description: description.trim(),
          amount: amountNum,
          frequency,
          dueDay: dueNum,
          paymentMethod: paymentMethod || undefined,
          payerNote: payerNote.trim() || undefined
        };
        invoice = await api.patch<Invoice>('/api/v1/invoices/' + initial.id, body);
      } else {
        const body: InvoiceRequestBody = {
          description: description.trim(),
          amount: amountNum,
          frequency,
          dueDay: dueNum,
          paymentMethod: paymentMethod || undefined,
          creditCardId: paymentMethod === 'CREDIT_CARD' ? creditCardId : undefined,
          own,
          responsibleName: own ? undefined : responsibleName.trim(),
          payerNote: payerNote.trim() || undefined
        };
        invoice = await api.post<Invoice>('/api/v1/invoices', body);
      }
      onSaved(invoice);
    } catch (err) {
      setError(errorMessage(err, 'No pudimos guardar la factura.'));
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={onSubmit}
      className={embedded ? undefined : 'card'}
      style={
        embedded
          ? { marginTop: 14, paddingTop: 14, borderTop: '1px solid var(--line)', display: 'grid', gap: 12 }
          : { marginTop: 22, display: 'grid', gap: 12 }
      }
    >
      <Field label="Descripción" type="text" value={description} onChange={setDescription}
             placeholder="Netflix, recibo de luz, plan celular…" />
      <Field label="Monto" type="number" value={amount} onChange={setAmount}
             placeholder="60000" hint="Debe ser mayor a 0." />

      <Select label="Frecuencia" value={frequency} onChange={(v) => setFrequency(v as InvoiceFrequency)}>
        <option value="MONTHLY">Mensual</option>
        <option value="BIMONTHLY">Bimestral</option>
        <option value="ANNUAL">Anual</option>
      </Select>

      <Field label="Día de vencimiento" type="number" value={dueDay} onChange={setDueDay}
             placeholder="15" hint="Entre 1 y 31." />

      <Select label="Método de pago (opcional)" value={paymentMethod}
              onChange={(v) => setPaymentMethod(v as PaymentMethod | '')}>
        <option value="">Sin especificar</option>
        <option value="CASH">Efectivo</option>
        <option value="TRANSFER">Transferencia</option>
        {!editing && <option value="CREDIT_CARD">Tarjeta</option>}
      </Select>

      {!editing && paymentMethod === 'CREDIT_CARD' && (
        cards.length > 0 ? (
          <Select label="Tarjeta" value={creditCardId} onChange={setCreditCardId}>
            {cards.map((c) => (
              <option key={c.id} value={c.id}>{c.nickname || c.institution}</option>
            ))}
          </Select>
        ) : (
          <p style={{ fontSize: 13, color: 'var(--muted)' }}>No tienes tarjetas registradas.</p>
        )
      )}

      {wasCreditCard && (
        <p style={{ fontSize: 13, color: 'var(--muted)' }}>
          Tarjeta asociada: <strong style={{ color: 'var(--ink)' }}>{initial?.creditCardName}</strong>
          {' '}(no se puede cambiar aquí).
        </p>
      )}

      {!editing && (
        <div style={{ display: 'grid', gap: 6 }}>
          <span className="eyebrow">¿Es tuya?</span>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="button" className="btn btn-ghost" onClick={() => setOwn(true)}
                    style={{ flex: 1, ...(own ? activeBtn : smallBtn) }}>
              Sí
            </button>
            <button type="button" className="btn btn-ghost" onClick={() => setOwn(false)}
                    style={{ flex: 1, ...(!own ? activeBtn : smallBtn) }}>
              No, le pertenece a alguien más
            </button>
          </div>
        </div>
      )}

      {!editing && !own && (
        <Field label="¿De quién es?" type="text" value={responsibleName} onChange={setResponsibleName}
               placeholder="Papá" />
      )}

      <Field label="Nota (opcional)" type="text" value={payerNote} onChange={setPayerNote}
             placeholder="Pago yo, cobro después" />

      {error && <InlineAlert message={error} />}

      <button className="btn" type="submit" disabled={busy || !valid}
              style={{ marginTop: 4, opacity: busy || !valid ? 0.5 : 1 }}>
        {busy ? 'Guardando…' : editing ? 'Guardar cambios' : 'Guardar factura'}
      </button>
      <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}
              style={{ borderColor: 'var(--line)' }}>
        Cancelar
      </button>
    </form>
  );
}