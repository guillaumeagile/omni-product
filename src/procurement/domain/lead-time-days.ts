import {err, ok, type Result} from 'neverthrow';
import {LEAD_TIME_DAYS_ERROR_KIND, type LeadTimeDaysError} from './lead-time-days-error';

// A year is the outer bound: anything longer is a data-entry mistake, not a
// real procurement lead time.
const MAX_DAYS = 365;

/**
 * The number of days between placing an order with a supplier and receiving it.
 * Always valid: a positive integer, at most {@link MAX_DAYS}.
 */
export class LeadTimeDays {
    private constructor(readonly days: number) {}

    static of(days: number): Result<LeadTimeDays, LeadTimeDaysError> {
        if (!Number.isInteger(days)) {
            return err({kind: LEAD_TIME_DAYS_ERROR_KIND.NotInteger, days});
        }
        if (days <= 0) {
            return err({kind: LEAD_TIME_DAYS_ERROR_KIND.NotPositive, days});
        }
        if (days > MAX_DAYS) {
            return err({kind: LEAD_TIME_DAYS_ERROR_KIND.TooLong, days});
        }

        return ok(new LeadTimeDays(days));
    }

    equals(other: LeadTimeDays): boolean {
        return this.days === other.days;
    }
}
