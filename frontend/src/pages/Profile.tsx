import { useEffect, useState } from 'react';
import { api } from '../lib/api';
import { useAuth } from '../auth/AuthContext';
import { Pager } from '../components/Pager';
import { InlineAlert } from '../components/InlineAlert';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { errorMessage } from '../lib/errorMessage';
import type { User, Session, Page } from '../types';

/* ============================================================
   Perfil — identidad, sesiones activas, y las dos formas de salir.

   El eje de diseño: "cerrar esta sesión" y "cerrar todas" son acciones
   con consecuencias muy distintas — una te saca de este dispositivo,
   la otra te saca de todos los que tengas abiertos (celular, laptop).
   Por eso van separadas, con la segunda marcada como peligrosa y con
   confirmación explícita.
   ============================================================ */

const PAGE_SIZE = 10;

const TYPE_LABELS: Record<string, string> = {
  ACCESS: 'Sesión activa',
  REFRESH: 'Renovación automática'
};

const smallBtn = {
  width: 'auto',
  padding: '6px 12px',
  fontSize: 13,
  borderColor: 'var(--line)'
} as const;

export default function Profile() {
  const { logout } = useAuth();
  const [me, setMe] = useState<User | null>(null);
  const [sessions, setSessions] = useState<Page<Session> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pendingLogoutAll, setPendingLogoutAll] = useState(false);
  const [busy, setBusy] = useState(false);

  async function loadSessions(pageNumber = 0) {
    try {
      const data = await api.get<Page<Session>>(
        `/api/v1/auth/sessions?page=${pageNumber}&size=${PAGE_SIZE}`
      );
      setSessions(data);
    } catch (err) {
      setError(errorMessage(err, 'No pudimos cargar tus sesiones.'));
    }
  }

  useEffect(() => {
    api.get<User>('/api/v1/auth/me')
      .then(setMe)
      .catch((err) => setError(errorMessage(err, 'No pudimos cargar tu perfil.')));
    loadSessions(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function handleLogoutAll() {
    setPendingLogoutAll(false);
    setBusy(true);
    try {
      await api.post('/api/v1/auth/logout-all', undefined);
    } finally {
      // logout() ya limpia el estado local y navega a /login. Si el
      // access token de este dispositivo ya quedó revocado por la línea
      // de arriba, revocarlo de nuevo aquí es inofensivo (idempotente).
      await logout();
    }
  }

  if (error && !me) {
    return (
      <main>
        <InlineAlert message={error} />
      </main>
    );
  }

  if (!me) {
    return (
      <main>
        <p style={{ color: 'var(--muted)' }}>Cargando…</p>
      </main>
    );
  }

  return (
    <main>
      <header>
        <p className="eyebrow">Tu perfil</p>
      </header>

      {error && me && (
        <div style={{ marginTop: 16 }}>
          <InlineAlert message={error} />
        </div>
      )}

      <div className="card" style={{ marginTop: 22, display: 'flex', alignItems: 'center', gap: 14 }}>
        {me.pictureUrl && (
          <img
            src={me.pictureUrl}
            alt=""
            style={{ width: 48, height: 48, borderRadius: '50%', objectFit: 'cover' }}
          />
        )}
        <div style={{ minWidth: 0 }}>
          <p style={{ fontWeight: 600, fontSize: 16 }}>{me.name}</p>
          <p className="eyebrow" style={{ marginTop: 2 }}>{me.email}</p>
        </div>
      </div>
      <p style={{ marginTop: 8, fontSize: 12, color: 'var(--muted)' }}>
        {me.provider === 'google' ? 'Cuenta de Google' : 'Cuenta con contraseña'}
      </p>

      <section style={{ marginTop: 26 }}>
        <p className="eyebrow" style={{ marginBottom: 12 }}>Sesiones activas</p>

        {!sessions && <p style={{ color: 'var(--muted)', fontSize: 14 }}>Cargando…</p>}

        {sessions && sessions.content.length === 0 && (
          <div className="card" style={{ color: 'var(--muted)', fontSize: 14 }}>
            Sin sesiones registradas.
          </div>
        )}

        {sessions && sessions.content.length > 0 && (
          <div style={{ display: 'grid', gap: 10 }}>
            {sessions.content.map((s) => (
              <div key={s.id} className="card">
                <p style={{ fontWeight: 600, fontSize: 14 }}>
                  {s.deviceInfo || 'Dispositivo desconocido'}
                </p>
                <p className="eyebrow" style={{ marginTop: 4 }}>
                  {TYPE_LABELS[s.type] || s.type}
                  {' · iniciada el '}
                  {new Date(s.createdAt).toLocaleDateString('es-CO', { day: 'numeric', month: 'short' })}
                </p>
              </div>
            ))}
          </div>
        )}

        {sessions && <Pager page={sessions} onChange={loadSessions} />}
      </section>

      <section style={{ marginTop: 26, display: 'grid', gap: 10 }}>
        <button
          className="btn btn-ghost"
          onClick={() => logout()}
          disabled={busy}
          style={smallBtn}
        >
          Cerrar esta sesión
        </button>
        <button
          className="btn btn-ghost"
          onClick={() => setPendingLogoutAll(true)}
          disabled={busy}
          style={{ ...smallBtn, color: 'var(--danger)' }}
        >
          {busy ? 'Cerrando…' : 'Cerrar todas las sesiones'}
        </button>
      </section>

      {pendingLogoutAll && (
        <ConfirmDialog
          title="Cerrar todas las sesiones"
          message="Vas a salir de todos tus dispositivos, incluido este. Tendrás que iniciar sesión de nuevo en cada uno."
          confirmLabel="Cerrar todas"
          tone="danger"
          onConfirm={handleLogoutAll}
          onCancel={() => setPendingLogoutAll(false)}
        />
      )}
    </main>
  );
}