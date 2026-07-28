import { useState } from "react";
import type { FormEvent } from "react";
import { useNavigate, Link } from "react-router-dom";
import { api, tokens, ApiError } from "../lib/api";
import { useAuth } from "../auth/AuthContext";
import { Field } from "./Login";
import type { AuthResponse } from "../types";

export default function Register() {
  const navigate = useNavigate();
  const { reload } = useAuth();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [repeatPassword, setRepeatPassword] = useState("");

  // Espeja la validación del backend: no dejamos mandar algo que va a rebotar.
  const tooShort = password.length > 0 && password.length < 8;
  const mismatch = repeatPassword.length > 0 && password !== repeatPassword;
  const canSubmit =
    name.trim() &&
    email.trim() &&
    password.length >= 8 &&
    password === repeatPassword &&
    !busy;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      // /register devuelve un AuthResponse: queda logueado de una.
      const res = await api.post<AuthResponse>("/api/v1/auth/register", {
        name: name.trim(),
        email: email.trim(),
        password,
      });
      tokens.save(res.accessToken, res.refreshToken);
      await reload();
      navigate("/", { replace: true });
    } catch (err) {
      // El backend sabe si el correo ya existe: mostramos su mensaje.
      setError(
        err instanceof ApiError ? err.message : "No pudimos crear tu cuenta.",
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
            fontSize: 34,
            lineHeight: 1.05,
            fontWeight: 800,
            letterSpacing: "-0.03em",
            marginTop: 12,
            marginBottom: 26,
          }}
        >
          Crea tu cuenta.
        </h1>

        <form onSubmit={onSubmit} style={{ display: "grid", gap: 12 }}>
          <Field
            label="Nombre"
            type="text"
            value={name}
            onChange={setName}
            autoComplete="name"
            placeholder="Tu nombre"
          />
          <Field
            label="Correo"
            type="email"
            value={email}
            onChange={setEmail}
            autoComplete="email"
            placeholder="tu@correo.com"
          />
          <Field
            label="Contraseña"
            type="password"
            value={password}
            onChange={setPassword}
            autoComplete="new-password"
            placeholder="••••••••"
            hint={tooShort ? "Mínimo 8 caracteres." : "Al menos 8 caracteres."}
          />
          <Field
            label="Confirmar contraseña"
            type="password"
            value={repeatPassword}
            onChange={setRepeatPassword}
            autoComplete="new-password"
            placeholder="••••••••"
            hint={mismatch ? "Las contraseñas no coinciden." : undefined}
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
            disabled={!canSubmit}
            style={{ marginTop: 4, opacity: canSubmit ? 1 : 0.5 }}
          >
            {busy ? "Creando…" : "Crear cuenta"}
          </button>
        </form>
      </div>

      <p
        style={{
          textAlign: "center",
          fontSize: 14,
          color: "var(--muted)",
          marginTop: 20,
        }}
      >
        ¿Ya tienes cuenta?{" "}
        <Link to="/login" style={{ color: "var(--ink)", fontWeight: 600 }}>
          Entrar
        </Link>
      </p>
    </main>
  );
}
