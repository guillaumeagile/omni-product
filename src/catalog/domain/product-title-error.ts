export const PRODUCT_TITLE_ERROR_KIND = {
    Empty: 'ProductTitleEmpty',
    TooLong: 'ProductTitleTooLong',
} as const;

export type ProductTitleError =
    | { readonly kind: typeof PRODUCT_TITLE_ERROR_KIND.Empty }
    | { readonly kind: typeof PRODUCT_TITLE_ERROR_KIND.TooLong; readonly length: number };
