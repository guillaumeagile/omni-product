export const IMAGE_COLLECTION_ERROR_KIND = {
    Empty: 'ImageCollectionEmpty',
    InvalidImageUrl: 'ImageCollectionInvalidImageUrl',
    DuplicateImageUrl: 'ImageCollectionDuplicateImageUrl',
} as const;

export type ImageCollectionError =
    | { readonly kind: typeof IMAGE_COLLECTION_ERROR_KIND.Empty }
    | { readonly kind: typeof IMAGE_COLLECTION_ERROR_KIND.InvalidImageUrl; readonly value: string }
    | { readonly kind: typeof IMAGE_COLLECTION_ERROR_KIND.DuplicateImageUrl; readonly value: string };
