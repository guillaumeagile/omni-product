export const PRICE_ERROR_KIND = {
    AmountNotPositive: 'PriceAmountNotPositive',
    AmountTooManyDecimals: 'PriceAmountTooManyDecimals',
    AmountTooHigh: 'PriceAmountTooHigh',
} as const;

export type PriceError =
    | { readonly kind: typeof PRICE_ERROR_KIND.AmountNotPositive; readonly amount: number }
    | { readonly kind: typeof PRICE_ERROR_KIND.AmountTooManyDecimals; readonly amount: number }
    | { readonly kind: typeof PRICE_ERROR_KIND.AmountTooHigh; readonly amount: number };
