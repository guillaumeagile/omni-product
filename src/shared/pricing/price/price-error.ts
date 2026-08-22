export type PriceError =
    | { readonly kind: 'PriceAmountNotPositive'; readonly amount: number }
    | { readonly kind: 'PriceAmountTooManyDecimals'; readonly amount: number }
    | { readonly kind: 'PriceAmountTooHigh'; readonly amount: number };
