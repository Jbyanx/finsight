import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import { api } from "../lib/api";
import { Field } from "./Login";
import { Select } from "../components/Select";
import { MoneyField } from "../components/MoneyField";
import { BlockingWarning } from "../components/BlockingWarning";
import { money } from "../lib/money";
import { errorMessage } from "../lib/errorMessage";
import { InlineAlert } from "../components/InlineAlert";
import { ConfirmDialog } from "../components/ConfirmDialog";
import type {
  IncomeSource,
  OperationalExpense,
  IncomeEntry,
  SavingsAccount,
  Pocket,
  IncomeType,
  IncomeFrequency,
  DestinationType,
  Page,
} from "../types";

/* ============================================================
   Ingresos — fuentes, gastos operativos, y el registro de cada
   entrada real de dinero.

   El eje de diseño: una FUENTE es de dónde viene la plata (turno,
   mesada, freelance); una ENTRADA es cada vez que realmente entró.
   Registrar una entrada es también la única forma correcta de meter
   dinero a una cuenta o bolsillo — no existe un botón genérico de
   "agregar saldo" porque en la vida real la plata siempre viene de
   algún lado, nunca aparece sola.
   ============================================================ */

const PAGE_SIZE = 10;

type Tab = "ENTRIES" | "SOURCES" | "EXPENSES";

const TAB_LABELS: Record<Tab, string> = {
  ENTRIES: "Registrar",
  SOURCES: "Fuentes",
  EXPENSES: "Gastos",
};

const TYPE_LABELS: Record<IncomeType, string> = {
  FIXED: "Fijo",
  VARIABLE: "Variable",
};

const FREQUENCY_LABELS: Record<IncomeFrequency, string> = {
  PER_SHIFT: "Por trabajo",
  WEEKLY: "Semanal",
  BIWEEKLY: "Quincenal",
  MONTHLY: "Mensual",
  ONE_TIME: "Una vez",
};

const DESTINATION_LABELS: Record<DestinationType, string> = {
  CASH: "Efectivo",
  SAVINGS: "Cuenta",
  POCKET: "Bolsillo",
};

// Mismos estilos de botón que el resto de la app, por coherencia.
const smallBtn = {
  width: "auto",
  padding: "6px 12px",
  fontSize: 13,
  borderColor: "var(--line)",
} as const;

const activeBtn = {
  ...smallBtn,
  background: "var(--ink)",
  color: "var(--paper)",
  borderColor: "var(--ink)",
} as const;

export default function Income() {
  const [tab, setTab] = useState<Tab>("ENTRIES");

  return (
    <main>
      <header>
        <p className="eyebrow">Tus ingresos</p>
      </header>

      <div style={{ display: "flex", gap: 8, marginTop: 16 }}>
        {(Object.keys(TAB_LABELS) as Tab[]).map((t) => (
          <button
            key={t}
            className="btn btn-ghost"
            onClick={() => setTab(t)}
            style={{ flex: 1, ...(tab === t ? activeBtn : smallBtn) }}
          >
            {TAB_LABELS[t]}
          </button>
        ))}
      </div>

      {tab === "ENTRIES" && <EntriesTab />}
      {tab === "SOURCES" && <SourcesTab />}
      {tab === "EXPENSES" && <ExpensesTab />}
    </main>
  );
}

/* ============================================================
   Registrar — formulario de entrada + historial paginado.
   ============================================================ */

/** POST entrada: espeja IncomeEntryRequest. */
interface IncomeEntryRequestBody {
  amount: number;
  description?: string;
  incomeSourceId?: string;
  destinationType: DestinationType;
  savingsAccountId?: string;
  pocketId?: string;
}

