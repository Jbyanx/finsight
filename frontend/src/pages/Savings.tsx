import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api, ApiError } from '../lib/api';
import { Field } from './Login';
import { money } from '../lib/money';
import type { SavingsAccount, Pocket } from '../types';

/* ============================================================
   Cuentas de ahorro y bolsillos.

   El eje del diseño es de dominio: una cuenta puede alojar dinero de terceros.
   Lo tuyo (ownBalance) es la cifra protagonista; lo ajeno (sharedTotal) se
   muestra aparte y atenuado, nunca sumado como propio. Un bolsillo compartido
   lleva el rótulo "de <nombre>" y se ve apagado. Una meta de ahorro usa una
   barra SOBRIA propia: aquí el 100% es bueno, lo contrario de la utilización.
   ============================================================ */

/** POST cuenta: espeja SavingsAccountRequest. */
interface SavingsAccountRequest {
  institution: string;
  nickname: string;
  balance: number;
  shared: boolean;
  sharedOwnerName: string;
}

/** PATCH cuenta: espeja SavingsAccountPatchRequest (institution y balance fijos). */
interface SavingsAccountPatchRequest {
  nickname: string;
  shared: boolean;
  sharedOwnerName: string;
}

/** POST bolsillo: espeja PocketRequest. */
interface PocketRequest {
  name: string;
  balance: number;
  goalAmount?: number;
  shared: boolean;
  sharedOwnerName: string;
}

/** PATCH bolsillo: espeja PocketPatchRequest (balance no se edita). */
interface PocketPatchRequest {
  name: string;
  goalAmount?: number;
  removeGoal: boolean;
  shared: boolean;
  sharedOwnerName: string;
}

