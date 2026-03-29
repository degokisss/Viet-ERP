import type Redis from 'ioredis';
import type { IRateLimitStrategy, RateLimitResult } from './types';
import { REDIS_PREFIXES } from './constants';

/**
 * Base class for rate limit strategies
 */
abstract class BaseRateLimitStrategy implements IRateLimitStrategy {
  protected redis: Redis;
  protected points: number;
  protected duration: number;
  protected blockDuration: number;
  protected keyPrefix: string;

  constructor(
    redis: Redis,
    points: number,
    duration: number,
    blockDuration: number,
    keyPrefix: string = REDIS_PREFIXES.RATE_LIMIT
  ) {
    this.redis = redis;
    this.points = points;
    this.duration = duration;
    this.blockDuration = blockDuration;
    this.keyPrefix = keyPrefix;
  }

  protected getKey(key: string): string {
    return `${this.keyPrefix}:${key}`;
  }

  protected getBlockKey(key: string): string {
    return `${REDIS_PREFIXES.BLOCKED}:${key}`;
  }

  protected getExpireTime(): number {
    return Math.floor(Date.now() / 1000) + this.duration;
  }

  protected getBlockExpireTime(): number {
    return Math.floor(Date.now() / 1000) + this.blockDuration;
  }

  protected async checkBlocked(key: string): Promise<boolean> {
    const blockKey = this.getBlockKey(key);
    const blocked = await this.redis.exists(blockKey);
    return blocked > 0;
  }

  protected async createBlock(key: string): Promise<void> {
    const blockKey = this.getBlockKey(key);
    await this.redis.setex(blockKey, this.blockDuration, '1');
  }

  abstract consume(key: string, points: number): Promise<RateLimitResult>;
  abstract get(key: string): Promise<RateLimitResult>;
  abstract reset(key: string): Promise<void>;
}

/**
 * Fixed Window Strategy
 * Resets counter at fixed intervals (e.g., every minute)
 * Simpler but can have issues at window boundaries
 */
export class FixedWindowStrategy extends BaseRateLimitStrategy {
  async consume(key: string, points: number = 1): Promise<RateLimitResult> {
    const redisKey = this.getKey(key);
    const now = Math.floor(Date.now() / 1000);
    const windowKey = `${redisKey}:window:${Math.floor(now / this.duration)}`;

    // Check if blocked
    const isBlocked = await this.checkBlocked(key);
    if (isBlocked) {
      return {
        allowed: false,
        points: 0,
        pointsRemaining: 0,
        retryAfter: this.blockDuration,
        expireAt: now + this.blockDuration,
        isBlocked: true,
      };
    }

    // Consume points
    const currentPoints = await this.redis.incrby(windowKey, points);
    await this.redis.expire(windowKey, this.duration);

    const allowed = currentPoints <= this.points;
    const pointsRemaining = Math.max(0, this.points - currentPoints);

    // Block if exceeded
    if (!allowed) {
      await this.createBlock(key);
    }

    return {
      allowed,
      points: currentPoints,
      pointsRemaining,
      retryAfter: allowed ? 0 : this.blockDuration,
      expireAt: now + this.duration,
      isBlocked: !allowed,
    };
  }

  async get(key: string): Promise<RateLimitResult> {
    const redisKey = this.getKey(key);
    const now = Math.floor(Date.now() / 1000);
    const windowKey = `${redisKey}:window:${Math.floor(now / this.duration)}`;
    const isBlocked = await this.checkBlocked(key);

    const currentPoints = await this.redis.get(windowKey);
    const points = currentPoints ? parseInt(currentPoints, 10) : 0;
    const pointsRemaining = Math.max(0, this.points - points);

    return {
      allowed: !isBlocked && points <= this.points,
      points,
      pointsRemaining,
      retryAfter: isBlocked ? this.blockDuration : 0,
      expireAt: now + this.duration,
      isBlocked,
    };
  }

  async reset(key: string): Promise<void> {
    const redisKey = this.getKey(key);
    const now = Math.floor(Date.now() / 1000);
    const windowKey = `${redisKey}:window:${Math.floor(now / this.duration)}`;
    const blockKey = this.getBlockKey(key);

    await Promise.all([
      this.redis.del(windowKey),
      this.redis.del(blockKey),
    ]);
  }
}

/**
 * Sliding Window Strategy
 * More accurate rate limiting using a sliding window approach
 * Tracks timestamps of all requests within the window
 */
export class SlidingWindowStrategy extends BaseRateLimitStrategy {
  async consume(key: string, points: number = 1): Promise<RateLimitResult> {
    const redisKey = this.getKey(key);
    const now = Math.floor(Date.now() / 1000);
    const windowStart = now - this.duration;

    // Check if blocked
    const isBlocked = await this.checkBlocked(key);
    if (isBlocked) {
      return {
        allowed: false,
        points: 0,
        pointsRemaining: 0,
        retryAfter: this.blockDuration,
        expireAt: now + this.duration,
        isBlocked: true,
      };
    }

    // Remove old entries outside the window
    await this.redis.zremrangebyscore(redisKey, 0, windowStart);

    // Count current requests in window
    const currentCount = await this.redis.zcard(redisKey);

    // Check if we can consume
    const allowed = currentCount + points <= this.points;

    if (allowed) {
      // Add new request(s) to the sorted set
      for (let i = 0; i < points; i++) {
        await this.redis.zadd(redisKey, now + i, `${now + i}:${i}`);
      }
      // Set expiration
      await this.redis.expire(redisKey, this.duration);
    } else {
      // Block if exceeded
      await this.createBlock(key);
    }

    const pointsRemaining = Math.max(0, this.points - currentCount - points);

    return {
      allowed,
      points: currentCount + points,
      pointsRemaining,
      retryAfter: allowed ? 0 : this.blockDuration,
      expireAt: now + this.duration,
      isBlocked: !allowed,
    };
  }

