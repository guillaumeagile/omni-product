/**
 * The ways a `Money` can fail to be constructed: a tagged union discriminated
 * by `kind`. The discriminants live once in `MONEY_ERROR_KIND`; the variant
 * types, factories, and guards all derive from it, so no `kind` string is
 * written twice.
 */
export const MONEY_ERROR_KIND = {
    NegativeAmountNotAllowed: 'NegativeAmountNotAllowed',
} as const;

export type NegativeAmountNotAllowed = {
    readonly kind: typeof MONEY_ERROR_KIND.NegativeAmountNotAllowed;
    readonly amount: number;
};

export type MoneyError = NegativeAmountNotAllowed;

export const negativeAmountNotAllowed = (amount: number): NegativeAmountNotAllowed => ({
    kind: MONEY_ERROR_KIND.NegativeAmountNotAllowed,
    amount,
});

export const isNegativeAmountNotAllowed = (
    error: MoneyError,
): error is NegativeAmountNotAllowed =>
    error.kind === MONEY_ERROR_KIND.NegativeAmountNotAllowed;
