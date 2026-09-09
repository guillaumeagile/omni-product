export const STOCK_ITEM_ERROR_KIND = {
    ProductIdEmpty: 'StockItemProductIdEmpty',
    AvailableQuantityNegative: 'StockItemAvailableQuantityNegative',
    AvailableQuantityNotInteger: 'StockItemAvailableQuantityNotInteger',
    QuantityNotPositive: 'StockItemQuantityNotPositive',
    QuantityNotInteger: 'StockItemQuantityNotInteger',
    InsufficientStock: 'StockItemInsufficientStock',
    InsufficientReservation: 'StockItemInsufficientReservation',
} as const;

export type StockItemError =
    | { readonly kind: typeof STOCK_ITEM_ERROR_KIND.ProductIdEmpty }
    | { readonly kind: typeof STOCK_ITEM_ERROR_KIND.AvailableQuantityNegative; readonly availableQuantity: number }
    | { readonly kind: typeof STOCK_ITEM_ERROR_KIND.AvailableQuantityNotInteger; readonly availableQuantity: number }
    | { readonly kind: typeof STOCK_ITEM_ERROR_KIND.QuantityNotPositive; readonly quantity: number }
    | { readonly kind: typeof STOCK_ITEM_ERROR_KIND.QuantityNotInteger; readonly quantity: number }
    | { readonly kind: typeof STOCK_ITEM_ERROR_KIND.InsufficientStock; readonly requested: number; readonly available: number }
    | { readonly kind: typeof STOCK_ITEM_ERROR_KIND.InsufficientReservation; readonly requested: number; readonly reserved: number };