export default function Savings() {
  const [accounts, setAccounts] = useState<SavingsAccount[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  async function load() {
    setError(null);
    try {
      setAccounts(await api.get<SavingsAccount[]>('/api/v1/savings'));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos cargar tus cuentas.');
    }
  }

  useEffect(() => {
    load();
  }, []);

  function upsert(account: SavingsAccount) {
    setAccounts((prev) => {
      if (!prev) return [account];
      const idx = prev.findIndex((a) => a.id === account.id);
      if (idx === -1) return [...prev, account];
      const next = [...prev];
      next[idx] = account;
      return next;
    });
  }

  /**
   * Los endpoints de bolsillo devuelven solo el Pocket, pero mutar un bolsillo
   * recalcula los agregados de la cuenta (ownBalance, sharedTotal…). Por eso, tras
   * tocar un bolsillo, recargamos esa cuenta y no solo su lista de bolsillos.
   */
  async function refreshAccount(accountId: string) {
    const fresh = await api.get<SavingsAccount>('/api/v1/savings/' + accountId);
    upsert(fresh);
  }

  async function removeAccount(account: SavingsAccount) {
    if (!confirm('¿Eliminar esta cuenta de ahorro?')) return;
    setError(null);
    setDeletingId(account.id);
    try {
      await api.del('/api/v1/savings/' + account.id);
      setAccounts((prev) => (prev ? prev.filter((a) => a.id !== account.id) : prev));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos eliminar la cuenta.');
    } finally {
      setDeletingId(null);
    }
  }

  if (error && !accounts) {
    return (
      <main>
        <div className="card" style={{ borderColor: 'var(--danger)', color: 'var(--danger)' }}>
          {error}
        </div>
      </main>
    );
  }

  if (!accounts) {
    return (
      <main>
        <p style={{ color: 'var(--muted)' }}>Cargando…</p>
      </main>
    );
  }

  return (
    <main>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <p className="eyebrow">Tus ahorros</p>
        {!adding && (
          <button
            className="btn btn-ghost"
            onClick={() => setAdding(true)}
            style={{ width: 'auto', padding: '6px 10px', fontSize: 13, borderColor: 'var(--line)' }}
          >
            Agregar cuenta
          </button>
        )}
      </header>

      {error && accounts && (
        <p role="alert" style={alertStyle}>
          {error}
        </p>
      )}

      {adding && (
        <AccountForm
          onCancel={() => setAdding(false)}
          onSaved={(a) => {
            upsert(a);
            setAdding(false);
          }}
        />
      )}

      {!adding && accounts.length === 0 && (
        <div className="card" style={{ marginTop: 22, color: 'var(--muted)', fontSize: 14, lineHeight: 1.5 }}>
          Todavía no tienes cuentas de ahorro. Agrega la primera para separar lo
          tuyo de lo que guardas para terceros.
        </div>
      )}

      <section style={{ marginTop: 22, display: 'grid', gap: 12 }}>
        {accounts.map((acc) =>
          editingId === acc.id ? (
            <AccountForm
              key={acc.id}
              initial={acc}
              onCancel={() => setEditingId(null)}
              onSaved={(a) => {
                upsert(a);
                setEditingId(null);
              }}
            />
          ) : (
            <AccountCard
              key={acc.id}
              account={acc}
              expanded={expandedId === acc.id}
              deleting={deletingId === acc.id}
              onToggle={() => setExpandedId((id) => (id === acc.id ? null : acc.id))}
              onEdit={() => setEditingId(acc.id)}
              onDelete={() => removeAccount(acc)}
              onPocketChange={() => refreshAccount(acc.id)}
            />
          )
        )}
      </section>
    </main>
  );
}

function AccountCard({
  account,
  expanded,
  deleting,
  onToggle,
  onEdit,
  onDelete,
  onPocketChange
}: {
  account: SavingsAccount;
  expanded: boolean;
  deleting: boolean;
  onToggle: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onPocketChange: () => void;
}) {
  const [addingPocket, setAddingPocket] = useState(false);
  const [editingPocketId, setEditingPocketId] = useState<string | null>(null);
  const [pocketError, setPocketError] = useState<string | null>(null);
  const [deletingPocketId, setDeletingPocketId] = useState<string | null>(null);

  async function removePocket(pocket: Pocket) {
    if (!confirm('¿Eliminar este bolsillo?')) return;
    setPocketError(null);
    setDeletingPocketId(pocket.id);
    try {
      await api.del('/api/v1/savings/' + account.id + '/pockets/' + pocket.id);
      onPocketChange();
    } catch (err) {
      setPocketError(err instanceof ApiError ? err.message : 'No pudimos eliminar el bolsillo.');
    } finally {
      setDeletingPocketId(null);
    }
  }

  // Lo apartado en bolsillos es lo que el balance total le lleva al disponible.
  // Por construccion: disponible + apartado = balance total (la invariante que
  // el usuario ve cuadrar en pantalla).
  const pocketed = account.balance - account.availableBalance;

  return (
    <div className="card">
      <div>
        <p style={{ fontWeight: 600, fontSize: 15 }}>{account.nickname || account.institution}</p>
        {account.nickname && (
          <p className="eyebrow" style={{ marginTop: 2 }}>{account.institution}</p>
        )}
      </div>

      {/* Disponible es la cifra protagonista: lo libre, ni apartado en bolsillos
          ni de terceros. El resto son lineas de contexto para que se entienda a
          donde fue la plata. Es tambien el numero que cambia tras deposit/
          withdraw (balance, ownBalance y sharedTotal no se mueven). */}
      <div style={{ marginTop: 14 }}>
        <span className="eyebrow">Disponible</span>
        <p className="amount" style={{ fontSize: 28, fontWeight: 800, marginTop: 2 }}>
          {money(account.availableBalance)}
        </p>
      </div>

      <div style={{ marginTop: 12, display: 'grid', gap: 6 }}>
        <InfoRow label="Balance total" value={money(account.balance)} />
        {pocketed > 0 && (
          <InfoRow label="Apartado en bolsillos" value={money(pocketed)} />
        )}
        {account.sharedTotal > 0 && (
          <InfoRow label="De terceros" value={money(account.sharedTotal)} muted />
        )}
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 14, flexWrap: 'wrap' }}>
        <button className="btn btn-ghost" onClick={onToggle} disabled={deleting}
                style={{ ...(expanded ? activeBtn : smallBtn),
                         display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          {expanded ? 'Ocultar bolsillos' : 'Ver bolsillos (' + account.pockets.length + ')'}
          <Chevron open={expanded} />
        </button>
        <button className="btn btn-ghost" onClick={onEdit} disabled={deleting} style={smallBtn}>
          Editar
        </button>
        <button className="btn btn-ghost" onClick={onDelete} disabled={deleting}
                style={{ ...smallBtn, color: 'var(--danger)' }}>
          {deleting ? 'Eliminando…' : 'Eliminar'}
        </button>
      </div>

      {expanded && (
        <div style={{ marginTop: 16, borderTop: '1px solid var(--line)', paddingTop: 16,
                      display: 'grid', gap: 12 }}>
          {pocketError && <p role="alert" style={alertStyle}>{pocketError}</p>}

          {account.pockets.length === 0 && !addingPocket && (
            <p style={{ color: 'var(--muted)', fontSize: 13 }}>
              Sin bolsillos. Crea uno para reservar dinero hacia una meta o para un tercero.
            </p>
          )}

          {account.pockets.map((p) =>
            editingPocketId === p.id ? (
              <PocketForm
                key={p.id}
                accountId={account.id}
                initial={p}
                onCancel={() => setEditingPocketId(null)}
                onSaved={() => {
                  setEditingPocketId(null);
                  onPocketChange();
                }}
              />
            ) : (
              <PocketRow
                key={p.id}
                pocket={p}
                accountId={account.id}
                deleting={deletingPocketId === p.id}
                onEdit={() => setEditingPocketId(p.id)}
                onDelete={() => removePocket(p)}
                onOperated={onPocketChange}
              />
            )
          )}

          {addingPocket ? (
            <PocketForm
              accountId={account.id}
              onCancel={() => setAddingPocket(false)}
              onSaved={() => {
                setAddingPocket(false);
                onPocketChange();
              }}
            />
          ) : (
            <button className="btn btn-ghost" onClick={() => setAddingPocket(true)}
                    style={{ borderColor: 'var(--line)' }}>
              Agregar bolsillo
            </button>
          )}
        </div>
      )}
    </div>
  );
}

function PocketRow({
  pocket,
  accountId,
  deleting,
  onEdit,
  onDelete,
  onOperated
}: {
  pocket: Pocket;
  accountId: string;
  deleting: boolean;
  onEdit: () => void;
  onDelete: () => void;
  onOperated: () => void;
}) {
  const isShared = pocket.shared;
  // El usuario piensa "agrego $50.000", no en un total nuevo: abrimos un input de
  // MONTO. Solo una operación abierta a la vez por bolsillo.
  const [op, setOp] = useState<'deposit' | 'withdraw' | null>(null);

  return (
    <div style={{ opacity: isShared ? 0.55 : 1 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
        <div>
          <span style={{ fontWeight: 600, fontSize: 14 }}>{pocket.name}</span>
          {isShared && pocket.sharedOwnerName && (
            <span style={{ fontSize: 12, color: 'var(--muted)', marginLeft: 8 }}>
              de {pocket.sharedOwnerName}
            </span>
          )}
        </div>
        <span className="amount" style={{ fontSize: 14 }}>{money(pocket.balance)}</span>
      </div>

      {/* Meta solo para bolsillos propios: barra sobria, 100% es lo bueno. */}
      {!isShared && pocket.goalAmount != null && pocket.goalAmount > 0 && (
        <GoalBar
          balance={pocket.balance}
          goal={pocket.goalAmount}
          progress={pocket.progressPercentage ?? 0}
        />
      )}

      {/* El botón de la operación abierta se ve ACTIVO (relleno --ink); el resto
          se atenúa. Así el usuario ve en qué acción está sin leer el formulario.
          El activo se puede volver a tocar para cerrar. */}
      <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
        <button className="btn btn-ghost"
                onClick={() => setOp((cur) => (cur === 'deposit' ? null : 'deposit'))}
                disabled={deleting || op === 'withdraw'}
                style={op === 'deposit' ? activeBtn : op ? dimBtn : smallBtn}>
          Agregar
        </button>
        <button className="btn btn-ghost"
                onClick={() => setOp((cur) => (cur === 'withdraw' ? null : 'withdraw'))}
                disabled={deleting || op === 'deposit'}
                style={op === 'withdraw' ? activeBtn : op ? dimBtn : smallBtn}>
          Retirar
        </button>
        <button className="btn btn-ghost" onClick={onEdit} disabled={deleting}
                style={op ? dimBtn : smallBtn}>
          Editar
        </button>
        <button className="btn btn-ghost" onClick={onDelete} disabled={deleting}
                style={op ? dimBtn : { ...smallBtn, color: 'var(--danger)' }}>
          {deleting ? 'Eliminando…' : 'Eliminar'}
        </button>
      </div>

      {op && (
        <PocketAmountForm
          accountId={accountId}
          pocket={pocket}
          op={op}
          onCancel={() => setOp(null)}
          onDone={() => {
            setOp(null);
            onOperated();
          }}
        />
      )}
    </div>
  );
}

/**
 * Depositar o retirar un MONTO de un bolsillo. El endpoint de bolsillo devuelve
 * solo el Pocket, así que al terminar avisamos (onDone → refreshAccount) para
 * recargar la cuenta y refrescar el disponible y los agregados (ownBalance,
 * availableBalance). El backend valida los topes; su mensaje se muestra tal cual.
 */
function PocketAmountForm({
  accountId,
  pocket,
  op,
  onCancel,
  onDone
}: {
  accountId: string;
  pocket: Pocket;
  op: 'deposit' | 'withdraw';
  onCancel: () => void;
  onDone: () => void;
}) {
  const [amount, setAmount] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const amountNum = Number(amount);
  const valid = amount.trim() !== '' && Number.isFinite(amountNum) && amountNum > 0;

  // El formulario declara su acción sin ambigüedad: título en pregunta, acento y
  // etiqueta del confirmar propios de agregar (--ink) o retirar (--danger).
  const isWithdraw = op === 'withdraw';
  const accent = isWithdraw ? 'var(--danger)' : 'var(--ink)';
  const title = isWithdraw ? '¿Cuánto quieres retirar?' : '¿Cuánto quieres agregar?';
  const confirmLabel = isWithdraw ? 'Retirar del bolsillo' : 'Agregar al bolsillo';

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      await api.post<Pocket>(
        '/api/v1/savings/' + accountId + '/pockets/' + pocket.id + '/' + op,
        { amount: amountNum }
      );
      onDone();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos completar la operación.');
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit}
          style={{ marginTop: 10, display: 'grid', gap: 10,
                   border: '1px solid ' + accent, borderRadius: 10, padding: 12 }}>
      <p style={{ fontWeight: 600, fontSize: 14 }}>{title}</p>
      <Field
        label="Monto"
        type="number"
        value={amount}
        onChange={setAmount}
        placeholder="50000"
        hint={isWithdraw
          ? 'Vuelve al disponible de la cuenta.'
          : 'Sale del disponible de la cuenta.'}
      />

      {error && <p role="alert" style={alertStyle}>{error}</p>}

      <div style={{ display: 'flex', gap: 8 }}>
        <button className="btn" type="submit" disabled={busy || !valid}
                style={{ width: 'auto', padding: '8px 16px', fontSize: 13,
                         background: accent, borderColor: accent, color: 'var(--paper)',
                         opacity: busy || !valid ? 0.5 : 1 }}>
          {busy ? 'Procesando…' : confirmLabel}
        </button>
        <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}
                style={smallBtn}>
          Cancelar
        </button>
      </div>
    </form>
  );
}

