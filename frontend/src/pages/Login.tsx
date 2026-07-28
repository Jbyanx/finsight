import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, useSearchParams, Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../lib/api";

export default function Login() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  // El error puede venir del formulario o del rebote de Google (?error=...)
  const [error, setError] = useState<string | null>(params.get("error"));

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(email.trim(), password);
      navigate("/", { replace: true });
    } catch (err) {
      // El backend distingue "credenciales inválidas" de "esta cuenta usa Google".
      // Mostramos su mensaje: sabe más que nosotros.
      setError(
        err instanceof ApiError ? err.message : "No pudimos iniciar sesión.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <main
      style={{ display: "flex", flexDirection: "column", minHeight: "92dvh" }}
    >
      <div
        style={{
          flex: 1,
          display: "flex",
          flexDirection: "column",
          justifyContent: "center",
        }}
      >
        <p className="eyebrow">Finsight</p>
        <h1
          style={{
            fontFamily: "var(--display)",
            fontSize: 38,
            lineHeight: 1.05,
            fontWeight: 800,
            letterSpacing: "-0.03em",
            marginTop: 12,
            marginBottom: 26,
          }}
        >
          Sabes con qué
          <br />
          pagar hoy.
        </h1>

        <form onSubmit={onSubmit} style={{ display: "grid", gap: 12 }}>
          <Field
            label="Correo"
            type="email"
            value={email}
            onChange={setEmail}
            autoComplete="username"
            placeholder="tu@correo.com"
          />
          <Field
            label="Contraseña"
            type="password"
            value={password}
            onChange={setPassword}
            autoComplete="current-password"
            placeholder="••••••••"
          />

          {error && (
            <p
              role="alert"
              style={{
                color: "var(--danger)",
                background: "var(--danger-soft)",
                border: "1px solid var(--danger)",
                borderRadius: 10,
                padding: "10px 12px",
                fontSize: 13,
                lineHeight: 1.4,
              }}
            >
              {error}
            </p>
          )}

          <button
            className="btn"
            type="submit"
            disabled={busy || !email || !password}
            style={{
              marginTop: 4,
              opacity: busy || !email || !password ? 0.5 : 1,
            }}
          >
            {busy ? "Entrando…" : "Entrar"}
          </button>
        </form>

        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 12,
            margin: "20px 0",
          }}
        >
          <span style={{ flex: 1, height: 1, background: "var(--line)" }} />
          <span style={{ fontSize: 12, color: "var(--muted)" }}>o</span>
          <span style={{ flex: 1, height: 1, background: "var(--line)" }} />
        </div>

        {/* Navegación completa, no fetch: el OAuth necesita al navegador. */}
        <a
          href={`${import.meta.env.VITE_API_URL}/oauth2/authorization/google`}
          style={{ textDecoration: "none" }}
        >
          <button
            type="button"
            className="btn btn-ghost"
            style={{ borderColor: "var(--line)" }}
          >
            Entrar con Google
          </button>
        </a>
      </div>

      <p
        style={{
          textAlign: "center",
          fontSize: 14,
          color: "var(--muted)",
          marginTop: 20,
        }}
      >
        ¿No tienes cuenta?{" "}
        <Link to="/register" style={{ color: "var(--ink)", fontWeight: 600 }}>
          Crear una
        </Link>
      </p>

      <p
        style={{
          textAlign: "center",
          fontSize: 12,
          color: "var(--muted)",
          marginTop: 24,
        }}
      >
        Proyecto destacado en{" "}
        <a
          href="https://jbyanx.dev"
          target="_blank"
          rel="noopener"
          style={{ color: "var(--muted)", textDecoration: "underline" }}
        >
          jbyanx.dev
        </a>
      </p>
    </main>
  );
}

export function Field({
  label,
  type,
  value,
  onChange,
  autoComplete,
  placeholder,
  hint,
}: {
  label: string;
  type: string;
  value: string;
  onChange: (v: string) => void;
  autoComplete?: string;
  placeholder?: string;
  hint?: string;
}) {
  return (
    <label style={{ display: "grid", gap: 6 }}>
      <span className="eyebrow">{label}</span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        autoComplete={autoComplete}
        placeholder={placeholder}
        // En los campos numéricos, la rueda del mouse cambia el valor cuando el
        // input está enfocado: al desplazarse por la pantalla se alteran saldos
        // sin querer. Soltamos el foco antes de que la rueda actúe.
        onWheel={type === "number" ? (e) => e.currentTarget.blur() : undefined}
        style={{
          font: "inherit",
          fontSize: 16, // menos de 16px hace que iOS haga zoom al enfocar
          padding: "13px 14px",
          border: "1px solid var(--line)",
          borderRadius: 10,
          background: "var(--card)",
          color: "var(--ink)",
          width: "100%",
        }}
      />
      {hint && (
        <span style={{ fontSize: 12, color: "var(--muted)" }}>{hint}</span>
      )}
    </label>
  );
}
