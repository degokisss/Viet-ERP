/**
 * CORS Configuration Factory
 * Provides preset CORS configurations for different environments
 */

export interface CORSOptions {
  /** Allowed origins - wildcards or specific URLs */
  origin?: string | string[] | RegExp | ((origin: string) => boolean);
  /** Allowed HTTP methods */
  methods?: string[];
  /** Allowed request headers */
  allowedHeaders?: string[];
  /** Credentials (cookies, auth headers) */
  credentials?: boolean;
  /** Max age for preflight cache (seconds) */
  maxAge?: number;
  /** Expose headers to client */
  exposedHeaders?: string[];
}

/**
 * Parse allowed origins from environment variable
 * Format: "http://localhost:3000,https://example.com"
 */
function parseAllowedOrigins(envValue?: string): string[] {
  if (!envValue) return ['http://localhost:3000'];

  return envValue
    .split(',')
    .map(origin => origin.trim())
    .filter(origin => origin.length > 0);
}

/**
 * Create CORS configuration object
 */
export function corsConfig(options: CORSOptions = {}): CORSOptions {
  const {
    origin = parseAllowedOrigins(process.env.ALLOWED_ORIGINS),
    methods = ['GET', 'HEAD', 'PUT', 'PATCH', 'POST', 'DELETE'],
    allowedHeaders = ['Content-Type', 'Authorization', 'X-Requested-With'],
    credentials = true,
    maxAge = 86400, // 24 hours
    exposedHeaders = ['X-Total-Count', 'X-Page', 'X-Page-Size'],
  } = options;

  return {
    origin,
    methods,
    allowedHeaders,
    credentials,
    maxAge,
    exposedHeaders,
  };
}

/**
 * Development CORS preset - permissive, allows all origins
 */
export function developmentCORS(): CORSOptions {
  return corsConfig({
    origin: (origin) => {
      // Allow all origins in development
      return true;
    },
    methods: ['GET', 'HEAD', 'PUT', 'PATCH', 'POST', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['*'],
    credentials: true,
    maxAge: 3600,
  });
}

/**
 * Production CORS preset - strict, requires explicit origins
 */
export function productionCORS(allowedOrigins: string[] = []): CORSOptions {
  const origins = allowedOrigins.length > 0
    ? allowedOrigins
    : parseAllowedOrigins(process.env.ALLOWED_ORIGINS);

  return corsConfig({
    origin: (origin) => {
      if (!origin) return true; // Allow same-origin requests
      return origins.includes(origin);
    },
    methods: ['GET', 'HEAD', 'PUT', 'PATCH', 'POST', 'DELETE'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Requested-With'],
    credentials: true,
    maxAge: 86400,
  });
}

/**
 * Internal service-to-service CORS preset
 * Only allows requests from internal service URLs
 */
export function internalServiceCORS(serviceOrigins: string[] = []): CORSOptions {
  const origins = serviceOrigins.length > 0
    ? serviceOrigins
    : [
        'http://localhost:3000',
        'http://localhost:3001',
        'http://localhost:3002',
      ];

  return corsConfig({
    origin: (origin) => {
      if (!origin) return true;
      return origins.some(allowed =>
        origin.startsWith(allowed) || origin === allowed
      );
    },
    methods: ['GET', 'HEAD', 'PUT', 'PATCH', 'POST', 'DELETE'],
    allowedHeaders: ['Content-Type', 'Authorization', 'X-Service-Token'],
    credentials: true,
    maxAge: 3600,
  });
}

/**
 * Apply CORS headers to response headers object (for Next.js API routes)
 */
export function applyCORSHeaders(
  headers: Record<string, string>,
  options: CORSOptions,
  requestOrigin?: string
): Record<string, string> {
  const result = { ...headers };

  if (!requestOrigin) {
    requestOrigin = 'http://localhost:3000';
  }

  // Determine if origin is allowed
  let originAllowed = false;
  if (typeof options.origin === 'function') {
    originAllowed = options.origin(requestOrigin);
  } else if (typeof options.origin === 'string') {
    originAllowed = options.origin === '*' || options.origin === requestOrigin;
  } else if (Array.isArray(options.origin)) {
    originAllowed = options.origin.includes(requestOrigin);
  } else if (options.origin instanceof RegExp) {
    originAllowed = options.origin.test(requestOrigin);
  }

  if (originAllowed) {
    result['Access-Control-Allow-Origin'] = requestOrigin;
  }

  if (options.credentials) {
    result['Access-Control-Allow-Credentials'] = 'true';
  }

  if (options.methods) {
    result['Access-Control-Allow-Methods'] = options.methods.join(', ');
  }

  if (options.allowedHeaders) {
    result['Access-Control-Allow-Headers'] = options.allowedHeaders.join(', ');
  }

  if (options.maxAge) {
    result['Access-Control-Max-Age'] = options.maxAge.toString();
  }

  if (options.exposedHeaders) {
    result['Access-Control-Expose-Headers'] = options.exposedHeaders.join(', ');
  }

  return result;
}
