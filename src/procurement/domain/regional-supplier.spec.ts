import {describe, expect, it} from 'vitest';
import {RegionalSupplier} from './regional-supplier';
import {PROCUREMENT_ERROR_KIND} from './procurement-error';
import {SupplierId} from './supplier-id';
import {Region} from './region';
import {LeadTimeDays} from './lead-time-days';

const supplierId = SupplierId.create('supplier-1')._unsafeUnwrap();
const region = (code: string): Region => Region.create(code)._unsafeUnwrap();
const leadTime = (days: number): LeadTimeDays => LeadTimeDays.of(days)._unsafeUnwrap();

const supplier = (minimumOrderQuantity = 10): RegionalSupplier =>
    RegionalSupplier.create({supplierId, minimumOrderQuantity})._unsafeUnwrap();

describe('RegionalSupplier.create', () => {
    it('creates a supplier with no served regions', () => {
        const s = supplier();

        expect(s.supplierId.value).toBe('supplier-1');
        expect(s.minimumOrderQuantity).toBe(10);
        expect(s.servedRegions()).toEqual([]);
    });

    it('rejects a minimum order quantity of zero', () => {
        const result = RegionalSupplier.create({supplierId, minimumOrderQuantity: 0});

        expect(result._unsafeUnwrapErr()).toEqual({
            kind: PROCUREMENT_ERROR_KIND.MinimumOrderQuantityNotPositive,
            quantity: 0,
        });
    });

    it('rejects a negative minimum order quantity', () => {
        expect(
            RegionalSupplier.create({supplierId, minimumOrderQuantity: -5})._unsafeUnwrapErr().kind,
        ).toBe(PROCUREMENT_ERROR_KIND.MinimumOrderQuantityNotPositive);
    });

    it('rejects a non-integer minimum order quantity', () => {
        expect(
            RegionalSupplier.create({supplierId, minimumOrderQuantity: 2.5})._unsafeUnwrapErr(),
        ).toEqual({kind: PROCUREMENT_ERROR_KIND.MinimumOrderQuantityNotInteger, quantity: 2.5});
    });
});

describe('RegionalSupplier#assignRegion', () => {
    it('starts serving a region with the given lead time', () => {
        const assigned = supplier().assignRegion(region('EU'), leadTime(14))._unsafeUnwrap();

        expect(assigned.serves(region('EU'))).toBe(true);
        expect(assigned.leadTimeFor(region('EU'))?.days).toBe(14);
    });

    it('does not mutate the original supplier', () => {
        const s = supplier();

        s.assignRegion(region('EU'), leadTime(14));

        expect(s.serves(region('EU'))).toBe(false);
    });

    it('keeps existing assignments when adding another region', () => {
        const assigned = supplier()
            .assignRegion(region('EU'), leadTime(14))
            ._unsafeUnwrap()
            .assignRegion(region('US'), leadTime(30))
            ._unsafeUnwrap();

        expect(assigned.servedRegions().map(r => r.code)).toEqual(['EU', 'US']);
    });

    it('rejects assigning a region that is already served', () => {
        const assigned = supplier().assignRegion(region('EU'), leadTime(14))._unsafeUnwrap();

        expect(assigned.assignRegion(region('EU'), leadTime(20))._unsafeUnwrapErr()).toEqual({
            kind: PROCUREMENT_ERROR_KIND.RegionAlreadyAssigned,
            region: 'EU',
        });
    });
});

describe('RegionalSupplier#deactivateRegion', () => {
    it('stops serving a previously assigned region', () => {
        const deactivated = supplier()
            .assignRegion(region('EU'), leadTime(14))
            ._unsafeUnwrap()
            .deactivateRegion(region('EU'))
            ._unsafeUnwrap();

        expect(deactivated.serves(region('EU'))).toBe(false);
        expect(deactivated.servedRegions()).toEqual([]);
    });

    it('leaves other regions untouched', () => {
        const deactivated = supplier()
            .assignRegion(region('EU'), leadTime(14))
            ._unsafeUnwrap()
            .assignRegion(region('US'), leadTime(30))
            ._unsafeUnwrap()
            .deactivateRegion(region('EU'))
            ._unsafeUnwrap();

        expect(deactivated.servedRegions().map(r => r.code)).toEqual(['US']);
    });

    it('does not mutate the original supplier', () => {
        const assigned = supplier().assignRegion(region('EU'), leadTime(14))._unsafeUnwrap();

        assigned.deactivateRegion(region('EU'));

        expect(assigned.serves(region('EU'))).toBe(true);
    });

    it('rejects deactivating a region that was not assigned', () => {
        expect(supplier().deactivateRegion(region('EU'))._unsafeUnwrapErr()).toEqual({
            kind: PROCUREMENT_ERROR_KIND.RegionNotAssigned,
            region: 'EU',
        });
    });
});

describe('RegionalSupplier#leadTimeFor', () => {
    it('returns undefined for a region that is not served', () => {
        expect(supplier().leadTimeFor(region('EU'))).toBeUndefined();
    });
});
