export const PROCUREMENT_ERROR_KIND = {
    MinimumOrderQuantityNotPositive: 'ProcurementMinimumOrderQuantityNotPositive',
    MinimumOrderQuantityNotInteger: 'ProcurementMinimumOrderQuantityNotInteger',
    RegionAlreadyAssigned: 'ProcurementRegionAlreadyAssigned',
    RegionNotAssigned: 'ProcurementRegionNotAssigned',
} as const;

export type ProcurementError =
    | { readonly kind: typeof PROCUREMENT_ERROR_KIND.MinimumOrderQuantityNotPositive; readonly quantity: number }
    | { readonly kind: typeof PROCUREMENT_ERROR_KIND.MinimumOrderQuantityNotInteger; readonly quantity: number }
    | { readonly kind: typeof PROCUREMENT_ERROR_KIND.RegionAlreadyAssigned; readonly region: string }
    | { readonly kind: typeof PROCUREMENT_ERROR_KIND.RegionNotAssigned; readonly region: string };
