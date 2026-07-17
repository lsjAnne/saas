import { apiClient } from '@/services/http/apiClient';
import type {
  CreateProductSourceMappingPayload,
  Product,
  ProductMappingCatalogEntry,
  ProductSourceMapping,
  SwitchProductMappingSupplierPayload
} from '@/services/apiTypes';

export function getProducts(token: string) {
  return apiClient.get<Product[]>('/api/products', token);
}

export function getProductMappingCatalog(token: string) {
  return apiClient.get<ProductMappingCatalogEntry[]>('/api/product-mappings/catalog', token);
}

export function getProductMappings(token: string) {
  return apiClient.get<ProductSourceMapping[]>('/api/product-mappings', token);
}

export function createProductMapping(
  payload: CreateProductSourceMappingPayload,
  token: string
) {
  return apiClient.post<ProductSourceMapping>('/api/product-mappings', payload, token);
}

export function switchProductMappingSupplier(
  productMappingId: string,
  payload: SwitchProductMappingSupplierPayload,
  token: string
) {
  return apiClient.post<ProductSourceMapping>(
    `/api/product-mappings/${productMappingId}/switch-supplier`,
    payload,
    token
  );
}
