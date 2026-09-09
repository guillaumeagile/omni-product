import type {CatalogItemStatus} from './catalog-item-status';

export const CATALOG_ITEM_ERROR_KIND = {
    ProductIdEmpty: 'CatalogItemProductIdEmpty',
    PublishWithoutImages: 'CatalogItemPublishWithoutImages',
    AlreadyPublished: 'CatalogItemAlreadyPublished',
    PublishArchived: 'CatalogItemPublishArchived',
    AlreadyArchived: 'CatalogItemAlreadyArchived',
    SlugChangeOnArchived: 'CatalogItemSlugChangeOnArchived',
} as const;

export type CatalogItemError =
    | { readonly kind: typeof CATALOG_ITEM_ERROR_KIND.ProductIdEmpty }
    | { readonly kind: typeof CATALOG_ITEM_ERROR_KIND.PublishWithoutImages }
    | { readonly kind: typeof CATALOG_ITEM_ERROR_KIND.AlreadyPublished }
    | { readonly kind: typeof CATALOG_ITEM_ERROR_KIND.PublishArchived }
    | { readonly kind: typeof CATALOG_ITEM_ERROR_KIND.AlreadyArchived }
    | { readonly kind: typeof CATALOG_ITEM_ERROR_KIND.SlugChangeOnArchived; readonly status: CatalogItemStatus };
