import type { AuthResponse } from '../types';

/**
 * Cliente HTTP de Finsight.
 *
 * EL PROBLEMA QUE RESUELVE (y por qué no es un fetch pelado):
 *
 * El backend usa REFRESH TOKEN ROTATION: cada vez que usas un refresh token, el
 * backend lo invalida y emite uno nuevo. Es lo correcto en seguridad, pero rompe
 * el patrón ingenuo de "si da 401, refresca".
 *
 * Si el dashboard dispara 3 peticiones en paralelo y el access token venció, las
 * 3 dan 401 y las 3 intentan refrescar con el MISMO refresh token. La primera lo
 * consume y lo rota; la segunda y la tercera usan un token ya muerto → 401 → el
 * usuario queda deslogueado sin razón aparente.
 *
 * Solución: un único refresh en vuelo. La primera petición que ve el 401 arranca
 * el refresh; las demás se cuelgan de esa MISMA promesa y esperan. Cuando
 * resuelve, todas reintentan con el token nuevo. Un solo uso del refresh token,
 * cero deslogueos espurios.
 */

const ACCESS = 'finsight.access';
const REFRESH = 'finsight.refresh';

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

async function refreshTokens(): Promise<string> {
  const refreshToken = tokens.refresh;
  if (!refreshToken) throw new Error('no refresh token');

  // El backend espera el refresh token en el header X-Refresh-Token (ver
  // AuthController#refresh), NO en el body. Mandarlo en el body daba 500 y
  // deslogueaba al usuario en cada vencimiento del access token.
  const res = await fetch('/api/v1/auth/refresh', {
    method: 'POST',
    headers: { 'X-Refresh-Token': refreshToken }
  });

  if (!res.ok) {
    tokens.clear();
    onSessionLost();
    throw new Error('refresh failed');
  }

  const data: AuthResponse = await res.json();
  tokens.save(data.accessToken, data.refreshToken); // el refresh ROTÓ
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

  const res = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(access ? { Authorization: 'Bearer ' + access } : {}),
      ...init.headers
    }
  });

  if (res.status === 401 && retry && tokens.refresh) {
    await refreshOnce(); // si ya hay uno corriendo, se cuelga de él
    return request<T>(path, init, false); // un solo reintento, sin loops
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