function EntriesTab() {
  const [page, setPage] = useState<Page<IncomeEntry> | null>(null);
  const [sources, setSources] = useState<IncomeSource[]>([]);
  const [accounts, setAccounts] = useState<SavingsAccount[]>([]);
  const [adding, setAdding] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function load(pageNumber = 0) {
    setError(null);
    try {
      const data = await api.get<Page<IncomeEntry>>(
        `/api/v1/income/entries?page=${pageNumber}&size=${PAGE_SIZE}`,
      );
      setPage(data);
    } catch (err) {
      setError(errorMessage(err, "No pudimos cargar tu historial."));
    }
  }

  useEffect(() => {
    load(0);
    api
      .get<IncomeSource[]>("/api/v1/income/sources")
      .then(setSources)
      .catch(() => {});
    api
      .get<SavingsAccount[]>("/api/v1/savings")
      .then(setAccounts)
      .catch(() => {});
  }, []);

  function onSaved() {
    setAdding(false);
    load(page?.number ?? 0);
    // El destino (cuenta/bolsillo) cambió de saldo: refresca para que la
    // próxima entrada tenga datos actualizados si vuelve a elegir bolsillo.
    api
      .get<SavingsAccount[]>("/api/v1/savings")
      .then(setAccounts)
      .catch(() => {});
  }

  if (error && !page) {
    return (
      <div style={{ marginTop: 22 }}>
        <InlineAlert message={error} />
      </div>
    );
  }

  if (!page) {
    return <p style={{ marginTop: 22, color: "var(--muted)" }}>Cargando…</p>;
  }

  return (
    <>
      <div
        style={{ display: "flex", justifyContent: "flex-end", marginTop: 16 }}
      >
        {!adding && (
          <button
            className="btn btn-ghost"
            onClick={() => setAdding(true)}
            style={{
              width: "auto",
              padding: "6px 10px",
              fontSize: 13,
              borderColor: "var(--line)",
            }}
          >
            Registrar ingreso
          </button>
        )}
      </div>

      {error && page && (
        <div style={{ marginTop: 12 }}>
          <InlineAlert message={error} />
        </div>
      )}

      {adding && (
        <EntryForm
          sources={sources}
          accounts={accounts}
          onCancel={() => setAdding(false)}
          onSaved={onSaved}
        />
      )}

      {!adding && page.content.length === 0 && (
        <div
          className="card"
          style={{
            marginTop: 16,
            color: "var(--muted)",
            fontSize: 14,
            lineHeight: 1.5,
          }}
        >
          Todavía no has registrado ingresos. El primero puede ser tan simple
          como "me dieron $30.000" — no necesitas una fuente definida.
        </div>
      )}

      {page.content.length > 0 && (
        <section style={{ marginTop: 16, display: "grid", gap: 12 }}>
          {page.content.map((e) => (
            <div key={e.id} className="card">
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "flex-start",
                  gap: 12,
                }}
              >
                <div style={{ minWidth: 0 }}>
                  <p style={{ fontWeight: 600, fontSize: 15 }}>
                    {e.description || e.incomeSourceDescription || "Ingreso"}
                  </p>
                  <p className="eyebrow" style={{ marginTop: 4 }}>
                    {e.destinationName || DESTINATION_LABELS[e.destinationType]}
                    {" · "}
                    {new Date(e.receivedAt).toLocaleDateString("es-CO", {
                      day: "numeric",
                      month: "short",
                    })}
                  </p>
                </div>
                <p
                  className="amount"
                  style={{ fontSize: 17, fontWeight: 700, flexShrink: 0 }}
                >
                  {money(e.amount)}
                </p>
              </div>
            </div>
          ))}
        </section>
      )}

      {page.totalPages > 1 && (
        <div
          style={{
            display: "flex",
            justifyContent: "center",
            alignItems: "center",
            gap: 18,
            marginTop: 24,
          }}
        >
          <PagerButton
            direction="prev"
            disabled={page.first}
            onClick={() => load(page.number - 1)}
          />
          <span
            style={{
              fontSize: 13,
              color: "var(--muted)",
              minWidth: 92,
              textAlign: "center",
            }}
          >
            Página {page.number + 1} de {page.totalPages}
          </span>
          <PagerButton
            direction="next"
            disabled={page.last}
            onClick={() => load(page.number + 1)}
          />
        </div>
      )}
    </>
  );
}

