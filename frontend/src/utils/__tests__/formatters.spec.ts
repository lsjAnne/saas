import { describe, expect, it } from 'vitest';

import {
  formatCount,
  formatCurrency,
  formatDate,
  formatDateTime,
  formatPercent
} from '../formatters';

describe('formatCurrency', () => {
  it('returns placeholder for empty or invalid values', () => {
    expect(formatCurrency(null)).toBe('--');
    expect(formatCurrency(undefined)).toBe('--');
    expect(formatCurrency('')).toBe('--');
    expect(formatCurrency('not-a-number')).toBe('--');
  });

  it('formats numbers as CNY without fraction digits', () => {
    expect(formatCurrency(129)).toBe('¥129');
    expect(formatCurrency(1299.6)).toBe('¥1,300');
    expect(formatCurrency('88')).toBe('¥88');
  });
});

describe('formatPercent', () => {
  it('returns placeholder for empty values', () => {
    expect(formatPercent(null)).toBe('--');
    expect(formatPercent('')).toBe('--');
  });

  it('converts ratio to percentage with default digits', () => {
    expect(formatPercent(0.186)).toBe('18.6%');
    expect(formatPercent(1)).toBe('100.0%');
  });

  it('respects custom digits', () => {
    expect(formatPercent(0.12345, 2)).toBe('12.35%');
    expect(formatPercent(0.5, 0)).toBe('50%');
  });
});

describe('formatCount', () => {
  it('returns placeholder for empty values', () => {
    expect(formatCount(null)).toBe('--');
    expect(formatCount(undefined)).toBe('--');
    expect(formatCount('')).toBe('--');
  });

  it('formats numbers with zh-CN separators and optional suffix', () => {
    expect(formatCount(1234567)).toBe('1,234,567');
    expect(formatCount(999, ' 单')).toBe('999 单');
  });
});

describe('formatDate / formatDateTime', () => {
  it('returns placeholder for missing values', () => {
    expect(formatDate(null)).toBe('--');
    expect(formatDate(undefined)).toBe('--');
    expect(formatDateTime('')).toBe('--');
  });

  it('formats valid date strings', () => {
    expect(formatDate('2026-06-01T00:00:00+08:00')).toContain('2026');
    expect(formatDateTime('2026-06-01T10:30:00+08:00')).toContain('2026');
  });
});
