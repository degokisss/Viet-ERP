// ============================================================
// Security Package — RBAC, Rate Limiting, Input Sanitization,
// CSRF Protection, Audit Trail, IP Filtering
// ============================================================

import * as crypto from 'crypto';
import { z } from 'zod';

// ════════════════════════════════════════════════════════════
// 1. RBAC — Role-Based Access Control
// ════════════════════════════════════════════════════════════

export type Permission =
  // Master data
  | 'master_data:read' | 'master_data:write' | 'master_data:delete'
  // HRM
  | 'hrm:read' | 'hrm:write' | 'hrm:delete' | 'hrm:payroll' | 'hrm:leave_approve'
  // CRM
  | 'crm:read' | 'crm:write' | 'crm:delete' | 'crm:pipeline'
  // Accounting
  | 'accounting:read' | 'accounting:write' | 'accounting:delete'
  | 'accounting:journal_approve' | 'accounting:period_close' | 'accounting:reports'
  | 'accounting:einvoice' | 'accounting:tax'
  // MRP
  | 'mrp:read' | 'mrp:write' | 'mrp:delete' | 'mrp:production' | 'mrp:inventory'
  // PM
  | 'pm:read' | 'pm:write' | 'pm:delete' | 'pm:assign'
  // E-commerce
  | 'ecommerce:read' | 'ecommerce:write' | 'ecommerce:delete'
  | 'ecommerce:orders' | 'ecommerce:refund' | 'ecommerce:promotions'
  // Admin
  | 'admin:read' | 'admin:write' | 'admin:users' | 'admin:roles'
  | 'admin:billing' | 'admin:settings' | 'admin:audit'
  // AI
  | 'ai:copilot' | 'ai:admin'
  // SDK
  | 'sdk:api_keys' | 'sdk:webhooks' | 'sdk:plugins';

export type Role =
  | 'super_admin'
  | 'admin'
  | 'accountant'
  | 'hr_manager'
  | 'sales_manager'
  | 'warehouse_manager'
  | 'project_manager'
  | 'ecommerce_manager'
  | 'employee'
  | 'viewer'
  | 'api_client';

/**
 * Role → Permissions mapping
 */
const ROLE_PERMISSIONS: Record<Role, Permission[]> = {
  super_admin: [
    // All permissions
    'master_data:read', 'master_data:write', 'master_data:delete',
    'hrm:read', 'hrm:write', 'hrm:delete', 'hrm:payroll', 'hrm:leave_approve',
    'crm:read', 'crm:write', 'crm:delete', 'crm:pipeline',
    'accounting:read', 'accounting:write', 'accounting:delete',
    'accounting:journal_approve', 'accounting:period_close', 'accounting:reports',
    'accounting:einvoice', 'accounting:tax',
    'mrp:read', 'mrp:write', 'mrp:delete', 'mrp:production', 'mrp:inventory',
    'pm:read', 'pm:write', 'pm:delete', 'pm:assign',
    'ecommerce:read', 'ecommerce:write', 'ecommerce:delete',
    'ecommerce:orders', 'ecommerce:refund', 'ecommerce:promotions',
    'admin:read', 'admin:write', 'admin:users', 'admin:roles',
    'admin:billing', 'admin:settings', 'admin:audit',
    'ai:copilot', 'ai:admin',
    'sdk:api_keys', 'sdk:webhooks', 'sdk:plugins',
  ],

  admin: [
    'master_data:read', 'master_data:write',
    'hrm:read', 'hrm:write', 'hrm:leave_approve',
    'crm:read', 'crm:write', 'crm:pipeline',
    'accounting:read', 'accounting:reports',
    'mrp:read', 'mrp:write',
    'pm:read', 'pm:write', 'pm:assign',
    'ecommerce:read', 'ecommerce:write', 'ecommerce:orders',
    'admin:read', 'admin:write', 'admin:users', 'admin:roles', 'admin:settings',
    'ai:copilot',
    'sdk:api_keys', 'sdk:webhooks',
  ],

  accountant: [
    'master_data:read',
    'accounting:read', 'accounting:write', 'accounting:journal_approve',
    'accounting:period_close', 'accounting:reports', 'accounting:einvoice', 'accounting:tax',
    'ecommerce:read', 'ecommerce:orders',
    'ai:copilot',
  ],

  hr_manager: [
    'master_data:read', 'master_data:write',
    'hrm:read', 'hrm:write', 'hrm:payroll', 'hrm:leave_approve',
    'ai:copilot',
  ],

  sales_manager: [
    'master_data:read',
    'crm:read', 'crm:write', 'crm:pipeline',
    'ecommerce:read', 'ecommerce:write', 'ecommerce:orders', 'ecommerce:promotions',
    'ai:copilot',
  ],

  warehouse_manager: [
    'master_data:read', 'master_data:write',
    'mrp:read', 'mrp:write', 'mrp:inventory',
    'ecommerce:read', 'ecommerce:orders',
  ],

  project_manager: [
    'master_data:read',
    'pm:read', 'pm:write', 'pm:assign',
    'ai:copilot',
  ],

  ecommerce_manager: [
    'master_data:read',
    'ecommerce:read', 'ecommerce:write', 'ecommerce:delete',
    'ecommerce:orders', 'ecommerce:refund', 'ecommerce:promotions',
    'crm:read', 'crm:write',
    'ai:copilot',
  ],

  employee: [
    'master_data:read',
    'hrm:read',
    'pm:read',
    'crm:read',
  ],

  viewer: [
    'master_data:read',
    'hrm:read',
    'crm:read',
    'accounting:read',
    'mrp:read',
    'pm:read',
    'ecommerce:read',
  ],

  api_client: [
    'master_data:read', 'master_data:write',
    'crm:read', 'crm:write',
    'ecommerce:read', 'ecommerce:write', 'ecommerce:orders',
    'sdk:webhooks',
  ],
};

