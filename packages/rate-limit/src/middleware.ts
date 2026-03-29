import type { NextApiRequest, NextApiResponse } from 'next';
import type { Request, Response, NextFunction } from 'express';
import type { MiddlewareConfig, RateLimitResult } from './types';
import { RateLimiter, createRateLimiter } from './limiter';
import {
  HTTP_STATUS,
  DEFAULT_RATE_LIMIT_MESSAGE,
  DEFAULT_RATE_LIMIT_MESSAGE_EN,
  REDIS_PREFIXES,
} from './constants';

/**
 * Default key generator: extracts client IP address
 */
export function defaultKeyGenerator(context: any): string {
  const ip =
    context.ip ||
    context['x-forwarded-for']?.split(',')[0].trim() ||
    context['x-real-ip'] ||
    context.socket?.remoteAddress ||
    'unknown';
  return ip;
}

/**
 * Extract client IP from request headers
 */
function getClientIp(req: any): string {
  if (req.ip) return req.ip;
  if (req.headers['x-forwarded-for']) {
    return (req.headers['x-forwarded-for'] as string).split(',')[0].trim();
  }
  if (req.headers['x-real-ip']) {
    return req.headers['x-real-ip'] as string;
  }
  return req.socket?.remoteAddress || 'unknown';
}

/**
 * Next.js API Route Wrapper for rate limiting
 * Usage:
 * ```typescript
 * export default withRateLimit(handler, { redis, points: 100, duration: 60 });
 * ```
 */
export function withRateLimit(
  handler: (req: NextApiRequest, res: NextApiResponse) => Promise<void> | void,
  config: MiddlewareConfig
): (req: NextApiRequest, res: NextApiResponse) => Promise<void> {
  const limiter = createRateLimiter(config);
  const keyGenerator = config.keyGenerator || defaultKeyGenerator;
  const skip = config.skip || (() => false);
  const onLimitExceeded = config.onLimitExceeded || (() => {});
  const statusCode = config.statusCode || HTTP_STATUS.TOO_MANY_REQUESTS;
  const message = config.message || DEFAULT_RATE_LIMIT_MESSAGE;

  return async (req: NextApiRequest, res: NextApiResponse) => {
    // Check if should skip
    if (skip({ req, res })) {
      return handler(req, res);
    }

    // Generate key
    const ip = getClientIp(req);
    const key = keyGenerator({ ip, userId: (req as any).userId, apiKey: (req as any).apiKey });

    // Consume rate limit
    const result = await limiter.consume(key);

    // Set rate limit headers
    res.setHeader(HTTP_STATUS.RATE_LIMIT_LIMIT_HEADER, config.points);
    res.setHeader(HTTP_STATUS.RATE_LIMIT_REMAINING_HEADER, result.pointsRemaining);
    res.setHeader(HTTP_STATUS.RATE_LIMIT_RESET_HEADER, result.expireAt);

    if (!result.allowed) {
      onLimitExceeded(result, { req, res });
      res.setHeader(HTTP_STATUS.RETRY_AFTER_HEADER, result.retryAfter);
      res.status(statusCode).json({
        error: message,
        retryAfter: result.retryAfter,
        resetAt: new Date(result.expireAt * 1000).toISOString(),
      });
      return;
    }

    return handler(req, res);
  };
}

/**
 * Express/Generic middleware for rate limiting
 * Usage:
 * ```typescript
 * app.use(rateLimitMiddleware({ redis, points: 100, duration: 60 }));
 * ```
 */
