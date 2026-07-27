import type { AuthResponse } from '../types';

/**
 * Cliente HTTP de Finsight.
 */

const ACCESS = 'finsight.access';
const REFRESH = 'finsight.refresh';

// Leemos la URL base del backend desde las variables de entorno de Vite
const API_BASE_URL = import.meta.env.VITE_API_URL || '';

// Si un fetch tarda más que esto, lo abortamos. Sin esto, una pestaña que
// el navegador suspende en segundo plano puede dejar la promesa colgada
// para siempre — el componente se queda en "Cargando…" sin error ni éxito,
// y al volver a la pestaña no hay nada que dispare el refresh de sesión.
const REQUEST_TIMEOUT_MS = 15000;

export const tokens = {
  get access() {
    return localStorage.getItem(ACCESS);
  },
  get refresh() {
    return localStorage.getItem(REFRESH);
  },
  save(access: string, refresh: string) {
    localStorage.setItem(ACCESS, access);
    localStorage.setItem(REFRESH, refresh);
  },
  clear() {
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
  }
};

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

/** Refresh compartido en vuelo. null = no hay ninguno corriendo. */
let inFlight: Promise<string> | null = null;

/** Se dispara cuando el refresh falla de verdad: hay que volver al login. */
let onSessionLost: () => void = () => {};

export function setOnSessionLost(fn: () => void) {
  onSessionLost = fn;
}

/** fetch con límite de tiempo — nunca deja una promesa colgada para siempre. */
async function fetchWithTimeout(url: string, init: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } finally {
    clearTimeout(timeoutId);
  }
}

async function refreshTokens(): Promise<string> {
  const refreshToken = tokens.refresh;
  if (!refreshToken) throw new Error('no refresh token');

  let res: Response;
  try {
    res = await fetchWithTimeout(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'X-Refresh-Token': refreshToken }
    });
  } catch {
    // Se colgó o abortó por timeout — es lo mismo que un refresh fallido:
    // no dejamos al usuario esperando indefinidamente, cortamos la sesión.
    tokens.clear();
    onSessionLost();
    throw new Error('refresh failed');
  }

  if (!res.ok) {
    tokens.clear();
    onSessionLost();
    throw new Error('refresh failed');
  }

  const data: AuthResponse = await res.json();
  tokens.save(data.accessToken, data.refreshToken);
  return data.accessToken;
}

/** Garantiza un solo refresh concurrente. */
function refreshOnce(): Promise<string> {
  if (!inFlight) {
    inFlight = refreshTokens().finally(() => {
      inFlight = null;
    });
  }
  return inFlight;
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const access = tokens.access;
  const url = `${API_BASE_URL}${path}`;

  let res: Response;
  try {
    res = await fetchWithTimeout(url, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(access ? { Authorization: 'Bearer ' + access } : {}),
        ...init.headers
      }
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new ApiError(0, 'La solicitud tardó demasiado. Revisa tu conexión e intenta de nuevo.');
    }
    throw new ApiError(0, 'No pudimos conectar con el servidor. Revisa tu conexión.');
  }

  if (res.status === 401 && retry && tokens.refresh) {
    await refreshOnce();
    return request<T>(path, init, false);
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new ApiError(res.status, body.message ?? 'Error ' + res.status);
  }

  if (res.status === 204) return undefined as T;
  return res.json();
}

export const api = {
  get: <T,>(path: string) => request<T>(path),
  post: <T,>(path: string, body?: unknown) =>
    request<T>(path, {
      method: 'POST',
      body: body ? JSON.stringify(body) : undefined
    }),
  patch: <T,>(path: string, body: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  del: <T,>(path: string) => request<T>(path, { method: 'DELETE' })
};