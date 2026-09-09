import {describe, expect, it} from 'vitest';
import {LeadTimeDays} from './lead-time-days';
import {LEAD_TIME_DAYS_ERROR_KIND} from './lead-time-days-error';

describe('LeadTimeDays.of', () => {
    it('accepts a positive integer', () => {
        expect(LeadTimeDays.of(14)._unsafeUnwrap().days).toBe(14);
    });

    it('accepts the maximum of 365 days', () => {
        expect(LeadTimeDays.of(365)._unsafeUnwrap().days).toBe(365);
    });

    it('rejects zero', () => {
        expect(LeadTimeDays.of(0)._unsafeUnwrapErr()).toEqual({
            kind: LEAD_TIME_DAYS_ERROR_KIND.NotPositive,
            days: 0,
        });
    });

    it('rejects a negative value', () => {
        expect(LeadTimeDays.of(-3)._unsafeUnwrapErr()).toEqual({
            kind: LEAD_TIME_DAYS_ERROR_KIND.NotPositive,
            days: -3,
        });
    });

    it('rejects a non-integer value', () => {
        expect(LeadTimeDays.of(2.5)._unsafeUnwrapErr()).toEqual({
            kind: LEAD_TIME_DAYS_ERROR_KIND.NotInteger,
            days: 2.5,
        });
    });

    it('rejects a value beyond 365 days', () => {
        expect(LeadTimeDays.of(366)._unsafeUnwrapErr()).toEqual({
            kind: LEAD_TIME_DAYS_ERROR_KIND.TooLong,
            days: 366,
        });
    });
});

describe('LeadTimeDays#equals', () => {
    it('is true for the same number of days', () => {
        expect(LeadTimeDays.of(14)._unsafeUnwrap().equals(LeadTimeDays.of(14)._unsafeUnwrap())).toBe(true);
    });

    it('is false for a different number of days', () => {
        expect(LeadTimeDays.of(14)._unsafeUnwrap().equals(LeadTimeDays.of(21)._unsafeUnwrap())).toBe(false);
    });
});