/**
 * Check if user has required permission
 */
export function hasPermission(
  userRoles: Role[],
  requiredPermission: Permission,
  customPermissions?: Permission[]
): boolean {
  // Custom permissions override
  if (customPermissions?.includes(requiredPermission)) return true;

  // Check role-based permissions
  for (const role of userRoles) {
    if (ROLE_PERMISSIONS[role]?.includes(requiredPermission)) return true;
  }
  return false;
}

/**
 * Check if user has ALL required permissions
 */
export function hasAllPermissions(
  userRoles: Role[],
  requiredPermissions: Permission[],
  customPermissions?: Permission[]
): boolean {
  return requiredPermissions.every(p => hasPermission(userRoles, p, customPermissions));
}

/**
 * Check if user has ANY of the required permissions
 */
export function hasAnyPermission(
  userRoles: Role[],
  requiredPermissions: Permission[],
  customPermissions?: Permission[]
): boolean {
  return requiredPermissions.some(p => hasPermission(userRoles, p, customPermissions));
}

/**
 * Get all permissions for a set of roles
 */
export function getPermissionsForRoles(roles: Role[]): Permission[] {
  const permissions = new Set<Permission>();
  for (const role of roles) {
    for (const perm of ROLE_PERMISSIONS[role] || []) {
      permissions.add(perm);
    }
  }
  return Array.from(permissions);
}

/**
 * Middleware-style permission check
 */
export function requirePermission(permission: Permission) {
  return (user: { roles: Role[]; permissions?: Permission[] }): void => {
    if (!hasPermission(user.roles, permission, user.permissions)) {
      throw new SecurityError(
        'FORBIDDEN',
        `Bạn không có quyền thực hiện thao tác này (${permission})`,
        403
      );
    }
  };
}

// ════════════════════════════════════════════════════════════
// 2. Rate Limiting
// ════════════════════════════════════════════════════════════

export interface RateLimitConfig {
  windowMs: number;      // Time window in milliseconds
  maxRequests: number;   // Max requests per window
  keyPrefix?: string;    // Redis key prefix
  skipSuccessfulRequests?: boolean;
  message?: string;
}

export interface RateLimitResult {
  allowed: boolean;
  remaining: number;
  resetAt: Date;
  retryAfter?: number; // seconds
}

/**
 * In-memory sliding window rate limiter
 * For production: use Redis-backed implementation
 */
export class RateLimiter {
  private windows = new Map<string, { count: number; resetAt: number }>();

