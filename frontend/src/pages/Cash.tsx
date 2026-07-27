import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../lib/api';
import { MoneyField } from '../components/MoneyField';
import { BlockingWarning } from '../components/BlockingWarning';
import { money } from '../lib/money';
import { errorMessage } from '../lib/errorMessage';
import { InlineAlert } from '../components/InlineAlert';
import type { Cash } from '../types';

/* ============================================================
   Efectivo — un solo registro por usuario, sin lista ni alta.

   El eje de diseño: en la vida real nadie "crea" su efectivo, solo
   lo tiene. El backend ya lo garantiza (se autocrea en $0 al primer
   GET), así que esta pantalla nunca pregunta "¿cuánto tienes?" al
   entrar — solo ofrece Agregar / Retirar, igual que un bolsillo.
   ============================================================ */

type Operation = 'ADD' | 'SUBTRACT';

/** PATCH efectivo: espeja CashUpdateRequest. */
interface CashUpdateRequest {
  amount: number;
  operation: Operation;
}

export default function CashPage() {
  const [cash, setCash] = useState<Cash | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [op, setOp] = useState<Operation | null>(null);

  async function load() {
    setError(null);
    try {
      setCash(await api.get<Cash>('/api/v1/cash'));
    } catch (err) {
      setError(errorMessage(err, 'No pudimos cargar tu efectivo.'));
    }
  }

  useEffect(() => {
    load();
  }, []);

  if (error && !cash) {
    return (
      <main>
        <InlineAlert message={error} />
      </main>
    );
  }

  if (!cash) {
    return (
      <main>
        <p style={{ color: 'var(--muted)' }}>Cargando…</p>
      </main>
    );
  }

  const updatedAt = new Date(cash.updatedAt).toLocaleDateString('es-CO', {
    day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit'
  });

  return (
    <main>
      <header>
        <p className="eyebrow">Tu efectivo</p>
      </header>

      {error && cash && (
        <div style={{ marginTop: 16 }}>
          <InlineAlert message={error} />
        </div>
      )}

      <div className="card" style={{ marginTop: 22 }}>
        <span className="eyebrow">Disponible</span>
        <p className="amount" style={{ fontSize: 32, fontWeight: 800, marginTop: 4 }}>
          {money(cash.balance)}
        </p>

        <div style={{ display: 'flex', gap: 8, marginTop: 16 }}>
          <button
            className="btn btn-ghost"
            onClick={() => setOp((cur) => (cur === 'ADD' ? null : 'ADD'))}
            style={op === 'ADD' ? activeBtn : op ? dimBtn : smallBtn}
          >
            Agregar
          </button>
          <button
            className="btn btn-ghost"
            onClick={() => setOp((cur) => (cur === 'SUBTRACT' ? null : 'SUBTRACT'))}
            style={op === 'SUBTRACT' ? activeBtn : op ? dimBtn : smallBtn}
          >
            Retirar
          </button>
        </div>

        {op && (
          <CashOpForm
            cash={cash}
            operation={op}
            onCancel={() => setOp(null)}
            onDone={(fresh) => {
              setCash(fresh);
              setOp(null);
            }}
          />
        )}
      </div>

      <p style={{ marginTop: 16, fontSize: 12, color: 'var(--muted)' }}>
        Actualizado el {updatedAt}
      </p>
    </main>
  );
}

/**
 * Formulario compartido para agregar o retirar. La única regla de negocio
 * real está en retirar: no puedes sacar más de lo que tienes — el mismo
 * lenguaje visual (BlockingWarning) que usamos en Transacciones y Deudas.
 */
function CashOpForm({
  cash,
  operation,
  onCancel,
  onDone
}: {
  cash: Cash;
  operation: Operation;
  onCancel: () => void;
  onDone: (cash: Cash) => void;
}) {
  const [amount, setAmount] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const amountNum = Number(amount);
  const amountEntered = amount.trim() !== '' && amountNum > 0;
  const exceedsBalance = operation === 'SUBTRACT' && amountEntered && amountNum > cash.balance;

  const warning = exceedsBalance
    ? `No puedes retirar más de lo que tienes (${money(cash.balance)}).`
    : null;

  const valid = amountEntered && warning === null;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      const body: CashUpdateRequest = { amount: amountNum, operation };
      const fresh = await api.patch<Cash>('/api/v1/cash', body);
      onDone(fresh);
    } catch (err) {
      setError(errorMessage(err, 'No pudimos actualizar tu efectivo.'));
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={onSubmit}
      style={{ marginTop: 14, paddingTop: 14, borderTop: '1px solid var(--line)',
               display: 'grid', gap: 10 }}
    >
      <MoneyField
        label={operation === 'ADD' ? 'Monto a agregar' : 'Monto a retirar'}
        value={amount}
        onChange={setAmount}
        placeholder="50000"
        hint={operation === 'SUBTRACT' ? `Tienes ${money(cash.balance)}.` : undefined}
      />

      {warning && <BlockingWarning message={warning} />}
      {error && <InlineAlert message={error} />}

      <div style={{ display: 'flex', gap: 8 }}>
        <button
          className="btn"
          type="submit"
          disabled={busy || !valid}
          style={{ width: 'auto', padding: '8px 16px', fontSize: 13, opacity: busy || !valid ? 0.5 : 1 }}
        >
          {busy ? 'Guardando…' : operation === 'ADD' ? 'Agregar' : 'Retirar'}
        </button>
        <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy} style={smallBtn}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

// Mismos estilos de botón que Cards/Savings/Debts, por coherencia entre pantallas.
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