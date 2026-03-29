/**
 * Security Headers Configuration
 * Provides Next.js compatible security headers for XSS, clickjacking, content sniffing prevention
 */

export interface SecurityHeadersConfig {
  /** CSP directives - default, script-src, style-src, etc. */
  cspDirectives?: Record<string, string[]>;
  /** Frame ancestors for X-Frame-Options */
  frameAncestors?: string[];
  /** CSP report URI for violations */
  reportUri?: string;
  /** Enable HSTS (Strict-Transport-Security) */
  hsts?: boolean;
  /** HSTS max-age in seconds (default 31536000 = 1 year) */
  hstsMaxAge?: number;
  /** Include subdomains in HSTS */
  hstsIncludeSubdomains?: boolean;
}

/**
 * Default CSP directives - permissive for development
 */
const DEFAULT_CSP_DIRECTIVES = {
  'default-src': ["'self'"],
  'script-src': ["'self'", "'unsafe-inline'", "'unsafe-eval'"],
  'style-src': ["'self'", "'unsafe-inline'"],
  'img-src': ["'self'", 'data:', 'https:'],
  'font-src': ["'self'", 'data:', 'https:'],
  'connect-src': ["'self'", 'https:'],
  'frame-ancestors': ["'self'"],
};

/**
 * Strict CSP directives for production
 */
const STRICT_CSP_DIRECTIVES = {
  'default-src': ["'self'"],
  'script-src': ["'self'", "'strict-dynamic'"],
  'style-src': ["'self'"],
  'img-src': ["'self'", 'data:'],
  'font-src': ["'self'"],
  'connect-src': ["'self'"],
  'frame-ancestors': ["'none'"],
  'upgrade-insecure-requests': [],
};

/**
 * Generate Content-Security-Policy header value
 */
function generateCSP(directives: Record<string, string[]>): string {
  return Object.entries(directives)
    .map(([key, values]) => {
      if (values.length === 0) return key;
      return `${key} ${values.join(' ')}`;
    })
    .join('; ');
}

/**
 * Generate security headers array for Next.js headers configuration
 * Returns headers in Next.js format: { key: string, value: string }[]
 */
export function securityHeaders(config: SecurityHeadersConfig = {}): Array<{
  key: string;
  value: string;
}> {
  const {
    cspDirectives = DEFAULT_CSP_DIRECTIVES,
    frameAncestors = ["'self'"],
    reportUri,
    hsts = true,
    hstsMaxAge = 31536000,
    hstsIncludeSubdomains = true,
  } = config;

  // Merge frame-ancestors into CSP
  const finalCSPDirectives = {
    ...cspDirectives,
    'frame-ancestors': frameAncestors,
  };

  if (reportUri) {
    finalCSPDirectives['report-uri'] = [reportUri];
  }

  const cspValue = generateCSP(finalCSPDirectives);

  const headers: Array<{ key: string; value: string }> = [
    {
      key: 'X-Content-Type-Options',
      value: 'nosniff',
    },
    {
      key: 'X-Frame-Options',
      value: frameAncestors.includes("'none'") ? 'DENY' : 'SAMEORIGIN',
    },
    {
      key: 'X-XSS-Protection',
      value: '1; mode=block',
    },
    {
      key: 'Referrer-Policy',
      value: 'strict-origin-when-cross-origin',
    },
    {
      key: 'Permissions-Policy',
      value: 'geolocation=(), microphone=(), camera=(), payment=()',
    },
    {
      key: 'Content-Security-Policy',
      value: cspValue,
    },
  ];

  if (hsts) {
    const hstsValue = `max-age=${hstsMaxAge}${hstsIncludeSubdomains ? '; includeSubDomains' : ''}; preload`;
    headers.push({
      key: 'Strict-Transport-Security',
      value: hstsValue,
    });
  }

  return headers;
}

/**
 * Get strict CSP configuration for production
 */
export function strictSecurityHeaders(config: Partial<SecurityHeadersConfig> = {}): Array<{
  key: string;
  value: string;
}> {
  return securityHeaders({
    cspDirectives: STRICT_CSP_DIRECTIVES,
    frameAncestors: ["'none'"],
    hsts: true,
    hstsMaxAge: 31536000,
    hstsIncludeSubdomains: true,
    ...config,
  });
}

/**
 * Get development-friendly CSP configuration
 */
export function devSecurityHeaders(config: Partial<SecurityHeadersConfig> = {}): Array<{
  key: string;
  value: string;
}> {
  return securityHeaders({
    cspDirectives: DEFAULT_CSP_DIRECTIVES,
    frameAncestors: ["'self'"],
    hsts: false,
    ...config,
  });
}