function PagerButton({
  direction,
  disabled,
  onClick,
}: {
  direction: "prev" | "next";
  disabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={direction === "prev" ? "Página anterior" : "Página siguiente"}
      style={{
        width: 40,
        height: 40,
        borderRadius: "50%",
        border: "1px solid var(--line)",
        background: "var(--card)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        cursor: disabled ? "not-allowed" : "pointer",
        opacity: disabled ? 0.35 : 1,
        color: "var(--ink)",
      }}
    >
      <svg
        width="18"
        height="18"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {direction === "prev" ? (
          <path d="M15 18l-6-6 6-6" />
        ) : (
          <path d="M9 18l6-6-6-6" />
        )}
      </svg>
    </button>
  );
}

/**
 * Registrar una entrada. Aquí es donde realmente entra dinero a la app:
 * puede quedarse en efectivo, ir directo al balance de una cuenta, o a un
 * bolsillo específico. Nunca hay validación de "saldo insuficiente" — al
 * revés que en Transacciones o Deudas, aquí el dinero entra, no sale.
 */
function EntryForm({
  sources,
  accounts,
  onCancel,
  onSaved,
}: {
  sources: IncomeSource[];
  accounts: SavingsAccount[];
  onCancel: () => void;
  onSaved: () => void;
}) {
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [sourceId, setSourceId] = useState("");
  const [destinationType, setDestinationType] =
    useState<DestinationType>("CASH");
  const [savingsAccountId, setSavingsAccountId] = useState(
    accounts[0]?.id ?? "",
  );
  const [pocketId, setPocketId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedAccount = accounts.find((a) => a.id === savingsAccountId);
  const pockets: Pocket[] = selectedAccount?.pockets ?? [];

  const amountNum = Number(amount);
  const amountEntered = amount.trim() !== "" && amountNum >= 0.01;

  let destinationWarning: string | null = null;
  if (
    (destinationType === "SAVINGS" || destinationType === "POCKET") &&
    accounts.length === 0
  ) {
    destinationWarning = "No tienes cuentas de ahorro registradas.";
  } else if (
    destinationType === "POCKET" &&
    selectedAccount &&
    pockets.length === 0
  ) {
    destinationWarning = "Esa cuenta no tiene bolsillos todavía.";
  }

  const valid =
    amountEntered &&
    destinationWarning === null &&
    (destinationType !== "SAVINGS" || savingsAccountId !== "") &&
    (destinationType !== "POCKET" ||
      (savingsAccountId !== "" && pocketId !== ""));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      const body: IncomeEntryRequestBody = {
        amount: amountNum,
        description: description.trim() || undefined,
        incomeSourceId: sourceId || undefined,
        destinationType,
        savingsAccountId:
          destinationType !== "CASH" ? savingsAccountId : undefined,
        pocketId: destinationType === "POCKET" ? pocketId : undefined,
      };
      await api.post<IncomeEntry>("/api/v1/income/entries", body);
      onSaved();
    } catch (err) {
      setError(errorMessage(err, "No pudimos registrar el ingreso."));
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={onSubmit}
      className="card"
      style={{ marginTop: 16, display: "grid", gap: 12 }}
    >
      <MoneyField
        label="Monto"
        value={amount}
        onChange={setAmount}
        placeholder="60000"
        hint="Debe ser mayor a 0."
      />
      <Field
        label="Descripción (opcional)"
        type="text"
        value={description}
        onChange={setDescription}
        placeholder="Turno del martes"
      />

      {sources.length > 0 && (
        <Select
          label="Fuente (opcional)"
          value={sourceId}
          onChange={setSourceId}
        >
          <option value="">Sin fuente — ingreso espontáneo</option>
          {sources.map((s) => (
            <option key={s.id} value={s.id}>
              {s.description}
            </option>
          ))}
        </Select>
      )}

      <div style={{ display: "grid", gap: 6 }}>
        <span className="eyebrow">¿Dónde queda la plata?</span>
        <div style={{ display: "flex", gap: 6 }}>
          {(["CASH", "SAVINGS", "POCKET"] as const).map((d) => (
            <button
              key={d}
              type="button"
              className="btn btn-ghost"
              onClick={() => setDestinationType(d)}
              style={{
                flex: 1,
                ...(destinationType === d ? activeBtn : smallBtn),
              }}
            >
              {DESTINATION_LABELS[d]}
            </button>
          ))}
        </div>
      </div>

      {(destinationType === "SAVINGS" || destinationType === "POCKET") &&
        accounts.length > 0 && (
          <Select
            label="Cuenta"
            value={savingsAccountId}
            onChange={(v) => {
              setSavingsAccountId(v);
              setPocketId("");
            }}
          >
            {accounts.map((a) => (
              <option key={a.id} value={a.id}>
                {a.nickname || a.institution}
              </option>
            ))}
          </Select>
        )}

      {destinationType === "POCKET" && pockets.length > 0 && (
        <Select label="Bolsillo" value={pocketId} onChange={setPocketId}>
          <option value="">Selecciona uno</option>
          {pockets.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name} — {money(p.balance)}
            </option>
          ))}
        </Select>
      )}

      {destinationWarning && <BlockingWarning message={destinationWarning} />}
      {error && <InlineAlert message={error} />}

      <button
        className="btn"
        type="submit"
        disabled={busy || !valid}
        style={{ marginTop: 4, opacity: busy || !valid ? 0.5 : 1 }}
      >
        {busy ? "Guardando…" : "Registrar ingreso"}
      </button>
      <button
        type="button"
        className="btn btn-ghost"
        onClick={onCancel}
        disabled={busy}
        style={{ borderColor: "var(--line)" }}
      >
        Cancelar
      </button>
    </form>
  );
}

