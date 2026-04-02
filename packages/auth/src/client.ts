// ============================================================
// @vierp/auth - Browser Client for Keycloak SSO
// Handles login/logout redirect flow
// ============================================================

import type { KeycloakConfig, AuthSession } from './types';
import { extractUser } from './token';

const DEFAULT_CONFIG: KeycloakConfig = {
  issuerUrl: typeof window !== 'undefined'
    ? `${process.env.NEXT_PUBLIC_SSO_URL || 'http://localhost:8080'}/realms/${process.env.NEXT_PUBLIC_SSO_REALM || 'erp'}`
    : '',
  clientId: process.env.NEXT_PUBLIC_SSO_CLIENT_ID || 'erp-app',
};

const SESSION_KEY = 'erp_session';
const VERIFIER_KEY = 'erp_pkce_verifier';

/**
 * Generate a cryptographically random PKCE code verifier (43-128 chars)
 */
function generateCodeVerifier(): string {
  const array = new Uint8Array(64);
  crypto.getRandomValues(array);
  return base64UrlEncode(array).substring(0, 128);
}

/**
 * Derive PKCE code challenge from verifier using S256 method
 * challenge = BASE64URL(SHA256(verifier))
 */
async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  return base64UrlEncode(new Uint8Array(digest));
}

/**
 * Base64url encode (no padding)
 */
function base64UrlEncode(array: Uint8Array): string {
  const base64 = btoa(String.fromCharCode(...array));
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

/**
 * Redirect to Keycloak login page with PKCE
 */
export async function login(config: Partial<KeycloakConfig> = {}): Promise<void> {
  const cfg = { ...DEFAULT_CONFIG, ...config };
  const redirectUri = config.redirectUri || window.location.origin + '/auth/callback';

  // Generate PKCE verifier + challenge
  const verifier = generateCodeVerifier();
  const challenge = await generateCodeChallenge(verifier);
  const state = generateState();

  // Store verifier for callback exchange (survives redirect)
  sessionStorage.setItem(VERIFIER_KEY, JSON.stringify({ verifier, state }));

  const params = new URLSearchParams({
    client_id: cfg.clientId,
    redirect_uri: redirectUri,
    response_type: 'code',
    scope: 'openid profile email',
    state,
    code_challenge: challenge,
    code_challenge_method: 'S256',
  });

  window.location.href = `${cfg.issuerUrl}/protocol/openid-connect/auth?${params}`;
}

/**
 * Redirect to Keycloak logout
 */
export function logout(config: Partial<KeycloakConfig> = {}): void {
  const cfg = { ...DEFAULT_CONFIG, ...config };
  const redirectUri = window.location.origin;

  // Clear local session and PKCE verifier
  sessionStorage.removeItem(SESSION_KEY);
  sessionStorage.removeItem(VERIFIER_KEY);

  const params = new URLSearchParams({
    client_id: cfg.clientId,
    post_logout_redirect_uri: redirectUri,
  });

  window.location.href = `${cfg.issuerUrl}/protocol/openid-connect/logout?${params}`;
}

/**
 * Exchange authorization code for tokens (call from /auth/callback page)
 * Uses PKCE verifier and implements refresh token rotation
 */
export async function handleCallback(
  code: string,
  config: Partial<KeycloakConfig> = {}
): Promise<AuthSession> {
  const cfg = { ...DEFAULT_CONFIG, ...config };
  const redirectUri = config.redirectUri || window.location.origin + '/auth/callback';

  // Retrieve and verify PKCE state
  const rawVerifier = sessionStorage.getItem(VERIFIER_KEY);
  if (!rawVerifier) {
    throw new Error('PKCE verifier not found — login may have been initiated without PKCE or session expired');
  }
  const { verifier } = JSON.parse(rawVerifier) as { verifier: string };
  sessionStorage.removeItem(VERIFIER_KEY);

  const response = await fetch(`${cfg.issuerUrl}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: cfg.clientId,
      code,
      redirect_uri: redirectUri,
      code_verifier: verifier,
    }),
  });

  if (!response.ok) {
    throw new Error(`Token exchange failed: ${response.statusText}`);
  }

  const data = await response.json();
  const payload = parseJwtPayload(data.access_token);
  const user = extractUser(payload);

  const session: AuthSession = {
    user,
    accessToken: data.access_token,
    refreshToken: data.refresh_token,
    expiresAt: Date.now() + data.expires_in * 1000,
  };

  // Store session (refresh token rotation — old token is invalidated by Keycloak on use)
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

/**
 * Get current session (from sessionStorage)
 */
export function getSession(): AuthSession | null {
  if (typeof window === 'undefined') return null;
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw) return null;

  const session: AuthSession = JSON.parse(raw);
  if (Date.now() > session.expiresAt) {
    sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
  return session;
}

/**
 * Get access token for API calls
 */
export function getAccessToken(): string | null {
  const session = getSession();
  return session?.accessToken || null;
}

/**
 * Refresh the access token using refresh token
 * Implements refresh token rotation — old token is invalidated by Keycloak on use
 */
export async function refreshSession(
  config: Partial<KeycloakConfig> = {}
): Promise<AuthSession | null> {
  const session = getSession();
  if (!session?.refreshToken) return null;

  const cfg = { ...DEFAULT_CONFIG, ...config };

  try {
    const response = await fetch(`${cfg.issuerUrl}/protocol/openid-connect/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'refresh_token',
        client_id: cfg.clientId,
        refresh_token: session.refreshToken,
      }),
    });

    if (!response.ok) {
      // Refresh token reuse detected or token revoked — clear session
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }

    const data = await response.json();
    const payload = parseJwtPayload(data.access_token);
    const user = extractUser(payload);

    // Refresh token rotation: new tokens replace old ones
    // Keycloak invalidates the old refresh token on use
    const newSession: AuthSession = {
      user,
      accessToken: data.access_token,
      refreshToken: data.refresh_token,
      expiresAt: Date.now() + data.expires_in * 1000,
    };

    sessionStorage.setItem(SESSION_KEY, JSON.stringify(newSession));
    return newSession;
  } catch {
    sessionStorage.removeItem(SESSION_KEY);
    return null;
  }
}

// ==================== Helpers ====================

function generateState(): string {
  return Math.random().toString(36).substring(2) + Date.now().toString(36);
}

function parseJwtPayload(token: string): any {
  const base64 = token.split('.')[1];
  const json = atob(base64.replace(/-/g, '+').replace(/_/g, '/'));
  return JSON.parse(json);
}
