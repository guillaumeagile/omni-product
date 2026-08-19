export type MarginError =
    | { readonly kind: 'MarginPercentageOutOfRange'; readonly percentage: number }
    | { readonly kind: 'MarginPercentageNotFinite'; readonly percentage: number };
