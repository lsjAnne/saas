import { apiClient } from '@/services/http/apiClient';
import type { LoginPayload } from '@/services/apiTypes';

export interface LoginRequest {
  username: string;
  password: string;
}

export function login(request: LoginRequest) {
  return apiClient.post<LoginPayload>('/api/auth/login', request);
}
