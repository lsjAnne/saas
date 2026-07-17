import { apiClient } from '@/services/http/apiClient';
import type {
  CandidateProduct,
  CreateCandidateProductPayload,
  GenerateProductDraftPayload,
  Product,
  ProductDraft,
  PublishProductDraftPayload,
  UpdateCandidateProductPayload,
  UpdateCandidateProductStatusPayload,
  UpdateProductDraftPayload
} from '@/services/apiTypes';

export function getCandidateProducts(token: string) {
  return apiClient.get<CandidateProduct[]>('/api/candidate-products', token);
}

export function createCandidateProduct(
  payload: CreateCandidateProductPayload,
  token: string
) {
  return apiClient.post<CandidateProduct>('/api/candidate-products', payload, token);
}

export function updateCandidateProduct(
  candidateProductId: string,
  payload: UpdateCandidateProductPayload,
  token: string
) {
  return apiClient.put<CandidateProduct>(
    `/api/candidate-products/${candidateProductId}`,
    payload,
    token
  );
}

export function updateCandidateProductStatus(
  candidateProductId: string,
  payload: UpdateCandidateProductStatusPayload,
  token: string
) {
  return apiClient.post<CandidateProduct>(
    `/api/candidate-products/${candidateProductId}/status`,
    payload,
    token
  );
}

export function getProductDrafts(token: string) {
  return apiClient.get<ProductDraft[]>('/api/product-drafts', token);
}

export function generateProductDraft(
  payload: GenerateProductDraftPayload,
  token: string
) {
  return apiClient.post<ProductDraft>('/api/product-drafts/generate', payload, token);
}

export function updateProductDraft(
  productDraftId: string,
  payload: UpdateProductDraftPayload,
  token: string
) {
  return apiClient.put<ProductDraft>(
    `/api/product-drafts/${productDraftId}`,
    payload,
    token
  );
}

export function publishProductDraft(
  productDraftId: string,
  payload: PublishProductDraftPayload,
  token: string
) {
  return apiClient.post<Product>(
    `/api/product-drafts/${productDraftId}/publish`,
    payload,
    token
  );
}
