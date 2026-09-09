import type {Region} from './region';
import type {LeadTimeDays} from './lead-time-days';

/**
 * One active supplier-to-region link: the region served and the lead time
 * promised for it. Plain data — the aggregate owns the collection of these.
 */
export interface RegionAssignment {
    readonly region: Region;
    readonly leadTime: LeadTimeDays;
}