export function rateLimitMiddleware(config: MiddlewareConfig) {
  const limiter = createRateLimiter(config);
  const keyGenerator = config.keyGenerator || defaultKeyGenerator;
  const skip = config.skip || (() => false);
  const onLimitExceeded = config.onLimitExceeded || (() => {});
  const statusCode = config.statusCode || HTTP_STATUS.TOO_MANY_REQUESTS;
  const message = config.message || DEFAULT_RATE_LIMIT_MESSAGE;

  return async (
    req: Request | any,
    res: Response | any,
    next: NextFunction
  ): Promise<void> => {
    // Check if should skip
    if (skip({ req, res })) {
      return next();
    }

    try {
      // Generate key
      const ip = getClientIp(req);
      const key = keyGenerator({
        ip,
        userId: req.user?.id || req.userId,
        apiKey: req.apiKey,
      });

      // Consume rate limit
      const result = await limiter.consume(key);

      // Set rate limit headers
      res.setHeader(HTTP_STATUS.RATE_LIMIT_LIMIT_HEADER, config.points);
      res.setHeader(HTTP_STATUS.RATE_LIMIT_REMAINING_HEADER, result.pointsRemaining);
      res.setHeader(HTTP_STATUS.RATE_LIMIT_RESET_HEADER, result.expireAt);

      if (!result.allowed) {
        onLimitExceeded(result, { req, res });
        res.setHeader(HTTP_STATUS.RETRY_AFTER_HEADER, result.retryAfter);
        return res.status(statusCode).json({
          error: message,
          retryAfter: result.retryAfter,
          resetAt: new Date(result.expireAt * 1000).toISOString(),
        });
      }

      next();
    } catch (error) {
      // If rate limiter fails, allow request to proceed
      console.error('Rate limiter error:', error);
      next();
    }
  };
}

/**
 * Advanced middleware factory with more control
 */
export interface AdvancedRateLimitConfig extends MiddlewareConfig {
  /** Whether to trust proxy headers */
  trustProxy?: boolean;
  /** Custom error handler */
  errorHandler?: (error: Error, context: any) => void;
  /** Whether to include rate limit info in response body */
  includeRateLimitInfo?: boolean;
}

/**
 * Create advanced rate limiting middleware
 */
export function createRateLimitMiddleware(config: AdvancedRateLimitConfig) {
  const limiter = createRateLimiter(config);
  const keyGenerator = config.keyGenerator || defaultKeyGenerator;
  const skip = config.skip || (() => false);
  const onLimitExceeded = config.onLimitExceeded || (() => {});
  const errorHandler = config.errorHandler || ((error) => console.error(error));
  const statusCode = config.statusCode || HTTP_STATUS.TOO_MANY_REQUESTS;
  const message = config.message || DEFAULT_RATE_LIMIT_MESSAGE;
  const includeRateLimitInfo = config.includeRateLimitInfo !== false;

  return async (
    req: Request | any,
    res: Response | any,
    next: NextFunction
  ): Promise<void> => {
    // Check if should skip
    if (skip({ req, res })) {
      return next();
    }

    try {
      // Generate key
      const ip = getClientIp(req);
      const key = keyGenerator({
        ip,
        userId: req.user?.id || req.userId,
        apiKey: req.apiKey,
      });

      // Consume rate limit
      const result = await limiter.consume(key);

      // Attach rate limit info to request
      (req as any).rateLimit = result;

      // Set rate limit headers
      res.setHeader(HTTP_STATUS.RATE_LIMIT_LIMIT_HEADER, config.points);
      res.setHeader(HTTP_STATUS.RATE_LIMIT_REMAINING_HEADER, result.pointsRemaining);
      res.setHeader(HTTP_STATUS.RATE_LIMIT_RESET_HEADER, result.expireAt);

      if (!result.allowed) {
        onLimitExceeded(result, { req, res });
        res.setHeader(HTTP_STATUS.RETRY_AFTER_HEADER, result.retryAfter);

        const response: any = {
          error: message,
          retryAfter: result.retryAfter,
          resetAt: new Date(result.expireAt * 1000).toISOString(),
        };

        if (includeRateLimitInfo) {
          response.rateLimitInfo = {
            limit: config.points,
            remaining: result.pointsRemaining,
            reset: result.expireAt,
          };
        }

        return res.status(statusCode).json(response);
      }

      next();
    } catch (error) {
      errorHandler(error as Error, { req, res });
      // Allow request to proceed if rate limiter fails
      next();
    }
  };
}

/**
 * Create a custom rate limiter for specific paths or conditions
 */
export function createPathBasedRateLimiter(
  config: MiddlewareConfig,
  pathPatterns: string[]
): (req: Request | any, res: Response | any, next: NextFunction) => Promise<void> {
  const middleware = createRateLimitMiddleware(config);
  const patterns = pathPatterns.map((p) => new RegExp(p));

  return async (req: Request | any, res: Response | any, next: NextFunction) => {
    // Check if path matches patterns
    const matches = patterns.some((pattern) => pattern.test(req.path || req.url || ''));

    if (!matches) {
      return next();
    }

    return middleware(req, res, next);
  };
}
