import type { RateLimitConfig } from './types';

/**
 * Default tier configurations for different user types
 * Each tier specifies points (requests) allowed per minute
 */

/**
 * PUBLIC tier: 100 requests per minute
 * For unauthenticated or public API endpoints
 */
export const PUBLIC_TIER: Partial<RateLimitConfig> = {
  points: 100,
  duration: 60,
  blockDuration: 60,
  keyPrefix: 'rl:public',
};

/**
 * AUTHENTICATED tier: 500 requests per minute
 * For authenticated users with normal access
 */
export const AUTHENTICATED_TIER: Partial<RateLimitConfig> = {
  points: 500,
  duration: 60,
  blockDuration: 120,
  keyPrefix: 'rl:auth',
};

/**
 * ADMIN tier: 1000 requests per minute
 * For administrators and privileged users
 */
export const ADMIN_TIER: Partial<RateLimitConfig> = {
  points: 1000,
  duration: 60,
  blockDuration: 120,
  keyPrefix: 'rl:admin',
};

/**
 * WEBHOOK tier: 50 requests per minute
 * For webhook endpoints and external integrations
 */
export const WEBHOOK_TIER: Partial<RateLimitConfig> = {
  points: 50,
  duration: 60,
  blockDuration: 300,
  keyPrefix: 'rl:webhook',
};

/**
 * Default rate limit configuration
 */
export const DEFAULT_CONFIG: Partial<RateLimitConfig> = {
  points: 100,
  duration: 60,
  blockDuration: 60,
  keyPrefix: 'rl',
};

/**
 * Default Redis key prefixes
 */
export const REDIS_PREFIXES = {
  RATE_LIMIT: 'rl',
  BLOCKED: 'rl:blocked',
  WINDOW: 'rl:window',
  BUCKET: 'rl:bucket',
} as const;

/**
 * HTTP status codes and messages
 */
export const HTTP_STATUS = {
  TOO_MANY_REQUESTS: 429,
  RETRY_AFTER_HEADER: 'Retry-After',
  RATE_LIMIT_REMAINING_HEADER: 'X-RateLimit-Remaining',
  RATE_LIMIT_RESET_HEADER: 'X-RateLimit-Reset',
  RATE_LIMIT_LIMIT_HEADER: 'X-RateLimit-Limit',
} as const;

/**
 * Default error message for rate limit exceeded
 */
export const DEFAULT_RATE_LIMIT_MESSAGE = 'Quá nhiều yêu cầu. Vui lòng thử lại sau.';

/**
 * Default error message for rate limit exceeded (English)
 */
export const DEFAULT_RATE_LIMIT_MESSAGE_EN = 'Too many requests. Please try again later.';
