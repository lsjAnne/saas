import type { ApiResponse } from '@/services/apiTypes';

const AUTH_STORAGE_KEY = 'ark-commerce-auth';
export const AUTH_UNAUTHORIZED_EVENT = 'ark-commerce-auth-unauthorized';

export class ApiError extends Error {
  status: number;
  code?: string;
  traceId?: string;
  data?: unknown;

  constructor(message: string, status: number, code?: string, traceId?: string, data?: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.traceId = traceId;
    this.data = data;
  }
}

function resolveBaseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL?.trim();

  if (!configured) {
    return '';
  }

  return configured.replace(/\/$/, '');
}

function buildHeaders(token?: string, body?: BodyInit | null) {
  const headers = new Headers();
  headers.set('Accept', 'application/json');

  if (body && !(body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  return headers;
}

async function parseError(response: Response) {
  let payload: ApiResponse<unknown> | null = null;

  try {
    payload = (await response.json()) as ApiResponse<unknown>;
  } catch {
    payload = null;
  }

  if (response.status === 401) {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT));
  }

  throw new ApiError(
    payload?.message || `request failed with status ${response.status}`,
    response.status,
    payload?.code,
    payload?.traceId,
    payload?.data
  );
}

async function request<T>(path: string, init?: RequestInit, token?: string): Promise<T> {
  const baseUrl = resolveBaseUrl();
  const url = `${baseUrl}${path}`;
  const response = await fetch(url, {
    ...init,
    headers: buildHeaders(token, init?.body ?? null)
  });

  if (!response.ok) {
    return parseError(response);
  }

  const payload = (await response.json()) as ApiResponse<T>;
  return payload.data;
}

export const apiClient = {
  get<T>(path: string, token?: string) {
    return request<T>(path, { method: 'GET' }, token);
  },
  post<T>(path: string, body?: unknown, token?: string) {
    return request<T>(
      path,
      {
        method: 'POST',
        body: body == null ? null : JSON.stringify(body)
      },
      token
    );
  },
  put<T>(path: string, body?: unknown, token?: string) {
    return request<T>(
      path,
      {
        method: 'PUT',
        body: body == null ? null : JSON.stringify(body)
      },
      token
    );
  },
  delete<T>(path: string, token?: string) {
    return request<T>(path, { method: 'DELETE' }, token);
  }
};

export { AUTH_STORAGE_KEY };
