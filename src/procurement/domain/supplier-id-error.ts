export const SUPPLIER_ID_ERROR_KIND = {
    Empty: 'SupplierIdEmpty',
} as const;

export type SupplierIdError = {
    readonly kind: typeof SUPPLIER_ID_ERROR_KIND.Empty;
};
