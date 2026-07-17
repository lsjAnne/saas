import { apiClient } from '@/services/http/apiClient';
import type {
  ArchiveContentAssetVersionPayload,
  ContentAsset,
  ContentAssetDetailView,
  ContentAssetReferenceCopyView,
  GenerateContentAssetPayload,
  PublishContentAssetPayload,
  UploadContentAssetPayload
} from '@/services/apiTypes';

export function getContentAssets(token: string) {
  return apiClient.get<ContentAsset[]>('/api/content-assets', token);
}

export function getContentAssetDetail(assetId: string, token: string) {
  return apiClient.get<ContentAssetDetailView>(`/api/content-assets/${assetId}`, token);
}

export function uploadContentAsset(payload: UploadContentAssetPayload, token: string) {
  return apiClient.post<ContentAssetDetailView>('/api/content-assets/upload', payload, token);
}

export function generateContentAsset(payload: GenerateContentAssetPayload, token: string) {
  return apiClient.post<ContentAssetDetailView>('/api/content-assets/generate', payload, token);
}

export function archiveContentAssetVersion(
  assetId: string,
  payload: ArchiveContentAssetVersionPayload,
  token: string
) {
  return apiClient.post<ContentAssetDetailView>(
    `/api/content-assets/${assetId}/archive-version`,
    payload,
    token
  );
}

export function copyContentAssetReference(assetId: string, token: string) {
  return apiClient.post<ContentAssetReferenceCopyView>(
    `/api/content-assets/${assetId}/copy-reference`,
    null,
    token
  );
}

export function publishContentAsset(
  assetId: string,
  payload: PublishContentAssetPayload,
  token: string
) {
  return apiClient.post<ContentAssetDetailView>(
    `/api/content-assets/${assetId}/publish`,
    payload,
    token
  );
}
