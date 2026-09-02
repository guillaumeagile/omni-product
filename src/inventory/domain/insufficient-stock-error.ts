export const INSUFFICIENT_STOCK_ERROR_KIND = {
    NotEnoughAvailable: 'InsufficientStockNotEnoughAvailable',
} as const;

export type InsufficientStockError =
    {
        readonly kind: typeof INSUFFICIENT_STOCK_ERROR_KIND.NotEnoughAvailable;
        readonly requested: number;
        readonly available: number
    };
