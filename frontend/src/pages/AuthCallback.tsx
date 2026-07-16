import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { tokens } from '../lib/api';
import { useAuth } from '../auth/AuthContext';

/**
 * Aterrizaje del OAuth. El backend redirige aquí con los tokens en el FRAGMENTO
 * de la URL (#accessToken=...&refreshToken=...).
 *
 * El fragmento nunca viaja al servidor: no queda en logs ni en referers. Lo
 * leemos, lo guardamos, y lo BORRAMOS de la barra con replaceState para que no
 * sobreviva en el historial del navegador.
 */
export default function AuthCallback() {
  const navigate = useNavigate();
  const { reload } = useAuth();

  useEffect(() => {
    const params = new URLSearchParams(window.location.hash.slice(1));
    const access = params.get('accessToken');
    const refresh = params.get('refreshToken');

    // Borrar el fragmento ANTES de cualquier await: no debe sobrevivir ni un
    // instante en la barra de direcciones.
    window.history.replaceState(null, '', window.location.pathname);

    if (!access || !refresh) {
      navigate('/login?error=No pudimos completar el inicio de sesión', { replace: true });
      return;
    }

    tokens.save(access, refresh);
    reload().then(() => navigate('/', { replace: true }));
  }, [navigate, reload]);

  return (
    <main style={{ display: 'grid', placeItems: 'center', minHeight: '60dvh' }}>
      <p style={{ color: 'var(--muted)' }}>Entrando…</p>
    </main>
  );
}