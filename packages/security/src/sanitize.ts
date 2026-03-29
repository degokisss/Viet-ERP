/**
 * Input Sanitization
 * XSS prevention and SQL injection prevention using allowlist approach
 */

/**
 * XSS-unsafe characters and entities
 */
const XSS_PATTERNS = {
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#x27;',
  '/': '&#x2F;',
};

/**
 * Sanitize input for XSS prevention
 * Escapes HTML special characters
 */
export function sanitizeInput(input: unknown): string {
  if (input === null || input === undefined) {
    return '';
  }

  const str = String(input).trim();

  return str.replace(/[<>"'\/]/g, (char) => {
    return XSS_PATTERNS[char as keyof typeof XSS_PATTERNS] || char;
  });
}

/**
 * Sanitize HTML content - removes all HTML tags
 */
export function sanitizeHTML(input: unknown): string {
  if (input === null || input === undefined) {
    return '';
  }

  const str = String(input);

  // Remove all HTML tags
  return str.replace(/<[^>]*>/g, '');
}

/**
 * Allowlist of safe characters for identifiers (table names, column names)
 */
const IDENTIFIER_REGEX = /^[a-zA-Z0-9_-]+$/;

/**
 * Validate identifier (table name, column name, etc.)
 * Only allows alphanumeric, underscore, and hyphen
 */
export function validateIdentifier(identifier: string): boolean {
  if (typeof identifier !== 'string' || identifier.length === 0) {
    return false;
  }

  if (identifier.length > 255) {
    return false;
  }

  return IDENTIFIER_REGEX.test(identifier);
}

/**
 * Allowed query parameter characters
 */
const SAFE_QUERY_REGEX = /^[a-zA-Z0-9\-._~:/?#[\]@!$&'()*+,;=%]*$/;

/**
 * Validate query parameter value
 */
export function validateQueryParam(value: unknown): boolean {
  if (value === null || value === undefined) {
    return true; // Null/undefined is valid (param not provided)
  }

  const str = String(value);

  if (str.length > 2048) {
    return false; // Prevent extremely long values
  }

  return SAFE_QUERY_REGEX.test(str);
}

/**
 * Sanitize query parameters object
 * Removes or escapes suspicious values
 */
export function sanitizeQuery(params: Record<string, unknown>): Record<
  string,
  unknown
> {
  const sanitized: Record<string, unknown> = {};

  for (const [key, value] of Object.entries(params)) {
    // Validate key
    if (!validateIdentifier(key)) {
      continue;
    }

    // Handle array values
    if (Array.isArray(value)) {
      sanitized[key] = value
        .map((v) => sanitizeQueryValue(v))
        .filter((v) => v !== null);
      continue;
    }

    // Handle single values
    const sanitizedValue = sanitizeQueryValue(value);
    if (sanitizedValue !== null) {
      sanitized[key] = sanitizedValue;
    }
  }

  return sanitized;
}

/**
 * Sanitize a single query parameter value
 */
function sanitizeQueryValue(value: unknown): unknown {
  if (value === null || value === undefined) {
    return null;
  }

  if (typeof value === 'boolean' || typeof value === 'number') {
    return value;
  }

  if (typeof value === 'string') {
    // Check length
    if (value.length > 2048) {
      return null;
    }

    // Check for safe characters
    if (!validateQueryParam(value)) {
      return null;
    }

    return value;
  }

  // Reject other types
  return null;
}

/**
 * Sanitize email address
 */
export function sanitizeEmail(email: unknown): string | null {
  if (typeof email !== 'string') {
    return null;
  }

  const trimmed = email.trim().toLowerCase();

  // Basic email validation regex
  const emailRegex =
    /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$/;

  if (!emailRegex.test(trimmed) || trimmed.length > 254) {
    return null;
  }

  return sanitizeInput(trimmed);
}

/**
 * Sanitize URL
 */
export function sanitizeURL(url: unknown): string | null {
  if (typeof url !== 'string') {
    return null;
  }

  try {
    const urlObj = new URL(url);

    // Only allow http and https
    if (!['http:', 'https:'].includes(urlObj.protocol)) {
      return null;
    }

    return urlObj.toString();
  } catch {
    return null;
  }
}

/**
 * Sanitize number (parse and validate)
 */
export function sanitizeNumber(value: unknown, min?: number, max?: number): number | null {
  const num = Number(value);

  if (isNaN(num) || !isFinite(num)) {
    return null;
  }

  if (min !== undefined && num < min) {
    return null;
  }

  if (max !== undefined && num > max) {
    return null;
  }

  return num;
}

/**
 * Sanitize phone number
 */
export function sanitizePhoneNumber(phone: unknown): string | null {
  if (typeof phone !== 'string') {
    return null;
  }

  // Remove all non-digit characters except + and -
  const cleaned = phone.replace(/[^\d+\-]/g, '');

  // Check length (7-15 digits)
  const digitCount = cleaned.replace(/\D/g, '').length;
  if (digitCount < 7 || digitCount > 15) {
    return null;
  }

  return cleaned;
}

/**
 * Validate Vietnamese phone number format
 */
export function validateVietnamesePhone(phone: string): boolean {
  // Vietnamese phone numbers start with 0, 84+ (country code), or +84
  const phoneRegex = /^(\+84|0)[1-9]\d{8,9}$/;
  return phoneRegex.test(phone.replace(/\s/g, ''));
}
