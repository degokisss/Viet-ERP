/**
 * Security Middleware
 * Next.js API route wrapper and Express middleware for applying security headers and CORS
 */

import { securityHeaders, SecurityHeadersConfig, devSecurityHeaders } from './headers';
import { CORSOptions, applyCORSHeaders, productionCORS } from './cors';

/**
 * Next.js API route handler type
 */
export type NextApiHandler = (req: any, res: any) => Promise<void> | void;

/**
 * Express middleware handler type
 */
export type ExpressHandler = (req: any, res: any, next: any) => void;

/**
 * Configuration for security middleware
 */
export interface SecurityMiddlewareConfig {
  headers?: SecurityHeadersConfig;
  cors?: CORSOptions;
  enableCors?: boolean;
  enableHeaders?: boolean;
  environment?: 'development' | 'production';
}

/**
 * Next.js API route wrapper that applies security headers and CORS
 * Usage:
 * export default withSecurityHeaders(async (req, res) => {
 *   res.status(200).json({ message: 'ok' });
 * });
 */
export function withSecurityHeaders(
  handler: NextApiHandler,
  config: SecurityMiddlewareConfig = {}
): NextApiHandler {
  const {
    headers: headerConfig = {},
    cors: corsConfig = {},
    enableCors = true,
    enableHeaders = true,
    environment = process.env.NODE_ENV || 'development',
  } = config;

  return async (req: any, res: any) => {
    // Apply security headers
    if (enableHeaders) {
      const secHeaders = environment === 'production'
        ? securityHeaders(headerConfig)
        : devSecurityHeaders(headerConfig);

      secHeaders.forEach(({ key, value }) => {
        res.setHeader(key, value);
      });
    }

    // Apply CORS headers
    if (enableCors) {
      const origin = req.headers.origin || req.headers.referer;
      const corsOptions = corsConfig.origin ? corsConfig : productionCORS();

      const corsHeaders = applyCORSHeaders(
        {},
        corsOptions,
        origin
      );

      Object.entries(corsHeaders).forEach(([key, value]) => {
        res.setHeader(key, value);
      });
    }

    // Handle preflight requests
    if (req.method === 'OPTIONS') {
      res.status(200).end();
      return;
    }

    // Call the actual handler
    await handler(req, res);
  };
}

/**
 * Express middleware for applying security headers and CORS
 * Usage:
 * app.use(securityMiddleware({ environment: 'production' }));
 */
export function securityMiddleware(
  config: SecurityMiddlewareConfig = {}
): ExpressHandler {
  const {
    headers: headerConfig = {},
    cors: corsConfig = {},
    enableCors = true,
    enableHeaders = true,
    environment = process.env.NODE_ENV || 'development',
  } = config;

  return (req: any, res: any, next: ExpressHandler) => {
    // Apply security headers
    if (enableHeaders) {
      const secHeaders = environment === 'production'
        ? securityHeaders(headerConfig)
        : devSecurityHeaders(headerConfig);

      secHeaders.forEach(({ key, value }) => {
        res.setHeader(key, value);
      });
    }

    // Apply CORS headers
    if (enableCors) {
      const origin = req.headers.origin;
      const corsOptions = corsConfig.origin ? corsConfig : productionCORS();

      const corsHeaders = applyCORSHeaders(
        {},
        corsOptions,
        origin
      );

      Object.entries(corsHeaders).forEach(([key, value]) => {
        res.setHeader(key, value);
      });
    }

    // Handle preflight requests
    if (req.method === 'OPTIONS') {
      res.status(200).end();
      return;
    }

    next();
  };
}

/**
 * Simplified Next.js middleware for Next.js 12+ using middleware.ts
 * Returns a response object with headers applied
 */
export function applySecurityHeaders(
  responseOrHeaders: any = {},
  config: SecurityMiddlewareConfig = {}
): Record<string, string> {
  const {
    headers: headerConfig = {},
    environment = process.env.NODE_ENV || 'development',
  } = config;

  const headers: Record<string, string> = {};

  const secHeaders = environment === 'production'
    ? securityHeaders(headerConfig)
    : devSecurityHeaders(headerConfig);

  secHeaders.forEach(({ key, value }) => {
    headers[key] = value;
  });

  return headers;
}
