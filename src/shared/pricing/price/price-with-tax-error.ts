export const PRICE_WITH_TAX_ERROR_KIND = {
    RateOutOfRange: 'PriceWithTaxRateOutOfRange',
} as const;

export type PriceWithTaxError = { readonly kind: typeof PRICE_WITH_TAX_ERROR_KIND.RateOutOfRange; readonly rate: number };
