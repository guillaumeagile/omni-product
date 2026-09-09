export const REGION_ERROR_KIND = {
    Unsupported: 'RegionUnsupported',
} as const;

export type RegionError = {
    readonly kind: typeof REGION_ERROR_KIND.Unsupported;
    readonly value: string;
};
