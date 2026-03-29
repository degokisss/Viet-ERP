/**
 * CSRF Protection
 * Double-submit cookie pattern with token rotation support
 */

import * as crypto from 'crypto';

export interface CSRFTokenOptions {
  /** Token length in bytes (default: 32) */
  tokenLength?: number;
  /** Cookie name for CSRF token (default: 'csrf-token') */
  cookieName?: string;
  /** Header name for CSRF token (default: 'X-CSRF-Token') */
  headerName?: string;
  /** Token rotation enabled (default: true) */
  rotateToken?: boolean;
  /** Cookie max age in seconds (default: 86400 = 24 hours) */
  maxAge?: number;
  /** Secure cookie flag (default: true in production) */
  secure?: boolean;
  /** SameSite cookie attribute (default: 'Strict') */
  sameSite?: 'Strict' | 'Lax' | 'None';
}

/**
 * CSRF token state for validation
 */
export interface CSRFTokenState {
  token: string;
  createdAt: number;
  lastRotated: number;
}

const TOKEN_STORE = new Map<string, CSRFTokenState>();
const CLEANUP_INTERVAL = 3600000; // 1 hour
const TOKEN_EXPIRY = 86400000; // 24 hours

/**
 * Initialize CSRF protection - cleans up expired tokens
 */
let cleanupStarted = false;

function initializeCleanup(): void {
  if (cleanupStarted) return;
  cleanupStarted = true;

  setInterval(() => {
    const now = Date.now();
    for (const [sessionId, state] of TOKEN_STORE.entries()) {
      if (now - state.createdAt > TOKEN_EXPIRY) {
        TOKEN_STORE.delete(sessionId);
      }
    }
  }, CLEANUP_INTERVAL);
}

/**
 * Generate a new CSRF token
 */
export function generateCSRFToken(sessionId: string): string {
  initializeCleanup();

  const token = crypto.randomBytes(32).toString('hex');
  const now = Date.now();

  TOKEN_STORE.set(sessionId, {
    token,
    createdAt: now,
    lastRotated: now,
  });

  return token;
}

/**
 * Validate CSRF token
 * Returns true if valid, false otherwise
 */
export function validateCSRFToken(
  sessionId: string,
  token: string
): boolean {
  const state = TOKEN_STORE.get(sessionId);

  if (!state) {
    return false;
  }

  const now = Date.now();

  // Check token expiry
  if (now - state.createdAt > TOKEN_EXPIRY) {
    TOKEN_STORE.delete(sessionId);
    return false;
  }

  // Constant-time comparison to prevent timing attacks
  const storedToken = state.token;
  if (storedToken.length !== token.length) {
    return false;
  }

  let result = 0;
  for (let i = 0; i < storedToken.length; i++) {
    result |= storedToken.charCodeAt(i) ^ token.charCodeAt(i);
  }

  return result === 0;
}

/**
 * Rotate CSRF token - invalidates old token and generates new one
 */
export function rotateCSRFToken(sessionId: string): string {
  const oldState = TOKEN_STORE.get(sessionId);

  if (oldState) {
    const now = Date.now();
    if (now - oldState.createdAt > TOKEN_EXPIRY) {
      TOKEN_STORE.delete(sessionId);
    }
  }

  return generateCSRFToken(sessionId);
}

/**
 * Get CSRF token without rotation
 */
export function getCSRFToken(sessionId: string): string | null {
  const state = TOKEN_STORE.get(sessionId);

  if (!state) {
    return null;
  }

  const now = Date.now();
  if (now - state.createdAt > TOKEN_EXPIRY) {
    TOKEN_STORE.delete(sessionId);
    return null;
  }

  return state.token;
}

/**
 * CSRF protection wrapper for Next.js API routes
 * Usage:
 * export default csrfProtection(async (req, res) => {
 *   // Handle request
 * }, { rotateToken: true });
 */
export function csrfProtection(
  handler: (req: any, res: any) => Promise<void> | void,
  options: CSRFTokenOptions = {}
) {
  const {
    tokenLength = 32,
    cookieName = 'csrf-token',
    headerName = 'X-CSRF-Token',
    rotateToken = true,
    maxAge = 86400,
    secure = process.env.NODE_ENV === 'production',
    sameSite = 'Strict',
  } = options;

  return async (req: any, res: any) => {
    // Generate or get session ID from cookie
    let sessionId = req.cookies['x-session-id'];

    if (!sessionId) {
      sessionId = crypto.randomUUID();
      res.setHeader(
        'Set-Cookie',
        `x-session-id=${sessionId}; Path=/; Max-Age=${maxAge}; ${secure ? 'Secure; ' : ''}SameSite=${sameSite}`
      );
    }

    // For GET requests, generate or return existing token
    if (req.method === 'GET') {
      let token = getCSRFToken(sessionId);

      if (!token) {
        token = generateCSRFToken(sessionId);
      }

      res.setHeader(
        'Set-Cookie',
        `${cookieName}=${token}; Path=/; Max-Age=${maxAge}; ${secure ? 'Secure; ' : ''}SameSite=${sameSite}; HttpOnly`
      );

      // Attach token to request for handler use
      req.csrfToken = token;
      req.sessionId = sessionId;

      await handler(req, res);
      return;
    }

    // For POST/PUT/PATCH/DELETE, validate token
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(req.method)) {
      const tokenFromHeader = req.headers[headerName.toLowerCase()];
      const tokenFromBody = req.body?.[cookieName];
      const tokenToValidate = tokenFromHeader || tokenFromBody;

      if (!tokenToValidate) {
        res.status(403).json({ error: 'CSRF token missing' });
        return;
      }

      if (!validateCSRFToken(sessionId, tokenToValidate)) {
        res.status(403).json({ error: 'CSRF token invalid' });
        return;
      }

      // Rotate token if enabled
      if (rotateToken) {
        const newToken = rotateCSRFToken(sessionId);
        res.setHeader(
          'Set-Cookie',
          `${cookieName}=${newToken}; Path=/; Max-Age=${maxAge}; ${secure ? 'Secure; ' : ''}SameSite=${sameSite}; HttpOnly`
        );
        req.csrfToken = newToken;
      }

      req.sessionId = sessionId;
      await handler(req, res);
      return;
    }

    await handler(req, res);
  };
}
