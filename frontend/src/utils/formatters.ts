export function formatCurrency(value: number | string | null | undefined) {
  if (value == null || value === '') {
    return '--';
  }

  const amount = Number(value);
  if (Number.isNaN(amount)) {
    return '--';
  }

  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: 'CNY',
    maximumFractionDigits: 0
  }).format(amount);
}

export function formatDate(value: string | null | undefined) {
  if (!value) {
    return '--';
  }

  return new Date(value).toLocaleDateString('zh-CN');
}

export function formatDateTime(value: string | null | undefined) {
  if (!value) {
    return '--';
  }

  return new Date(value).toLocaleString('zh-CN', {
    hour12: false
  });
}

export function formatPercent(value: number | string | null | undefined, digits = 1) {
  if (value == null || value === '') {
    return '--';
  }

  const ratio = Number(value);
  if (Number.isNaN(ratio)) {
    return '--';
  }

  return `${(ratio * 100).toFixed(digits)}%`;
}

export function formatCount(value: number | string | null | undefined, suffix = '') {
  if (value == null || value === '') {
    return '--';
  }

  return `${Number(value).toLocaleString('zh-CN')}${suffix}`;
}
