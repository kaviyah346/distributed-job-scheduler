'use client';

import React, { createContext, useContext, useEffect, useState, useRef, ReactNode } from 'react';
import keycloak from './keycloak';

export type UserRole = 'ADMIN' | 'DEVELOPER' | 'OPERATOR' | string;

export interface AuthUser {
  username: string;
  roles: string[];
  role: UserRole;
  email?: string;
  token?: string;
}

interface AuthContextType {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: AuthUser | null;
  token: string | null;
  login: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType>({
  isAuthenticated: false,
  isLoading: true,
  user: null,
  token: null,
  login: async () => {},
  logout: async () => {},
});

const TOKEN_STORAGE_KEY = 'djs_auth_token';
const USER_STORAGE_KEY = 'djs_auth_user';

export function getStoredToken(): string | null {
  if (typeof window === 'undefined') return null;
  if (keycloak && keycloak.token) {
    return keycloak.token;
  }
  return localStorage.getItem(TOKEN_STORAGE_KEY);
}

export async function getToken(): Promise<string | null> {
  if (typeof window === 'undefined') return null;
  if (!keycloak) return localStorage.getItem(TOKEN_STORAGE_KEY);

  try {
    if (keycloak.authenticated) {
      // Refresh token if it will expire within 30 seconds
      await keycloak.updateToken(30);
      if (keycloak.token) {
        localStorage.setItem(TOKEN_STORAGE_KEY, keycloak.token);
        return keycloak.token;
      }
    }
  } catch (err) {
    console.warn('Failed to refresh Keycloak token:', err);
  }

  return keycloak.token || localStorage.getItem(TOKEN_STORAGE_KEY);
}

export function getStoredUser(): AuthUser | null {
  if (typeof window === 'undefined') return null;

  try {
    const raw = localStorage.getItem(USER_STORAGE_KEY);
    if (raw) return JSON.parse(raw);
  } catch {
    // Ignore invalid stored data
  }

  return null;
}

function extractRoles(tokenParsed: any): string[] {
  if (!tokenParsed) return [];
  const roles = new Set<string>();

  if (Array.isArray(tokenParsed.realm_access?.roles)) {
    tokenParsed.realm_access.roles.forEach((r: string) => roles.add(r));
  }

  const clientId = process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || 'job-scheduler-client';
  if (Array.isArray(tokenParsed.resource_access?.[clientId]?.roles)) {
    tokenParsed.resource_access[clientId].roles.forEach((r: string) => roles.add(r));
  }

  return Array.from(roles);
}

function determinePrimaryRole(roles: string[]): UserRole {
  const upperRoles = roles.map((r) => r.toUpperCase());
  if (upperRoles.includes('ADMIN') || upperRoles.includes('ROLE_ADMIN')) return 'ADMIN';
  if (upperRoles.includes('DEVELOPER') || upperRoles.includes('ROLE_DEVELOPER')) return 'DEVELOPER';
  if (upperRoles.includes('OPERATOR') || upperRoles.includes('ROLE_OPERATOR')) return 'OPERATOR';

  const meaningfulRoles = upperRoles.filter(
    (r) => !r.startsWith('DEFAULT-') && !r.startsWith('OFFLINE_') && !r.startsWith('UMA_')
  );

  return (meaningfulRoles[0] as UserRole) || 'OPERATOR';
}

function buildAuthUser(keycloakInstance: typeof keycloak): AuthUser | null {
  if (!keycloakInstance || !keycloakInstance.tokenParsed) return null;

  const parsed = keycloakInstance.tokenParsed;
  const username =
    parsed.preferred_username ||
    parsed.name ||
    parsed.email ||
    parsed.sub ||
    'User';

  const roles = extractRoles(parsed);
  const primaryRole = determinePrimaryRole(roles);

  return {
    username,
    roles,
    role: primaryRole,
    email: parsed.email,
    token: keycloakInstance.token,
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const isInitializing = useRef(false);

  useEffect(() => {
    const kc = keycloak;
    if (!kc || isInitializing.current) return;
    isInitializing.current = true;

    kc.init({
      onLoad: 'login-required',
      checkLoginIframe: false,
      pkceMethod: 'S256',
    })
      .then((authenticated) => {
        setIsAuthenticated(authenticated);
        if (authenticated && kc.token) {
          const authUser = buildAuthUser(kc);
          setUser(authUser);
          setToken(kc.token);
          localStorage.setItem(TOKEN_STORAGE_KEY, kc.token);
          if (authUser) {
            localStorage.setItem(USER_STORAGE_KEY, JSON.stringify(authUser));
          }
        }
        setIsLoading(false);
      })
      .catch((err) => {
        console.error('Keycloak initialization failed:', err);
        setIsLoading(false);
      });

    kc.onTokenExpired = () => {
      kc.updateToken(30)
        .then((refreshed) => {
          if (refreshed && kc.token) {
            setToken(kc.token);
            localStorage.setItem(TOKEN_STORAGE_KEY, kc.token);
          }
        })
        .catch(() => {
          console.warn('Session expired, redirecting to Keycloak login');
          kc.login();
        });
    };
  }, []);

  const login = async () => {
    if (keycloak) {
      await keycloak.login({
        redirectUri: window.location.origin,
      });
    }
  };

  const logout = async () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      localStorage.removeItem(USER_STORAGE_KEY);
    }
    if (keycloak) {
      await keycloak.logout({
        redirectUri: window.location.origin,
      });
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col items-center justify-center text-slate-100 font-sans">
        <div className="flex flex-col items-center gap-4 p-8 rounded-2xl border border-slate-800 bg-slate-900/60 shadow-2xl backdrop-blur-md max-w-sm text-center">
          <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-blue-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-blue-500/25 animate-pulse">
            <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin" />
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-100">Authenticating Session</h2>
            <p className="text-xs text-slate-400 mt-1">Connecting to Keycloak OIDC Security Realm...</p>
          </div>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        isLoading,
        user,
        token,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}

export async function loginWithKeycloak(): Promise<boolean> {
  if (keycloak) {
    await keycloak.login({
      redirectUri: window.location.origin,
    });
    return true;
  }
  return false;
}

export function logoutFromKeycloak() {
  if (typeof window !== 'undefined') {
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    localStorage.removeItem(USER_STORAGE_KEY);
  }
  if (keycloak) {
    keycloak.logout({
      redirectUri: window.location.origin,
    });
  }
}
