'use client';

export type UserRole = 'ADMIN' | 'DEVELOPER' | 'OPERATOR';

export interface AuthUser {
  username: string;
  role: UserRole;
  token?: string;
}

export const PRESET_USERS: Record<UserRole, { username: string; password: string; role: UserRole; label: string }> = {
  ADMIN: { username: 'admin', password: 'admin123', role: 'ADMIN', label: 'Admin (Full Access)' },
  DEVELOPER: { username: 'developer', password: 'dev123', role: 'DEVELOPER', label: 'Developer (Jobs & Policies)' },
  OPERATOR: { username: 'operator', password: 'op123', role: 'OPERATOR', label: 'Operator (Queues & DLQ)' },
};

const TOKEN_STORAGE_KEY = 'djs_auth_token';
const USER_STORAGE_KEY = 'djs_auth_user';

export function getStoredToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function getStoredUser(): AuthUser {
  if (typeof window === 'undefined') return { username: 'admin', role: 'ADMIN' };
  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch (e) {
    // fallback
  }
  return { username: 'admin', role: 'ADMIN' };
}

export function saveAuthSession(user: AuthUser, token: string) {
  if (typeof window === 'undefined') return;
  localStorage.setItem(TOKEN_STORAGE_KEY, token);
  localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(user));
}

export function clearAuthSession() {
  if (typeof window === 'undefined') return;
  localStorage.removeItem(TOKEN_STORAGE_KEY);
  localStorage.removeItem(USER_STORAGE_KEY);
}

const KEYCLOAK_BASE_URL = process.env.NEXT_PUBLIC_KEYCLOAK_URL || 'http://localhost:8180';

/**
 * Fetch JWT token directly from Keycloak (port 8180 or Cloud URL).
 */
export async function loginWithKeycloak(role: UserRole = 'ADMIN'): Promise<{ success: boolean; token?: string; error?: string }> {
  const credentials = PRESET_USERS[role];
  try {
    const res = await fetch(`${KEYCLOAK_BASE_URL}/realms/distributed-scheduler-realm/protocol/openid-connect/token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      body: new URLSearchParams({
        client_id: 'job-scheduler-client',
        username: credentials.username,
        password: credentials.password,
        grant_type: 'password',
      }),
    });

    if (!res.ok) {
      const err = await res.text();
      return { success: false, error: `Keycloak returned status ${res.status}: ${err}` };
    }

    const data = await res.json();
    const token = data.access_token;
    saveAuthSession({ username: credentials.username, role: credentials.role }, token);
    return { success: true, token };
  } catch (err: any) {
    return { success: false, error: err.message || 'Could not connect to Keycloak' };
  }
}
