export const SLUG_ERROR_KIND = {
    TooShort: 'SlugTooShort',
    InvalidFormat: 'SlugInvalidFormat',
} as const;

export type SlugError =
    | { readonly kind: typeof SLUG_ERROR_KIND.TooShort; readonly value: string; readonly length: number }
    | { readonly kind: typeof SLUG_ERROR_KIND.InvalidFormat; readonly value: string };
