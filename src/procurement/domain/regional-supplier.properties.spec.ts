import {describe, it} from 'vitest';
import * as fc from 'fast-check';
import {RegionalSupplier} from './regional-supplier';
import {SupplierId} from './supplier-id';
import {Region} from './region';
import {LeadTimeDays} from './lead-time-days';

const supplierId = SupplierId.create('supplier-1')._unsafeUnwrap();
const emptySupplier = RegionalSupplier.create({supplierId, minimumOrderQuantity: 10})._unsafeUnwrap();

const anyRegionCode = fc.constantFrom('EU', 'UK', 'US', 'APAC');
const anyLeadTime = fc.integer({min: 1, max: 365});

type Move =
    | { readonly op: 'assign'; readonly code: string; readonly days: number }
    | { readonly op: 'deactivate'; readonly code: string };

const anyMove: fc.Arbitrary<Move> = fc.oneof(
    fc.record({op: fc.constant('assign' as const), code: anyRegionCode, days: anyLeadTime}),
    fc.record({op: fc.constant('deactivate' as const), code: anyRegionCode}),
);

const apply = (supplier: RegionalSupplier, move: Move): RegionalSupplier => {
    const region = Region.create(move.code)._unsafeUnwrap();
    const result =
        move.op === 'assign'
            ? supplier.assignRegion(region, LeadTimeDays.of(move.days)._unsafeUnwrap())
            : supplier.deactivateRegion(region);

    return result.isOk() ? result.value : supplier;
};

describe('RegionalSupplier properties', () => {
    it('property: minimum order quantity is untouched by any sequence of region moves', () => {
        fc.assert(
            fc.property(fc.array(anyMove, {maxLength: 30}), moves => {
                const finalSupplier = moves.reduce(apply, emptySupplier);

                return finalSupplier.minimumOrderQuantity === 10;
            }),
        );
    });

    it('property: every served region is one of the four supported regions', () => {
        fc.assert(
            fc.property(fc.array(anyMove, {maxLength: 30}), moves => {
                const finalSupplier = moves.reduce(apply, emptySupplier);
                const supported = Region.supported() as readonly string[];

                return finalSupplier.servedRegions().every(r => supported.includes(r.code));
            }),
        );
    });

    it('property: served regions never contain a duplicate', () => {
        fc.assert(
            fc.property(fc.array(anyMove, {maxLength: 30}), moves => {
                const finalSupplier = moves.reduce(apply, emptySupplier);
                const codes = finalSupplier.servedRegions().map(r => r.code);

                return new Set(codes).size === codes.length;
            }),
        );
    });

    it('property: a served region always has a retrievable lead time, an unserved one never does', () => {
        fc.assert(
            fc.property(fc.array(anyMove, {maxLength: 30}), moves => {
                const finalSupplier = moves.reduce(apply, emptySupplier);

                return (Region.supported() as readonly string[]).every(code => {
                    const region = Region.create(code)._unsafeUnwrap();
                    const served = finalSupplier.serves(region);
                    const hasLeadTime = finalSupplier.leadTimeFor(region) !== undefined;

                    return served === hasLeadTime;
                });
            }),
        );
    });

    it('property: assign then deactivate the same region returns to not serving it', () => {
        fc.assert(
            fc.property(anyRegionCode, anyLeadTime, (code, days) => {
                const region = Region.create(code)._unsafeUnwrap();
                const roundTripped = emptySupplier
                    .assignRegion(region, LeadTimeDays.of(days)._unsafeUnwrap())
                    .andThen(s => s.deactivateRegion(region));

                return roundTripped.isOk() && !roundTripped.value.serves(region);
            }),
        );
    });
});
