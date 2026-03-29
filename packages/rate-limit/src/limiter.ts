import type Redis from 'ioredis';
import type { RateLimitConfig, IRateLimiter, RateLimitResult, IRateLimitStrategy } from './types';
import { RateLimitStrategy } from './types';
import { createStrategy } from './strategies';
import { DEFAULT_CONFIG, REDIS_PREFIXES } from './constants';

/**
 * Main RateLimiter class
 * Provides a unified interface for rate limiting with configurable strategies
 */
export class RateLimiter implements IRateLimiter {
  private config: Required<RateLimitConfig>;
  private strategy: IRateLimitStrategy;

  constructor(config: RateLimitConfig) {
    this.config = {
      ...DEFAULT_CONFIG,
      ...config,
      strategy: config.strategy || RateLimitStrategy.FIXED_WINDOW,
      keyPrefix: config.keyPrefix || REDIS_PREFIXES.RATE_LIMIT,
      blockDuration: config.blockDuration || config.duration,
      keyGenerator: config.keyGenerator,
    };

    this.strategy = createStrategy(
      this.config.strategy,
      this.config.redis,
      this.config.points,
      this.config.duration,
      this.config.blockDuration,
      this.config.keyPrefix
    );
  }

  /**
   * Consume points for a rate limit key
   * @param key - The rate limit key (e.g., IP address, user ID, or custom identifier)
   * @param points - Number of points to consume (default: 1)
   * @returns RateLimitResult with consumption result
   */
  async consume(key: string, points: number = 1): Promise<RateLimitResult> {
    if (points <= 0) {
      throw new Error('Points to consume must be greater than 0');
    }

    if (points > this.config.points) {
      throw new Error(`Points (${points}) cannot exceed configured limit (${this.config.points})`);
    }

    return this.strategy.consume(key, points);
  }

  /**
   * Get current rate limit state for a key without consuming points
   * @param key - The rate limit key
   * @returns RateLimitResult with current state
   */
  async get(key: string): Promise<RateLimitResult> {
    return this.strategy.get(key);
  }

  /**
   * Delete/reset rate limit data for a key
   * Removes all tracking data and unblocks if blocked
   * @param key - The rate limit key
   */
  async delete(key: string): Promise<void> {
    await this.strategy.reset(key);
  }

  /**
   * Reset rate limit for a key
   * Alias for delete() for backward compatibility
   * @param key - The rate limit key
   */
  async reset(key: string): Promise<void> {
    await this.delete(key);
  }

  /**
   * Check if a key is currently blocked
   * @param key - The rate limit key
   * @returns Boolean indicating if the key is blocked
   */
  async isBlocked(key: string): Promise<boolean> {
    const blockKey = `${REDIS_PREFIXES.BLOCKED}:${key}`;
    const exists = await this.config.redis.exists(blockKey);
    return exists > 0;
  }

  /**
   * Get configuration
   * @returns Current configuration
   */
  getConfig(): RateLimitConfig {
    return this.config;
  }

  /**
   * Update configuration
   * Note: Changes strategy if different
   * @param config - Partial configuration to update
   */
  updateConfig(config: Partial<RateLimitConfig>): void {
    this.config = {
      ...this.config,
      ...config,
      keyPrefix: config.keyPrefix || this.config.keyPrefix,
    };

    // Recreate strategy if configuration changed
    this.strategy = createStrategy(
      this.config.strategy,
      this.config.redis,
      this.config.points,
      this.config.duration,
      this.config.blockDuration,
      this.config.keyPrefix
    );
  }
}

/**
 * Factory function to create a RateLimiter instance
 * @param config - Rate limiter configuration
 * @returns RateLimiter instance
 */
export function createRateLimiter(config: RateLimitConfig): RateLimiter {
  return new RateLimiter(config);
}

/**
 * Create a rate limiter with a specific strategy
 * @param redis - Redis instance
 * @param points - Number of allowed points/requests
 * @param duration - Duration in seconds
 * @param strategy - Rate limiting strategy
 * @param blockDuration - Block duration in seconds when limit exceeded
 * @returns RateLimiter instance
 */
export function createRateLimiterWithStrategy(
  redis: Redis,
  points: number,
  duration: number,
  strategy: RateLimitStrategy,
  blockDuration?: number
): RateLimiter {
  return new RateLimiter({
    redis,
    points,
    duration,
    strategy,
    blockDuration,
  });
}
