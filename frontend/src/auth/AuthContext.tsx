import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { api, tokens, setOnSessionLost } from '../lib/api';
import type { User } from '../types';

interface AuthState {
  user: User | null;
  loading: boolean;
  logout: () => Promise<void>;
  reload: () => Promise<void>;
}

const Ctx = createContext<AuthState>({
  user: null,
  loading: true,
  logout: async () => {},
  reload: async () => {}
});

export const useAuth = () => useContext(Ctx);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  const reload = useCallback(async () => {
    if (!tokens.access) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const me = await api.get<User>('/api/v1/auth/me');
      setUser(me);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // Si el refresh muere (expiró o fue revocado), el cliente avisa.
    setOnSessionLost(() => setUser(null));
    reload();
  }, [reload]);

  const logout = useCallback(async () => {
    try {
      await api.post('/api/v1/auth/logout', { refreshToken: tokens.refresh });
    } catch {
      // Aunque el backend falle, la sesión local se corta igual.
    }
    tokens.clear();
    setUser(null);
  }, []);

  return <Ctx.Provider value={{ user, loading, logout, reload }}>{children}</Ctx.Provider>;
}