/* ============================================================
   Fuentes — de dónde viene la plata. Pocas, se editan poco:
   List<>, no Page<>, mismo patrón que Cards/Debts.
   ============================================================ */

/** POST/PUT fuente: espeja IncomeSourceRequest. */
interface IncomeSourceRequest {
  description: string;
  type: IncomeType;
  estimatedAmount?: number;
  occurrencesPerMonth?: number;
  frequency?: IncomeFrequency;
}

/** PATCH fuente: todos los campos opcionales. */
interface IncomeSourcePatchRequest {
  description?: string;
  type?: IncomeType;
  estimatedAmount?: number;
  frequency?: IncomeFrequency;
  occurrencesPerMonth?: number;
}

function SourcesTab() {
  const [sources, setSources] = useState<IncomeSource[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<IncomeSource | null>(null);

  async function load() {
    setError(null);
    try {
      setSources(await api.get<IncomeSource[]>("/api/v1/income/sources"));
    } catch (err) {
      setError(errorMessage(err, "No pudimos cargar tus fuentes."));
    }
  }

  useEffect(() => {
    load();
  }, []);

  function upsert(source: IncomeSource) {
    setSources((prev) => {
      if (!prev) return [source];
      const idx = prev.findIndex((s) => s.id === source.id);
      if (idx === -1) return [...prev, source];
      const next = [...prev];
      next[idx] = source;
      return next;
    });
  }

  async function confirmDelete(source: IncomeSource) {
    setPendingDelete(null);
    setError(null);
    setDeletingId(source.id);
    try {
      await api.del("/api/v1/income/sources/" + source.id);
      setSources((prev) =>
        prev ? prev.filter((s) => s.id !== source.id) : prev,
      );
    } catch (err) {
      setError(errorMessage(err, "No pudimos eliminar la fuente."));
    } finally {
      setDeletingId(null);
    }
  }

  if (error && !sources) {
    return (
      <div style={{ marginTop: 22 }}>
        <InlineAlert message={error} />
      </div>
    );
  }

  if (!sources) {
    return <p style={{ marginTop: 22, color: "var(--muted)" }}>Cargando…</p>;
  }

  return (
    <>
      <div
        style={{ display: "flex", justifyContent: "flex-end", marginTop: 16 }}
      >
        {!adding && (
          <button
            className="btn btn-ghost"
            onClick={() => setAdding(true)}
            style={{
              width: "auto",
              padding: "6px 10px",
              fontSize: 13,
              borderColor: "var(--line)",
            }}
          >
            Agregar fuente
          </button>
        )}
      </div>

      {error && sources && (
        <div style={{ marginTop: 12 }}>
          <InlineAlert message={error} />
        </div>
      )}

      {adding && (
        <SourceForm
          onCancel={() => setAdding(false)}
          onSaved={(s) => {
            upsert(s);
            setAdding(false);
          }}
        />
      )}

      {!adding && sources.length === 0 && (
        <div
          className="card"
          style={{
            marginTop: 16,
            color: "var(--muted)",
            fontSize: 14,
            lineHeight: 1.5,
          }}
        >
          Sin fuentes registradas. Puedes seguir registrando ingresos sin
          fuente, pero agruparlos aquí te deja ver cuánto te da cada trabajo.
        </div>
      )}

      <section style={{ marginTop: 16, display: "grid", gap: 12 }}>
        {sources.map((s) => (
          <div key={s.id} className="card">
            {editingId === s.id ? (
              <SourceForm
                embedded
                initial={s}
                onCancel={() => setEditingId(null)}
                onSaved={(fresh) => {
                  upsert(fresh);
                  setEditingId(null);
                }}
              />
            ) : (
              <>
                <p style={{ fontWeight: 600, fontSize: 15 }}>{s.description}</p>
                <p className="eyebrow" style={{ marginTop: 4 }}>
                  {TYPE_LABELS[s.type]}
                  {s.frequency ? ` · ${FREQUENCY_LABELS[s.frequency]}` : ""}
                  {s.estimatedAmount ? ` · ${money(s.estimatedAmount)}` : ""}
                  {s.occurrencesPerMonth
                    ? ` · ${s.occurrencesPerMonth}/mes`
                    : ""}
                </p>
                <div style={{ display: "flex", gap: 8, marginTop: 14 }}>
                  <button
                    className="btn btn-ghost"
                    onClick={() => setEditingId(s.id)}
                    disabled={deletingId === s.id}
                    style={smallBtn}
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-ghost"
                    onClick={() => setPendingDelete(s)}
                    disabled={deletingId === s.id}
                    style={{ ...smallBtn, color: "var(--danger)" }}
                  >
                    {deletingId === s.id ? "Eliminando…" : "Eliminar"}
                  </button>
                </div>
              </>
            )}
          </div>
        ))}
      </section>

      {pendingDelete && (
        <ConfirmDialog
          title="Eliminar fuente"
          message={`¿Eliminar "${pendingDelete.description}"?`}
          confirmLabel="Eliminar fuente"
          tone="danger"
          onConfirm={() => confirmDelete(pendingDelete)}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </>
  );
}

function SourceForm({
  initial,
  embedded,
  onCancel,
  onSaved,
}: {
  initial?: IncomeSource;
  embedded?: boolean;
  onCancel: () => void;
  onSaved: (source: IncomeSource) => void;
}) {
  const editing = initial != null;

  const [description, setDescription] = useState(initial?.description ?? "");
  const [type, setType] = useState<IncomeType>(initial?.type ?? "FIXED");
  const [estimatedAmount, setEstimatedAmount] = useState(
    initial?.estimatedAmount != null ? String(initial.estimatedAmount) : "",
  );
  const [frequency, setFrequency] = useState<IncomeFrequency | "">(
    initial?.frequency ?? "",
  );
  const [occurrencesPerMonth, setOccurrencesPerMonth] = useState(
    initial?.occurrencesPerMonth != null
      ? String(initial.occurrencesPerMonth)
      : "",
  );
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const valid = description.trim() !== "";

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      const commonFields = {
        description: description.trim(),
        type,
        estimatedAmount:
          estimatedAmount.trim() === "" ? undefined : Number(estimatedAmount),
        frequency: frequency || undefined,
        occurrencesPerMonth:
          occurrencesPerMonth.trim() === ""
            ? undefined
            : Number(occurrencesPerMonth),
      };
      let source: IncomeSource;
      if (editing) {
        source = await api.patch<IncomeSource>(
          "/api/v1/income/sources/" + initial.id,
          commonFields as IncomeSourcePatchRequest,
        );
      } else {
        source = await api.post<IncomeSource>(
          "/api/v1/income/sources",
          commonFields as IncomeSourceRequest,
        );
      }
      onSaved(source);
    } catch (err) {
      setError(errorMessage(err, "No pudimos guardar la fuente."));
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={onSubmit}
      className={embedded ? undefined : "card"}
      style={
        embedded
          ? { display: "grid", gap: 12 }
          : { marginTop: 16, display: "grid", gap: 12 }
      }
    >
      <Field
        label="Descripción"
        type="text"
        value={description}
        onChange={setDescription}
        placeholder="Recepcionista tiempo completo"
      />

      <div style={{ display: "grid", gap: 6 }}>
        <span className="eyebrow">Tipo</span>
        <div style={{ display: "flex", gap: 8 }}>
          {(["FIXED", "VARIABLE"] as const).map((t) => (
            <button
              key={t}
              type="button"
              className="btn btn-ghost"
              onClick={() => setType(t)}
              style={{ flex: 1, ...(type === t ? activeBtn : smallBtn) }}
            >
              {TYPE_LABELS[t]}
            </button>
          ))}
        </div>
        <span style={{ fontSize: 12, color: "var(--muted)" }}>
          {type === "FIXED"
            ? "Un trabajo con monto y frecuencia predecibles."
            : "Ingreso ocasional, sin monto ni frecuencia fijos."}
        </span>
      </div>

      <MoneyField
        label="Monto estimado (opcional)"
        value={estimatedAmount}
        onChange={setEstimatedAmount}
        placeholder="1200000"
      />

      <Select
        label="Frecuencia (opcional)"
        value={frequency}
        onChange={(v) => setFrequency(v as IncomeFrequency | "")}
      >
        <option value="">Sin frecuencia fija</option>
        <option value="PER_SHIFT">Por trabajo</option>
        <option value="WEEKLY">Semanal</option>
        <option value="MONTHLY">Mensual</option>
        <option value="BIWEEKLY">Quincenal</option>
        <option value="ONE_TIME">Una vez</option>
      </Select>

      <Field
        label="Veces al mes (opcional)"
        type="number"
        value={occurrencesPerMonth}
        onChange={setOccurrencesPerMonth}
        placeholder="12"
        hint="Ej: 3 turnos por semana ≈ 12 al mes."
      />

      {error && <InlineAlert message={error} />}

      <button
        className="btn"
        type="submit"
        disabled={busy || !valid}
        style={{ marginTop: 4, opacity: busy || !valid ? 0.5 : 1 }}
      >
        {busy ? "Guardando…" : editing ? "Guardar cambios" : "Guardar fuente"}
      </button>
      <button
        type="button"
        className="btn btn-ghost"
        onClick={onCancel}
        disabled={busy}
        style={{ borderColor: "var(--line)" }}
      >
        Cancelar
      </button>
    </form>
  );
}

/* ============================================================
   Gastos operativos — lo que cuesta generar el ingreso.
   ============================================================ */

interface OperationalExpenseRequest {
  description: string;
  frequency: IncomeFrequency;
  occurrencesPerMonth?: number;
  estimatedAmount: number;
  incomeSourceId?: string;
}

interface OperationalExpensePatchRequest {
  description?: string;
  frequency?: IncomeFrequency;
  occurrencesPerMonth?: number;
  estimatedAmount?: number;
}

function ExpensesTab() {
  const [expenses, setExpenses] = useState<OperationalExpense[] | null>(null);
  const [sources, setSources] = useState<IncomeSource[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<OperationalExpense | null>(
    null,
  );

  async function load() {
    setError(null);
    try {
      setExpenses(
        await api.get<OperationalExpense[]>("/api/v1/income/expenses"),
      );
    } catch (err) {
      setError(errorMessage(err, "No pudimos cargar tus gastos operativos."));
    }
  }

  useEffect(() => {
    load();
    api
      .get<IncomeSource[]>("/api/v1/income/sources")
      .then(setSources)
      .catch(() => {});
  }, []);

  function upsert(expense: OperationalExpense) {
    setExpenses((prev) => {
      if (!prev) return [expense];
      const idx = prev.findIndex((e) => e.id === expense.id);
      if (idx === -1) return [...prev, expense];
      const next = [...prev];
      next[idx] = expense;
      return next;
    });
  }

  async function confirmDelete(expense: OperationalExpense) {
    setPendingDelete(null);
    setError(null);
    setDeletingId(expense.id);
    try {
      await api.del("/api/v1/income/expenses/" + expense.id);
      setExpenses((prev) =>
        prev ? prev.filter((e) => e.id !== expense.id) : prev,
      );
    } catch (err) {
      setError(errorMessage(err, "No pudimos eliminar el gasto."));
    } finally {
      setDeletingId(null);
    }
  }

  if (error && !expenses) {
    return (
      <div style={{ marginTop: 22 }}>
        <InlineAlert message={error} />
      </div>
    );
  }

  if (!expenses) {
    return <p style={{ marginTop: 22, color: "var(--muted)" }}>Cargando…</p>;
  }

  return (
    <>
      <div
        style={{ display: "flex", justifyContent: "flex-end", marginTop: 16 }}
      >
        {!adding && (
          <button
            className="btn btn-ghost"
            onClick={() => setAdding(true)}
            style={{
              width: "auto",
              padding: "6px 10px",
              fontSize: 13,
              borderColor: "var(--line)",
            }}
          >
            Agregar gasto
          </button>
        )}
      </div>

      {error && expenses && (
        <div style={{ marginTop: 12 }}>
          <InlineAlert message={error} />
        </div>
      )}

      {adding && (
        <ExpenseForm
          sources={sources}
          onCancel={() => setAdding(false)}
          onSaved={(e) => {
            upsert(e);
            setAdding(false);
          }}
        />
      )}

      {!adding && expenses.length === 0 && (
        <div
          className="card"
          style={{
            marginTop: 16,
            color: "var(--muted)",
            fontSize: 14,
            lineHeight: 1.5,
          }}
        >
          Sin gastos operativos. Registra lo que te cuesta generar un ingreso —
          gasolina, merienda — para conocer tu ganancia neta real.
        </div>
      )}

      <section style={{ marginTop: 16, display: "grid", gap: 12 }}>
        {expenses.map((e) => (
          <div key={e.id} className="card">
            {editingId === e.id ? (
              <ExpenseForm
                embedded
                sources={sources}
                initial={e}
                onCancel={() => setEditingId(null)}
                onSaved={(fresh) => {
                  upsert(fresh);
                  setEditingId(null);
                }}
              />
            ) : (
              <>
                <p style={{ fontWeight: 600, fontSize: 15 }}>{e.description}</p>
                <p className="eyebrow" style={{ marginTop: 4 }}>
                  {FREQUENCY_LABELS[e.frequency]} · {money(e.estimatedAmount)}
                  {e.occurrencesPerMonth
                    ? ` · ${e.occurrencesPerMonth}/mes`
                    : ""}
                  {e.incomeSourceDescription
                    ? ` · ${e.incomeSourceDescription}`
                    : ""}
                </p>
                <div style={{ display: "flex", gap: 8, marginTop: 14 }}>
                  <button
                    className="btn btn-ghost"
                    onClick={() => setEditingId(e.id)}
                    disabled={deletingId === e.id}
                    style={smallBtn}
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-ghost"
                    onClick={() => setPendingDelete(e)}
                    disabled={deletingId === e.id}
                    style={{ ...smallBtn, color: "var(--danger)" }}
                  >
                    {deletingId === e.id ? "Eliminando…" : "Eliminar"}
                  </button>
                </div>
              </>
            )}
          </div>
        ))}
      </section>

      {pendingDelete && (
        <ConfirmDialog
          title="Eliminar gasto"
          message={`¿Eliminar "${pendingDelete.description}"?`}
          confirmLabel="Eliminar gasto"
          tone="danger"
          onConfirm={() => confirmDelete(pendingDelete)}
          onCancel={() => setPendingDelete(null)}
        />
      )}
    </>
  );
}

function ExpenseForm({
  sources,
  initial,
  embedded,
  onCancel,
  onSaved,
}: {
  sources: IncomeSource[];
  initial?: OperationalExpense;
  embedded?: boolean;
  onCancel: () => void;
  onSaved: (expense: OperationalExpense) => void;
}) {
  const editing = initial != null;

  const [description, setDescription] = useState(initial?.description ?? "");
  const [frequency, setFrequency] = useState<IncomeFrequency>(
    initial?.frequency ?? "PER_SHIFT",
  );
  const [estimatedAmount, setEstimatedAmount] = useState(
    initial ? String(initial.estimatedAmount) : "",
  );
  const [occurrencesPerMonth, setOccurrencesPerMonth] = useState(
    initial?.occurrencesPerMonth != null
      ? String(initial.occurrencesPerMonth)
      : "",
  );
  const [sourceId, setSourceId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const amountNum = Number(estimatedAmount);
  const valid =
    description.trim() !== "" &&
    estimatedAmount.trim() !== "" &&
    amountNum >= 0.01;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    if (!valid) return;
    setError(null);
    setBusy(true);
    try {
      let expense: OperationalExpense;
      if (editing) {
        const body: OperationalExpensePatchRequest = {
          description: description.trim(),
          frequency,
          occurrencesPerMonth:
            occurrencesPerMonth.trim() === ""
              ? undefined
              : Number(occurrencesPerMonth),
          estimatedAmount: amountNum,
        };
        expense = await api.patch<OperationalExpense>(
          "/api/v1/income/expenses/" + initial.id,
          body,
        );
      } else {
        const body: OperationalExpenseRequest = {
          description: description.trim(),
          frequency,
          occurrencesPerMonth:
            occurrencesPerMonth.trim() === ""
              ? undefined
              : Number(occurrencesPerMonth),
          estimatedAmount: amountNum,
          incomeSourceId: sourceId || undefined,
        };
        expense = await api.post<OperationalExpense>(
          "/api/v1/income/expenses",
          body,
        );
      }
      onSaved(expense);
    } catch (err) {
      setError(errorMessage(err, "No pudimos guardar el gasto."));
      setBusy(false);
    }
  }

  return (
    <form
      onSubmit={onSubmit}
      className={embedded ? undefined : "card"}
      style={
        embedded
          ? { display: "grid", gap: 12 }
          : { marginTop: 16, display: "grid", gap: 12 }
      }
    >
      <Field
        label="Descripción"
        type="text"
        value={description}
        onChange={setDescription}
        placeholder="Gasolina"
      />

      <Select
        label="Frecuencia"
        value={frequency}
        onChange={(v) => setFrequency(v as IncomeFrequency)}
      >
        <option value="PER_SHIFT">Por trabajo</option>
        <option value="WEEKLY">Semanal</option>
        <option value="MONTHLY">Mensual</option>
        <option value="BIWEEKLY">Quincenal</option>
        <option value="ONE_TIME">Una vez</option>
      </Select>

      <MoneyField
        label="Monto estimado"
        value={estimatedAmount}
        onChange={setEstimatedAmount}
        placeholder="20000"
        hint="Debe ser mayor a 0."
      />

      <Field
        label="Veces al mes (opcional)"
        type="number"
        value={occurrencesPerMonth}
        onChange={setOccurrencesPerMonth}
        placeholder="12"
      />

      {!editing && sources.length > 0 && (
        <Select
          label="Fuente asociada (opcional)"
          value={sourceId}
          onChange={setSourceId}
        >
          <option value="">Ninguna</option>
          {sources.map((s) => (
            <option key={s.id} value={s.id}>
              {s.description}
            </option>
          ))}
        </Select>
      )}

      {error && <InlineAlert message={error} />}

      <button
        className="btn"
        type="submit"
        disabled={busy || !valid}
        style={{ marginTop: 4, opacity: busy || !valid ? 0.5 : 1 }}
      >
        {busy ? "Guardando…" : editing ? "Guardar cambios" : "Guardar gasto"}
      </button>
      <button
        type="button"
        className="btn btn-ghost"
        onClick={onCancel}
        disabled={busy}
        style={{ borderColor: "var(--line)" }}
      >
        Cancelar
      </button>
    </form>
  );
}
