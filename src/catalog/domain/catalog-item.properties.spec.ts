import {describe, it} from 'vitest';
import * as fc from 'fast-check';
import {CatalogItem} from './catalog-item';
import {CATALOG_ITEM_STATUS} from './catalog-item-status';
import {Slug} from './slug';
import {ProductTitle} from './product-title';
import {ImageCollection} from './image-collection';

const slug = Slug.create('red-bike')._unsafeUnwrap();
const title = ProductTitle.create('Red Bike')._unsafeUnwrap();
const oneImage = ImageCollection.of(['https://cdn.example.com/a.jpg'])._unsafeUnwrap();

const draft = (withImages: boolean): CatalogItem =>
    CatalogItem.create({
        productId: 'product-1',
        title,
        slug,
        images: withImages ? oneImage : undefined,
    })._unsafeUnwrap();

type Op = 'publish' | 'archive' | 'updateSlug';

const apply = (item: CatalogItem, op: Op): CatalogItem => {
    const result = op === 'updateSlug' ? item.updateSlug(slug) : item[op]();
    return result.isOk() ? result.value : item;
};

const anyOp = fc.constantFrom<Op>('publish', 'archive', 'updateSlug');

describe('CatalogItem properties', () => {
    it('property: status is always one of the three known values, whatever the op sequence', () => {
        fc.assert(
            fc.property(fc.boolean(), fc.array(anyOp, {maxLength: 20}), (withImages, ops) => {
                const finalItem = ops.reduce(apply, draft(withImages));

                return (
                    finalItem.status === CATALOG_ITEM_STATUS.Draft ||
                    finalItem.status === CATALOG_ITEM_STATUS.Published ||
                    finalItem.status === CATALOG_ITEM_STATUS.Archived
                );
            }),
        );
    });

    it('property: a PUBLISHED item always carries at least one image', () => {
        fc.assert(
            fc.property(fc.boolean(), fc.array(anyOp, {maxLength: 20}), (withImages, ops) => {
                const finalItem = ops.reduce(apply, draft(withImages));

                if (finalItem.status !== CATALOG_ITEM_STATUS.Published) {
                    return true;
                }

                return finalItem.images !== undefined && finalItem.images.count >= 1;
            }),
        );
    });

    it('property: publish never succeeds on an item created without images', () => {
        fc.assert(
            fc.property(fc.array(anyOp.filter(op => op !== 'publish'), {maxLength: 10}), ops => {
                // Apply only non-publish ops (updateSlug is a no-op on status here,
                // archive may fire) starting from a no-image draft, then try publish.
                const item = ops.reduce(apply, draft(false));
                const published = item.publish();

                return published.isErr();
            }),
        );
    });

    it('property: once ARCHIVED, no op ever moves the item out of ARCHIVED', () => {
        fc.assert(
            fc.property(fc.array(anyOp, {maxLength: 20}), ops => {
                const archived = draft(true).archive()._unsafeUnwrap();
                const finalItem = ops.reduce(apply, archived);

                return finalItem.status === CATALOG_ITEM_STATUS.Archived;
            }),
        );
    });

    it('property: every operation leaves the original instance untouched', () => {
        fc.assert(
            fc.property(fc.boolean(), anyOp, (withImages, op) => {
                const original = draft(withImages);
                const statusBefore = original.status;
                const slugBefore = original.slug.value;

                apply(original, op);

                return original.status === statusBefore && original.slug.value === slugBefore;
            }),
        );
    });
});