/**
 * Barra de progreso de meta de ahorro. Deliberadamente NO es UtilizationBar: no
 * hay umbral de peligro ni color de alerta. Llegar al 100% es el objetivo, así que
 * se llena en verde sobrio hacia una meta, sin marcas rojas.
 */
function GoalBar({ balance, goal, progress }: { balance: number; goal: number; progress: number }) {
  const fill = Math.min(Math.max(progress, 0), 100);
  return (
    <div style={{ marginTop: 8 }}>
      <div style={{ height: 6, borderRadius: 999, background: 'var(--line)' }}>
        <div style={{ width: fill + '%', height: '100%', borderRadius: 999,
                      background: 'var(--safe)', transition: 'width .4s ease' }} />
      </div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6 }}>
        <span className="amount" style={{ fontSize: 12, color: 'var(--muted)' }}>
          {money(balance)} <span style={{ opacity: 0.5 }}>/ {money(goal)}</span>
        </span>
        <span className="amount" style={{ fontSize: 12, color: 'var(--safe)', fontWeight: 700 }}>
          {fill.toFixed(0)}%
        </span>
      </div>
    </div>
  );
}

function AccountForm({
  initial,
  onCancel,
  onSaved
}: {
  initial?: SavingsAccount;
  onCancel: () => void;
  onSaved: (account: SavingsAccount) => void;
}) {
  const editing = initial != null;

  const [institution, setInstitution] = useState(initial?.institution ?? '');
  const [nickname, setNickname] = useState(initial?.nickname ?? '');
  const [balance, setBalance] = useState(initial ? String(initial.balance) : '');
  const [shared, setShared] = useState(initial?.shared ?? false);
  const [sharedOwnerName, setSharedOwnerName] = useState(initial?.sharedOwnerName ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const balanceNum = Number(balance);
  const valid =
    (editing || institution.trim() !== '') &&
    (editing || (balance.trim() !== '' && balanceNum >= 0)) &&
    (!shared || sharedOwnerName.trim() !== '');

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      let account: SavingsAccount;
      if (editing) {
        const body: SavingsAccountPatchRequest = {
          nickname: nickname.trim(),
          shared,
          sharedOwnerName: shared ? sharedOwnerName.trim() : ''
        };
        account = await api.patch<SavingsAccount>('/api/v1/savings/' + initial.id, body);
      } else {
        const body: SavingsAccountRequest = {
          institution: institution.trim(),
          nickname: nickname.trim(),
          balance: balanceNum,
          shared,
          sharedOwnerName: shared ? sharedOwnerName.trim() : ''
        };
        account = await api.post<SavingsAccount>('/api/v1/savings', body);
      }
      onSaved(account);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos guardar la cuenta.');
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="card" style={{ marginTop: 22, display: 'grid', gap: 12 }}>
      {editing ? (
        <>
          <ReadOnly label="Institución" value={institution} />
          <ReadOnly label="Saldo" value={money(initial.balance)} />
        </>
      ) : (
        <>
          <Field label="Institución" type="text" value={institution} onChange={setInstitution}
                 placeholder="Nu" />
          <Field label="Saldo" type="number" value={balance} onChange={setBalance}
                 placeholder="1500000" hint="No puede ser negativo." />
        </>
      )}
      <Field label="Apodo (opcional)" type="text" value={nickname} onChange={setNickname}
             placeholder="Mi cuenta de ahorros" />

      <Toggle
        label="Es una cuenta de terceros"
        checked={shared}
        onChange={(v) => {
          setShared(v);
          if (!v) setSharedOwnerName('');
        }}
      />
      {shared && (
        <Field label="Dueño real" type="text" value={sharedOwnerName} onChange={setSharedOwnerName}
               placeholder="Mamá" hint="Nombre de quien es realmente el dinero." />
      )}

      {error && <p role="alert" style={alertStyle}>{error}</p>}

      <button className="btn" type="submit" disabled={busy || !valid}
              style={{ marginTop: 4, opacity: busy || !valid ? 0.5 : 1 }}>
        {busy ? 'Guardando…' : editing ? 'Guardar cambios' : 'Guardar cuenta'}
      </button>
      <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}
              style={{ borderColor: 'var(--line)' }}>
        Cancelar
      </button>
    </form>
  );
}

