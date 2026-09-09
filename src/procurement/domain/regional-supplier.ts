import {err, ok, type Result} from 'neverthrow';
import {PROCUREMENT_ERROR_KIND, type ProcurementError} from './procurement-error';
import type {CreateRegionalSupplierProps} from './create-regional-supplier-props';
import type {RegionAssignment} from './region-assignment';
import type {SupplierId} from './supplier-id';
import type {Region} from './region';
import type {LeadTimeDays} from './lead-time-days';

/**
 * Procurement aggregate for a supplier and the regions it serves.
 *
 * Always valid: the private constructor means every `RegionalSupplier` has a
 * positive integer minimum order quantity, and every region it serves is a
 * supported {@link Region} with a valid {@link LeadTimeDays} — the legacy
 * `Record<string, unknown>` dictionary is gone.
 *
 * Immutable: `assignRegion` / `deactivateRegion` return a new
 * `RegionalSupplier`, or an `err` describing why the change is not allowed,
 * and never mutate the instance they are called on.
 */
export class RegionalSupplier {
    private constructor(
        readonly supplierId: SupplierId,
        readonly minimumOrderQuantity: number,
        private readonly assignments: readonly RegionAssignment[],
    ) {}

    static create(props: CreateRegionalSupplierProps): Result<RegionalSupplier, ProcurementError> {
        const moq = props.minimumOrderQuantity;

        if (!Number.isInteger(moq)) {
            return err({kind: PROCUREMENT_ERROR_KIND.MinimumOrderQuantityNotInteger, quantity: moq});
        }
        if (moq <= 0) {
            return err({kind: PROCUREMENT_ERROR_KIND.MinimumOrderQuantityNotPositive, quantity: moq});
        }

        return ok(new RegionalSupplier(props.supplierId, moq, []));
    }

    /**
     * Starts serving `region` with the given lead time. Fails when the region
     * is already assigned — changing an existing lead time is not this method's
     * job.
     */
    assignRegion(region: Region, leadTime: LeadTimeDays): Result<RegionalSupplier, ProcurementError> {
        if (this.serves(region)) {
            return err({kind: PROCUREMENT_ERROR_KIND.RegionAlreadyAssigned, region: region.code});
        }

        return ok(
            new RegionalSupplier(this.supplierId, this.minimumOrderQuantity, [
                ...this.assignments,
                {region, leadTime},
            ]),
        );
    }

    /** Stops serving `region`. Fails when the region was not assigned. */
    deactivateRegion(region: Region): Result<RegionalSupplier, ProcurementError> {
        if (!this.serves(region)) {
            return err({kind: PROCUREMENT_ERROR_KIND.RegionNotAssigned, region: region.code});
        }

        return ok(
            new RegionalSupplier(
                this.supplierId,
                this.minimumOrderQuantity,
                this.assignments.filter(assignment => !assignment.region.equals(region)),
            ),
        );
    }

    /** True when the supplier currently serves `region`. */
    serves(region: Region): boolean {
        return this.assignments.some(assignment => assignment.region.equals(region));
    }

    /** The regions currently served, in assignment order. */
    servedRegions(): readonly Region[] {
        return this.assignments.map(assignment => assignment.region);
    }

    /** The lead time promised for `region`, or `undefined` when it is not served. */
    leadTimeFor(region: Region): LeadTimeDays | undefined {
        return this.assignments.find(assignment => assignment.region.equals(region))?.leadTime;
    }
}