  constructor(private config: RateLimitConfig) {}

  /**
   * Check and consume a rate limit token
   */
  check(key: string): RateLimitResult {
    const fullKey = `${this.config.keyPrefix || 'rl'}:${key}`;
    const now = Date.now();
    const window = this.windows.get(fullKey);

    // Clean expired window
    if (window && window.resetAt <= now) {
      this.windows.delete(fullKey);
    }

    const current = this.windows.get(fullKey);

    if (!current) {
      // New window
      this.windows.set(fullKey, {
        count: 1,
        resetAt: now + this.config.windowMs,
      });
      return {
        allowed: true,
        remaining: this.config.maxRequests - 1,
        resetAt: new Date(now + this.config.windowMs),
      };
    }

    if (current.count >= this.config.maxRequests) {
      const retryAfter = Math.ceil((current.resetAt - now) / 1000);
      return {
        allowed: false,
        remaining: 0,
        resetAt: new Date(current.resetAt),
        retryAfter,
      };
    }

    current.count++;
    return {
      allowed: true,
      remaining: this.config.maxRequests - current.count,
      resetAt: new Date(current.resetAt),
    };
  }

  /**
   * Reset rate limit for a key
   */
  reset(key: string): void {
    const fullKey = `${this.config.keyPrefix || 'rl'}:${key}`;
    this.windows.delete(fullKey);
  }
}

/**
 * Pre-configured rate limiters for ERP endpoints
 */
export const RATE_LIMITS = {
  // Authentication: strict limits
  auth: new RateLimiter({
    windowMs: 15 * 60 * 1000, // 15 minutes
    maxRequests: 10,
    keyPrefix: 'rl:auth',
    message: 'Quá nhiều lần đăng nhập. Vui lòng thử lại sau 15 phút.',
  }),

  // API: per-tenant limits
  api: new RateLimiter({
    windowMs: 60 * 1000, // 1 minute
    maxRequests: 100,
    keyPrefix: 'rl:api',
    message: 'Đã vượt giới hạn API. Vui lòng thử lại sau.',
  }),

  // Webhook: per-endpoint limits
  webhook: new RateLimiter({
    windowMs: 60 * 1000,
    maxRequests: 30,
    keyPrefix: 'rl:webhook',
  }),

  // AI Copilot: per-user limits
  ai: new RateLimiter({
    windowMs: 60 * 1000,
    maxRequests: 10,
    keyPrefix: 'rl:ai',
    message: 'Đã đạt giới hạn AI Copilot. Vui lòng chờ 1 phút.',
  }),

  // E-commerce: cart/checkout
  ecommerce: new RateLimiter({
    windowMs: 60 * 1000,
    maxRequests: 60,
    keyPrefix: 'rl:ecom',
  }),
};

// ════════════════════════════════════════════════════════════
// 3. Input Sanitization & Validation
// ════════════════════════════════════════════════════════════

/**
 * Sanitize HTML input to prevent XSS
 */
