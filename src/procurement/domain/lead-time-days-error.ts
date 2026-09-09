export const LEAD_TIME_DAYS_ERROR_KIND = {
    NotPositive: 'LeadTimeDaysNotPositive',
    NotInteger: 'LeadTimeDaysNotInteger',
    TooLong: 'LeadTimeDaysTooLong',
} as const;

export type LeadTimeDaysError =
    | { readonly kind: typeof LEAD_TIME_DAYS_ERROR_KIND.NotPositive; readonly days: number }
    | { readonly kind: typeof LEAD_TIME_DAYS_ERROR_KIND.NotInteger; readonly days: number }
    | { readonly kind: typeof LEAD_TIME_DAYS_ERROR_KIND.TooLong; readonly days: number };