function PocketForm({
  accountId,
  initial,
  onCancel,
  onSaved
}: {
  accountId: string;
  initial?: Pocket;
  onCancel: () => void;
  onSaved: (pocket: Pocket) => void;
}) {
  const editing = initial != null;
  const hadGoal = initial?.goalAmount != null && initial.goalAmount > 0;

  const [name, setName] = useState(initial?.name ?? '');
  const [balance, setBalance] = useState(initial ? String(initial.balance) : '');
  const [goalAmount, setGoalAmount] = useState(
    hadGoal ? String(initial?.goalAmount) : ''
  );
  const [removeGoal, setRemoveGoal] = useState(false);
  const [shared, setShared] = useState(initial?.shared ?? false);
  const [sharedOwnerName, setSharedOwnerName] = useState(initial?.sharedOwnerName ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const balanceNum = Number(balance);
  const goalNum = goalAmount.trim() === '' ? undefined : Number(goalAmount);
  const goalValid = goalNum === undefined || (Number.isFinite(goalNum) && goalNum >= 0);
  const valid =
    name.trim() !== '' &&
    (editing || (balance.trim() !== '' && balanceNum >= 0)) &&
    goalValid &&
    (!shared || sharedOwnerName.trim() !== '');

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      let pocket: Pocket;
      if (editing) {
        const body: PocketPatchRequest = {
          name: name.trim(),
          goalAmount: removeGoal ? undefined : goalNum,
          removeGoal,
          shared,
          sharedOwnerName: shared ? sharedOwnerName.trim() : ''
        };
        pocket = await api.patch<Pocket>(
          '/api/v1/savings/' + accountId + '/pockets/' + initial.id,
          body
        );
      } else {
        const body: PocketRequest = {
          name: name.trim(),
          balance: balanceNum,
          goalAmount: goalNum,
          shared,
          sharedOwnerName: shared ? sharedOwnerName.trim() : ''
        };
        pocket = await api.post<Pocket>('/api/v1/savings/' + accountId + '/pockets', body);
      }
      onSaved(pocket);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos guardar el bolsillo.');
      setBusy(false);
    }
  }

  return (
    <form onSubmit={onSubmit} className="card"
          style={{ display: 'grid', gap: 12, background: 'var(--paper)' }}>
      <Field label="Nombre" type="text" value={name} onChange={setName}
             placeholder="Vacaciones" />

      {editing ? (
        <ReadOnly label="Saldo" value={money(initial.balance)} />
      ) : (
        <Field label="Saldo" type="number" value={balance} onChange={setBalance}
               placeholder="200000" hint="No puede ser negativo." />
      )}

      {editing && hadGoal && (
        <Toggle label="Quitar la meta" checked={removeGoal} onChange={setRemoveGoal} />
      )}
      {!removeGoal && (
        <Field label="Meta (opcional)" type="number" value={goalAmount} onChange={setGoalAmount}
               placeholder="1000000" hint="Déjala vacía si el bolsillo no tiene meta." />
      )}

      <Toggle
        label="El dinero es de un tercero"
        checked={shared}
        onChange={(v) => {
          setShared(v);
          if (!v) setSharedOwnerName('');
        }}
      />
      {shared && (
        <Field label="Dueño real" type="text" value={sharedOwnerName} onChange={setSharedOwnerName}
               placeholder="Papá" hint="Nombre de quien es realmente el dinero." />
      )}

      {error && <p role="alert" style={alertStyle}>{error}</p>}

      <button className="btn" type="submit" disabled={busy || !valid}
              style={{ marginTop: 4, opacity: busy || !valid ? 0.5 : 1 }}>
        {busy ? 'Guardando…' : editing ? 'Guardar cambios' : 'Guardar bolsillo'}
      </button>
      <button type="button" className="btn btn-ghost" onClick={onCancel} disabled={busy}
              style={{ borderColor: 'var(--line)' }}>
        Cancelar
      </button>
    </form>
  );
}