export function sanitizeHTML(input: string): string {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '/': '&#x2F;',
    '`': '&#96;',
  };
  return input.replace(/[&<>"'`/]/g, char => map[char] || char);
}

/**
 * Sanitize SQL-like input (defense-in-depth, use parameterized queries)
 */
export function sanitizeSQL(input: string): string {
  return input
    .replace(/['";\\]/g, '')
    .replace(/--/g, '')
    .replace(/\/\*/g, '')
    .replace(/\*\//g, '')
    .replace(/\b(DROP|DELETE|INSERT|UPDATE|ALTER|EXEC|UNION|SELECT)\b/gi, '');
}

/**
 * Validate and sanitize Vietnamese phone number
 */
export function sanitizePhoneVN(phone: string): string | null {
  const cleaned = phone.replace(/[\s\-\(\)\.]/g, '');

  // Vietnamese phone: starts with 0 or +84
  const match = cleaned.match(/^(?:\+?84|0)(3|5|7|8|9)(\d{8})$/);
  if (!match) return null;

  return `0${match[1]}${match[2]}`;
}

/**
 * Validate Vietnamese tax code (MST)
 */
export function validateTaxCode(taxCode: string): boolean {
  const cleaned = taxCode.replace(/[\s\-]/g, '');
  // 10 digits, or 10-3 format (13 digits with dash)
  return /^\d{10}(-\d{3})?$/.test(cleaned);
}

/**
 * Common validation schemas using Zod
 */
export const ValidationSchemas = {
  email: z.string().email('Email không hợp lệ').max(255),

  phone: z.string().regex(
    /^(?:\+?84|0)(3|5|7|8|9)\d{8}$/,
    'Số điện thoại không hợp lệ'
  ),

  taxCode: z.string().regex(
    /^\d{10}(-\d{3})?$/,
    'Mã số thuế không hợp lệ'
  ),

  password: z.string()
    .min(8, 'Mật khẩu tối thiểu 8 ký tự')
    .regex(/[A-Z]/, 'Cần ít nhất 1 chữ hoa')
    .regex(/[a-z]/, 'Cần ít nhất 1 chữ thường')
    .regex(/[0-9]/, 'Cần ít nhất 1 chữ số')
    .regex(/[^A-Za-z0-9]/, 'Cần ít nhất 1 ký tự đặc biệt'),

  slug: z.string()
    .min(1)
    .max(200)
    .regex(/^[a-z0-9]+(?:-[a-z0-9]+)*$/, 'Slug không hợp lệ'),

  money: z.string().regex(
    /^\d+(\.\d{1,4})?$/,
    'Số tiền không hợp lệ'
  ),

  pagination: z.object({
    page: z.coerce.number().int().min(1).default(1),
    limit: z.coerce.number().int().min(1).max(100).default(20),
    sortBy: z.string().optional(),
    sortOrder: z.enum(['asc', 'desc']).default('desc'),
  }),

  dateRange: z.object({
    from: z.coerce.date(),
    to: z.coerce.date(),
  }).refine(data => data.to >= data.from, {
    message: 'Ngày kết thúc phải sau ngày bắt đầu',
  }),
};

// ════════════════════════════════════════════════════════════
// 4. CSRF Protection
// ════════════════════════════════════════════════════════════

/**
 * Generate CSRF token
 */
export function generateCSRFToken(): string {
  return crypto.randomBytes(32).toString('hex');
}

/**
 * Verify CSRF token using timing-safe comparison
 */
export function verifyCSRFToken(token: string, expected: string): boolean {
  if (token.length !== expected.length) return false;
  return crypto.timingSafeEqual(
    Buffer.from(token, 'utf8'),
    Buffer.from(expected, 'utf8')
  );
}

// ════════════════════════════════════════════════════════════
// 5. API Key Management
// ════════════════════════════════════════════════════════════

export interface APIKey {
  id: string;
  tenantId: string;
  name: string;
  keyHash: string;
  prefix: string;        // First 8 chars for identification
  permissions: Permission[];
  rateLimit: number;     // Requests per minute
  expiresAt?: Date;
  lastUsedAt?: Date;
  isActive: boolean;
  createdAt: Date;
}

/**
 * Generate a new API key
 * Format: erp_live_xxxxxxxxxxxxxxxxxxxxxxxxxxxx
 */
export function generateAPIKey(type: 'live' | 'test' = 'live'): {
  key: string;
  hash: string;
  prefix: string;
} {
  const randomPart = crypto.randomBytes(24).toString('base64url');
  const key = `erp_${type}_${randomPart}`;
  const hash = crypto.createHash('sha256').update(key).digest('hex');
  const prefix = key.substring(0, 12);

  return { key, hash, prefix };
}

/**
 * Hash an API key for storage
 */
export function hashAPIKey(key: string): string {
  return crypto.createHash('sha256').update(key).digest('hex');
}

/**
 * Verify an API key against stored hash
 */
export function verifyAPIKey(key: string, storedHash: string): boolean {
  const hash = hashAPIKey(key);
  return crypto.timingSafeEqual(
    Buffer.from(hash, 'hex'),
    Buffer.from(storedHash, 'hex')
  );
}

// ════════════════════════════════════════════════════════════
// 6. IP Filtering
// ════════════════════════════════════════════════════════════

export interface IPFilterConfig {
  mode: 'whitelist' | 'blacklist';
  ips: string[];     // Individual IPs
  cidrs: string[];   // CIDR ranges
}

/**
 * Check if IP is in CIDR range
 */
export function isIPInCIDR(ip: string, cidr: string): boolean {
  const [network, bits] = cidr.split('/');
  const mask = parseInt(bits, 10);

  const ipNum = ipToNumber(ip);
  const netNum = ipToNumber(network);
  const maskNum = ~(Math.pow(2, 32 - mask) - 1) >>> 0;

  return (ipNum & maskNum) === (netNum & maskNum);
}

/**
 * Check IP against filter config
 */
export function checkIPFilter(ip: string, config: IPFilterConfig): boolean {
  const isInList = config.ips.includes(ip) ||
    config.cidrs.some(cidr => isIPInCIDR(ip, cidr));

  return config.mode === 'whitelist' ? isInList : !isInList;
}

function ipToNumber(ip: string): number {
  return ip.split('.').reduce((acc, octet) => (acc << 8) + parseInt(octet, 10), 0) >>> 0;
}

// ════════════════════════════════════════════════════════════
// 7. Security Headers
// ════════════════════════════════════════════════════════════

/**
 * Recommended security headers for all responses
 */
export const SECURITY_HEADERS: Record<string, string> = {
  'X-Content-Type-Options': 'nosniff',
  'X-Frame-Options': 'SAMEORIGIN',
  'X-XSS-Protection': '1; mode=block',
  'Strict-Transport-Security': 'max-age=31536000; includeSubDomains; preload',
  'Referrer-Policy': 'strict-origin-when-cross-origin',
  'Permissions-Policy': 'camera=(), microphone=(), geolocation=(self)',
  'Content-Security-Policy': [
    "default-src 'self'",
    "script-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com",
    "style-src 'self' 'unsafe-inline'",
    "img-src 'self' data: https:",
    "font-src 'self' https://fonts.gstatic.com",
    "connect-src 'self' https://api.erp.vn wss://api.erp.vn",
    "frame-ancestors 'self'",
  ].join('; '),
};

// ════════════════════════════════════════════════════════════
// 8. Audit Trail
// ════════════════════════════════════════════════════════════

export interface AuditEntry {
  id: string;
  tenantId: string;
  userId: string;
  userName: string;
  action: string;
  resource: string;
  resourceId?: string;
  changes?: {
    before: Record<string, any>;
    after: Record<string, any>;
  };
  ipAddress: string;
  userAgent: string;
  metadata?: Record<string, any>;
  timestamp: Date;
}

/**
 * Create an audit log entry
 */
export function createAuditEntry(
  params: Omit<AuditEntry, 'id' | 'timestamp'>
): AuditEntry {
  return {
    ...params,
    id: `audit_${Date.now()}_${crypto.randomBytes(4).toString('hex')}`,
    timestamp: new Date(),
  };
}

/**
 * Detect sensitive field changes that need extra logging
 */
export function detectSensitiveChanges(
  before: Record<string, any>,
  after: Record<string, any>
): string[] {
  const sensitiveFields = [
    'password', 'email', 'phone', 'salary', 'bankAccount',
    'taxCode', 'role', 'permissions', 'apiKey', 'secret',
    'status', 'tier', 'amount', 'price',
  ];

  const changed: string[] = [];
  for (const field of sensitiveFields) {
    if (before[field] !== after[field] && (before[field] || after[field])) {
      changed.push(field);
    }
  }
  return changed;
}

// ════════════════════════════════════════════════════════════
// Errors
// ════════════════════════════════════════════════════════════

export class SecurityError extends Error {
  constructor(
    public code: string,
    message: string,
    public statusCode: number = 403
  ) {
    super(message);
    this.name = 'SecurityError';
  }
}

// ════════════════════════════════════════════════════════════
// Security Headers (TIP-015)
// ════════════════════════════════════════════════════════════

export * from './headers';
export * from './cors';
export * from './middleware';
export * from './csrf';
export * from './sanitize';

// ════════════════════════════════════════════════════════════
// Exports
// ════════════════════════════════════════════════════════════

export { ROLE_PERMISSIONS };
