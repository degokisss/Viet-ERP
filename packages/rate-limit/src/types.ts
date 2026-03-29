import type Redis from 'ioredis';

/**
 * Enum for rate limiting strategies
 */
export enum RateLimitStrategy {
  FIXED_WINDOW = 'fixed-window',
  SLIDING_WINDOW = 'sliding-window',
  TOKEN_BUCKET = 'token-bucket',
}

/**
 * Configuration for rate limiter
 */
export interface RateLimitConfig {
  /** Redis connection instance */
  redis: Redis;

  /** Number of points/requests allowed */
  points: number;

  /** Duration in seconds for the rate limit window */
  duration: number;

  /** Duration in seconds to block when limit exceeded */
  blockDuration?: number;

  /** Rate limiting strategy to use */
  strategy?: RateLimitStrategy;

  /** Custom key generator function */
  keyGenerator?: KeyGenerator;

  /** Prefix for Redis keys */
  keyPrefix?: string;

  /** Enable sliding window persistence */
  enableSlidingWindow?: boolean;
}

/**
 * Result of rate limit consumption
 */
export interface RateLimitResult {
  /** Whether the request was allowed */
  allowed: boolean;

  /** Current point count */
  points: number;

  /** Number of points remaining in current window */
  pointsRemaining: number;

  /** Seconds until rate limit reset */
  retryAfter: number;

  /** Timestamp of when limit expires */
  expireAt: number;

  /** Whether the key is currently blocked */
  isBlocked: boolean;
}

/**
 * Interface for rate limiter methods
 */
export interface IRateLimiter {
  /**
   * Consume points for a key
   * @param key - The identifier for rate limiting
   * @param points - Number of points to consume (default: 1)
   * @returns Rate limit result
   */
  consume(key: string, points?: number): Promise<RateLimitResult>;

  /**
   * Get current state for a key
   * @param key - The identifier for rate limiting
   * @returns Rate limit result
   */
  get(key: string): Promise<RateLimitResult>;

  /**
   * Delete/reset a key
   * @param key - The identifier for rate limiting
   */
  delete(key: string): Promise<void>;

  /**
   * Reset all rate limit data for a key
   * @param key - The identifier for rate limiting
   */
  reset(key: string): Promise<void>;

  /**
   * Check if a key is blocked
   * @param key - The identifier for rate limiting
   * @returns Whether the key is currently blocked
   */
  isBlocked(key: string): Promise<boolean>;
}

/**
 * Type for custom key generator function
 * Used to generate unique rate limit keys based on request/context
 */
export type KeyGenerator = (context: {
  ip?: string;
  userId?: string;
  apiKey?: string;
  [key: string]: string | undefined;
}) => string;

/**
 * Middleware configuration options
 */
export interface MiddlewareConfig extends Omit<RateLimitConfig, 'keyGenerator'> {
  /** Custom key generator */
  keyGenerator?: KeyGenerator;

  /** Skip rate limiting for specific requests */
  skip?: (context: any) => boolean;

  /** Custom handler for rate limit exceeded */
  onLimitExceeded?: (result: RateLimitResult, context: any) => void;

  /** Response message when rate limited */
  message?: string;

  /** Response status code when rate limited */
  statusCode?: number;
}

/**
 * Interface for rate limit strategy implementation
 */
export interface IRateLimitStrategy {
  /**
   * Consume points from the rate limit
   * @param key - The identifier
   * @param points - Number of points to consume
   * @returns Rate limit result
   */
  consume(key: string, points: number): Promise<RateLimitResult>;

  /**
   * Get current state
   * @param key - The identifier
   * @returns Rate limit result
   */
  get(key: string): Promise<RateLimitResult>;

  /**
   * Reset the rate limit
   * @param key - The identifier
   */
  reset(key: string): Promise<void>;
}
