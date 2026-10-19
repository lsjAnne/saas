export type NavGroupKey =
  | 'overview'
  | 'supply'
  | 'order'
  | 'channel'
  | 'service'
  | 'finance'
  | 'system';

export interface NavGroupDefinition {
  key: NavGroupKey;
  label: string;
}

export const NAV_GROUPS: NavGroupDefinition[] = [
  { key: 'overview', label: '经营总览' },
  { key: 'supply', label: '商品供应链' },
  { key: 'order', label: '订单履约' },
  { key: 'channel', label: '渠道与营销' },
  { key: 'service', label: '服务与协作' },
  { key: 'finance', label: '财务与数据' },
  { key: 'system', label: '系统与平台' }
];
