import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, ApiError } from '../lib/api';

const BOT_USERNAME = 'jbyanx_bot';
const TELEGRAM_BASE = 'https://telegram.me'; // t.me falla en algunas redes/DNS

/** Espeja LinkCodeResponse del backend. */
interface LinkCodeResponse {
  code: string;
  expiresInSeconds: number;
}

function mmss(totalSeconds: number): string {
  const m = Math.floor(totalSeconds / 60);
  const s = totalSeconds % 60;
  return String(m).padStart(2, '0') + ':' + String(s).padStart(2, '0');
}

/**
 * Pantalla para vincular la cuenta con el bot de Telegram.
 *
 * No hay endpoint de estado: no sabemos si la cuenta ya está vinculada. Por eso
 * la pantalla no afirma nada sobre el estado; solo ofrece generar un código y,
 * de forma discreta, desvincular.
 */
export default function Telegram() {
  const [code, setCode] = useState<string | null>(null);
  const [remaining, setRemaining] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [unlinked, setUnlinked] = useState(false);

  // Un solo intervalo vivo a la vez. La ref sobrevive a los renders y nos deja
  // limpiarlo tanto al regenerar como al desmontar.
  const tickRef = useRef<ReturnType<typeof setInterval> | null>(null);

  function stopTick() {
    if (tickRef.current !== null) {
      clearInterval(tickRef.current);
      tickRef.current = null;
    }
  }

  // Al desmontar, nunca dejar un timer colgado.
  useEffect(() => stopTick, []);

  const expired = code !== null && remaining <= 0;

  async function generate() {
    setError(null);
    setCopied(false);
    setUnlinked(false);
    setBusy(true);
    try {
      const res = await api.post<LinkCodeResponse>('/api/v1/telegram/link-code');
      stopTick(); // si había un código anterior, su cuenta regresiva muere aquí
      setCode(res.code);
      setRemaining(res.expiresInSeconds);
      tickRef.current = setInterval(() => {
        setRemaining((r) => {
          if (r <= 1) {
            stopTick();
            return 0;
          }
          return r - 1;
        });
      }, 1000);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos generar el código.');
    } finally {
      setBusy(false);
    }
  }

  async function copyCommand() {
    if (!code) return;
    try {
      await navigator.clipboard.writeText('/start ' + code);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      setError('No pudimos copiar. Copia el comando a mano.');
    }
  }

  async function unlink() {
    if (!confirm('¿Desvincular esta cuenta del bot de Telegram?')) return;
    setError(null);
    setBusy(true);
    try {
      await api.del('/api/v1/telegram/unlink');
      stopTick();
      setCode(null);
      setRemaining(0);
      setUnlinked(true);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No pudimos desvincular la cuenta.');
    } finally {
      setBusy(false);
    }
  }

  const telegramUrl = code ? TELEGRAM_BASE + '/' + BOT_USERNAME + '?start=' + code : '#';

  return (
    <main>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <p className="eyebrow">Telegram</p>
        <Link
          to="/"
          className="btn btn-ghost"
          style={{
            width: 'auto',
            padding: '6px 10px',
            fontSize: 13,
            borderRadius: 10,
            border: '1px solid var(--line)',
            textDecoration: 'none'
          }}
        >
          Volver
        </Link>
      </header>

      <section style={{ marginTop: 26 }}>
        <h1
          style={{
            fontFamily: 'var(--display)',
            fontSize: 30,
            fontWeight: 800,
            letterSpacing: '-0.03em',
            lineHeight: 1.1,
            marginBottom: 10
          }}
        >
          Vincula tu Telegram
        </h1>
        <p style={{ color: 'var(--muted)', fontSize: 14, lineHeight: 1.5 }}>
          Registra gastos mandando un mensaje, sin abrir la app.
        </p>
      </section>

      {error && (
        <p
          role="alert"
          style={{
            marginTop: 20,
            color: 'var(--danger)',
            background: 'var(--danger-soft)',
            border: '1px solid var(--danger)',
            borderRadius: 10,
            padding: '10px 12px',
            fontSize: 13,
            lineHeight: 1.4
          }}
        >
          {error}
        </p>
      )}

      {unlinked && (
        <p
          role="status"
          style={{
            marginTop: 20,
            color: 'var(--muted)',
            border: '1px solid var(--line)',
            borderRadius: 10,
            padding: '10px 12px',
            fontSize: 14,
            lineHeight: 1.4
          }}
        >
          Cuenta desvinculada.
        </p>
      )}

      {!code ? (
        <button
          className="btn"
          onClick={generate}
          disabled={busy}
          style={{ marginTop: 24, opacity: busy ? 0.5 : 1 }}
        >
          {busy ? 'Generando…' : 'Generar código'}
        </button>
      ) : (
        <>
          <section className="card" style={{ marginTop: 24, textAlign: 'center' }}>
            <p className="eyebrow">Tu código</p>
            <p
              className="amount"
              style={{
                fontSize: 44,
                fontWeight: 700,
                letterSpacing: '0.08em',
                marginTop: 10,
                opacity: expired ? 0.35 : 1
              }}
            >
              {code}
            </p>
            <p
              className="amount"
              style={{
                fontSize: 14,
                marginTop: 10,
                color: expired ? 'var(--danger)' : 'var(--muted)'
              }}
            >
              {expired ? 'Código vencido' : 'Vence en ' + mmss(remaining)}
            </p>
          </section>

          {expired ? (
            <button
              className="btn"
              onClick={generate}
              disabled={busy}
              style={{ marginTop: 16, opacity: busy ? 0.5 : 1 }}
            >
              {busy ? 'Generando…' : 'Generar de nuevo'}
            </button>
          ) : (
            <div style={{ display: 'grid', gap: 12, marginTop: 16 }}>
              <a
                href={telegramUrl}
                target="_blank"
                rel="noopener noreferrer"
                style={{ textDecoration: 'none' }}
              >
                <button type="button" className="btn">
                  Abrir en Telegram
                </button>
              </a>
              <button
                type="button"
                className="btn btn-ghost"
                onClick={copyCommand}
                style={{ borderColor: 'var(--line)' }}
              >
                {copied ? 'Copiado' : 'Copiar comando'}
              </button>
            </div>
          )}
        </>
      )}

      <p style={{ textAlign: 'center', marginTop: 40 }}>
        <button
          type="button"
          onClick={unlink}
          disabled={busy}
          style={{
            background: 'transparent',
            border: 'none',
            color: 'var(--muted)',
            fontSize: 13,
            textDecoration: 'underline',
            cursor: 'pointer',
            padding: 4
          }}
        >
          Desvincular esta cuenta
        </button>
      </p>
    </main>
  );
}