  async get(key: string): Promise<RateLimitResult> {
    const redisKey = this.getKey(key);
    const now = Math.floor(Date.now() / 1000);
    const windowStart = now - this.duration;
    const isBlocked = await this.checkBlocked(key);

    // Remove old entries
    await this.redis.zremrangebyscore(redisKey, 0, windowStart);

    // Count current requests
    const currentCount = await this.redis.zcard(redisKey);
    const pointsRemaining = Math.max(0, this.points - currentCount);

    return {
      allowed: !isBlocked && currentCount <= this.points,
      points: currentCount,
      pointsRemaining,
      retryAfter: isBlocked ? this.blockDuration : 0,
      expireAt: now + this.duration,
      isBlocked,
    };
  }

  async reset(key: string): Promise<void> {
    const redisKey = this.getKey(key);
    const blockKey = this.getBlockKey(key);

    await Promise.all([
      this.redis.del(redisKey),
      this.redis.del(blockKey),
    ]);
  }
}

/**
 * Token Bucket Strategy
 * Tokens are added at a fixed rate, burst capacity is limited
 * Better for handling bursty traffic
 */
export class TokenBucketStrategy extends BaseRateLimitStrategy {
  private refillRate: number;

  constructor(
    redis: Redis,
    points: number,
    duration: number,
    blockDuration: number,
    keyPrefix: string = REDIS_PREFIXES.RATE_LIMIT
  ) {
    super(redis, points, duration, blockDuration, keyPrefix);
    // Calculate refill rate: points per second
    this.refillRate = points / duration;
  }

  async consume(key: string, points: number = 1): Promise<RateLimitResult> {
    const redisKey = this.getKey(key);
    const now = Date.now();

    // Check if blocked
    const isBlocked = await this.checkBlocked(key);
    if (isBlocked) {
      return {
        allowed: false,
        points: 0,
        pointsRemaining: 0,
        retryAfter: this.blockDuration,
        expireAt: Math.floor(now / 1000) + this.blockDuration,
        isBlocked: true,
      };
    }

    // Get bucket state
    const bucket = await this.redis.hgetall(redisKey);
    const lastRefill = bucket.lastRefill ? parseInt(bucket.lastRefill, 10) : now;
    let tokens = bucket.tokens ? parseFloat(bucket.tokens) : this.points;

    // Calculate tokens to add based on time elapsed
    const timeDiff = (now - lastRefill) / 1000; // Convert to seconds
    const tokensToAdd = timeDiff * this.refillRate;
    tokens = Math.min(this.points, tokens + tokensToAdd);

    const allowed = tokens >= points;

    if (allowed) {
      tokens -= points;
      await this.redis.hset(redisKey, 'tokens', tokens.toString(), 'lastRefill', now.toString());
      await this.redis.expire(redisKey, this.duration * 2);
    } else {
      await this.createBlock(key);
    }

    const pointsRemaining = Math.max(0, Math.floor(tokens));

    return {
      allowed,
      points: this.points - Math.floor(tokens),
      pointsRemaining,
      retryAfter: allowed ? 0 : this.blockDuration,
      expireAt: Math.floor(now / 1000) + this.duration,
      isBlocked: !allowed,
    };
  }

  async get(key: string): Promise<RateLimitResult> {
    const redisKey = this.getKey(key);
    const now = Date.now();
    const isBlocked = await this.checkBlocked(key);

    const bucket = await this.redis.hgetall(redisKey);
    const lastRefill = bucket.lastRefill ? parseInt(bucket.lastRefill, 10) : now;
    let tokens = bucket.tokens ? parseFloat(bucket.tokens) : this.points;

    const timeDiff = (now - lastRefill) / 1000;
    const tokensToAdd = timeDiff * this.refillRate;
    tokens = Math.min(this.points, tokens + tokensToAdd);

    const pointsRemaining = Math.max(0, Math.floor(tokens));

    return {
      allowed: !isBlocked && tokens >= 1,
      points: this.points - Math.floor(tokens),
      pointsRemaining,
      retryAfter: isBlocked ? this.blockDuration : 0,
      expireAt: Math.floor(now / 1000) + this.duration,
      isBlocked,
    };
  }

  async reset(key: string): Promise<void> {
    const redisKey = this.getKey(key);
    const blockKey = this.getBlockKey(key);

    await Promise.all([
      this.redis.del(redisKey),
      this.redis.del(blockKey),
    ]);
  }
}

/**
 * Factory function to create a rate limit strategy
 */
export function createStrategy(
  strategyType: string,
  redis: Redis,
  points: number,
  duration: number,
  blockDuration: number,
  keyPrefix: string
): IRateLimitStrategy {
  switch (strategyType.toLowerCase()) {
    case 'sliding-window':
    case 'sliding_window':
      return new SlidingWindowStrategy(redis, points, duration, blockDuration, keyPrefix);
    case 'token-bucket':
    case 'token_bucket':
      return new TokenBucketStrategy(redis, points, duration, blockDuration, keyPrefix);
    case 'fixed-window':
    case 'fixed_window':
    default:
      return new FixedWindowStrategy(redis, points, duration, blockDuration, keyPrefix);
  }
}