/** Linea de contexto del desglose de una cuenta: rotulo a la izquierda, monto a
 *  la derecha. `muted` atenua las cifras que no son propias (de terceros). */
function InfoRow({ label, value, muted }: { label: string; value: string; muted?: boolean }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline',
                  opacity: muted ? 0.55 : 1 }}>
      <span className="eyebrow">{label}</span>
      <span className="amount" style={{ fontSize: 14 }}>{value}</span>
    </div>
  );
}

function ReadOnly({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'grid', gap: 6 }}>
      <span className="eyebrow">{label}</span>
      <p className="amount" style={{ fontSize: 15, fontWeight: 600 }}>{value}</p>
    </div>
  );
}

function Toggle({
  label,
  checked,
  onChange
}: {
  label: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer' }}>
      <input
        type="checkbox"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
        style={{ width: 18, height: 18, accentColor: 'var(--ink)' }}
      />
      <span style={{ fontSize: 14 }}>{label}</span>
    </label>
  );
}

/** Chevron que rota 180° al abrir: comunica de un vistazo abierto/cerrado. */
function Chevron({ open }: { open: boolean }) {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden
         style={{ transition: 'transform .2s ease', transform: open ? 'rotate(180deg)' : 'none' }}>
      <path d="M6 9l6 6 6-6" />
    </svg>
  );
}

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

const alertStyle = {
  color: 'var(--danger)',
  background: 'var(--danger-soft)',
  border: '1px solid var(--danger)',
  borderRadius: 10,
  padding: '10px 12px',
  fontSize: 13,
  lineHeight: 1.4
} as const;
