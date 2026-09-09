export const CATALOG_ITEM_STATUS = {
    Draft: 'DRAFT',
    Published: 'PUBLISHED',
    Archived: 'ARCHIVED',
} as const;

export type CatalogItemStatus = (typeof CATALOG_ITEM_STATUS)[keyof typeof CATALOG_ITEM_STATUS];
