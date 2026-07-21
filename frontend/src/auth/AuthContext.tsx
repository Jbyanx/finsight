import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { api, tokens, setOnSessionLost } from '../lib/api';
import type { AuthResponse, User } from '../types';

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  reload: () => Promise<void>;
}

const Ctx = createContext<AuthState>({
  user: null,
  loading: true,
  login: async () => {},
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

  /**
   * El backend responde lo MISMO que el flujo OAuth: un AuthResponse con los dos
   * tokens y el usuario. Por eso no hace falta un /me después: el usuario ya viene.
   */
  const login = useCallback(async (email: string, password: string) => {
    const res = await api.post<AuthResponse>('/api/v1/auth/login', { email, password });
    tokens.save(res.accessToken, res.refreshToken);
    setUser(res.user);
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/api/v1/auth/logout', { refreshToken: tokens.refresh });
    } catch {
      // Aunque el backend falle, la sesión local se corta igual.
    }
    tokens.clear();
    setUser(null);
  }, []);

  return <Ctx.Provider value={{ user, loading, login, logout, reload }}>{children}</Ctx.Provider>;
}