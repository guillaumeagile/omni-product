import {describe, expect, it} from 'vitest';
import {CatalogItem} from './catalog-item';
import {CATALOG_ITEM_STATUS} from './catalog-item-status';
import {CATALOG_ITEM_ERROR_KIND} from './catalog-item-error';
import {Slug} from './slug';
import {ProductTitle} from './product-title';
import {ImageCollection} from './image-collection';

const slug = (value = 'red-bike'): Slug => Slug.create(value)._unsafeUnwrap();
const title = (value = 'Red Bike'): ProductTitle => ProductTitle.create(value)._unsafeUnwrap();
const images = (...urls: string[]): ImageCollection =>
    ImageCollection.of(urls.length > 0 ? urls : ['https://cdn.example.com/a.jpg'])._unsafeUnwrap();

const draft = (overrides: Partial<Parameters<typeof CatalogItem.create>[0]> = {}): CatalogItem =>
    CatalogItem.create({
        productId: 'product-1',
        title: title(),
        slug: slug(),
        images: images(),
        ...overrides,
    })._unsafeUnwrap();

describe('CatalogItem.create', () => {
    it('starts a new item in DRAFT', () => {
        expect(draft().status).toBe(CATALOG_ITEM_STATUS.Draft);
    });

    it('keeps the supplied title, slug and images', () => {
        const item = draft();

        expect(item.title.value).toBe('Red Bike');
        expect(item.slug.value).toBe('red-bike');
        expect(item.images?.count).toBe(1);
    });

    it('allows creation without images', () => {
        const item = draft({images: undefined});

        expect(item.images).toBeUndefined();
        expect(item.status).toBe(CATALOG_ITEM_STATUS.Draft);
    });

    it('rejects an empty productId', () => {
        const result = CatalogItem.create({productId: '  ', title: title(), slug: slug()});

        expect(result._unsafeUnwrapErr()).toEqual({kind: CATALOG_ITEM_ERROR_KIND.ProductIdEmpty});
    });
});

describe('CatalogItem#publish', () => {
    it('moves a DRAFT with at least one image to PUBLISHED', () => {
        const published = draft().publish();

        expect(published._unsafeUnwrap().status).toBe(CATALOG_ITEM_STATUS.Published);
    });

    it('does not mutate the original item', () => {
        const item = draft();

        item.publish();

        expect(item.status).toBe(CATALOG_ITEM_STATUS.Draft);
    });

    it('rejects publishing without images', () => {
        const result = draft({images: undefined}).publish();

        expect(result._unsafeUnwrapErr()).toEqual({kind: CATALOG_ITEM_ERROR_KIND.PublishWithoutImages});
    });

    it('rejects re-publishing an already PUBLISHED item', () => {
        const published = draft().publish()._unsafeUnwrap();

        expect(published.publish()._unsafeUnwrapErr()).toEqual({
            kind: CATALOG_ITEM_ERROR_KIND.AlreadyPublished,
        });
    });

    it('rejects publishing an ARCHIVED item', () => {
        const archived = draft().archive()._unsafeUnwrap();

        expect(archived.publish()._unsafeUnwrapErr()).toEqual({
            kind: CATALOG_ITEM_ERROR_KIND.PublishArchived,
        });
    });
});

describe('CatalogItem#updateSlug', () => {
    it('replaces the slug on a DRAFT item', () => {
        const updated = draft().updateSlug(slug('blue-bike'));

        expect(updated._unsafeUnwrap().slug.value).toBe('blue-bike');
        expect(updated._unsafeUnwrap().status).toBe(CATALOG_ITEM_STATUS.Draft);
    });

    it('replaces the slug on a PUBLISHED item without changing its status', () => {
        const published = draft().publish()._unsafeUnwrap();

        const updated = published.updateSlug(slug('blue-bike'));

        expect(updated._unsafeUnwrap().slug.value).toBe('blue-bike');
        expect(updated._unsafeUnwrap().status).toBe(CATALOG_ITEM_STATUS.Published);
    });

    it('does not mutate the original item', () => {
        const item = draft();

        item.updateSlug(slug('blue-bike'));

        expect(item.slug.value).toBe('red-bike');
    });

    it('rejects a slug change on an ARCHIVED item', () => {
        const archived = draft().archive()._unsafeUnwrap();

        expect(archived.updateSlug(slug('blue-bike'))._unsafeUnwrapErr()).toEqual({
            kind: CATALOG_ITEM_ERROR_KIND.SlugChangeOnArchived,
            status: CATALOG_ITEM_STATUS.Archived,
        });
    });
});

describe('CatalogItem#archive', () => {
    it('moves a DRAFT item to ARCHIVED', () => {
        expect(draft().archive()._unsafeUnwrap().status).toBe(CATALOG_ITEM_STATUS.Archived);
    });

    it('moves a PUBLISHED item to ARCHIVED', () => {
        const published = draft().publish()._unsafeUnwrap();

        expect(published.archive()._unsafeUnwrap().status).toBe(CATALOG_ITEM_STATUS.Archived);
    });

    it('rejects archiving an already ARCHIVED item', () => {
        const archived = draft().archive()._unsafeUnwrap();

        expect(archived.archive()._unsafeUnwrapErr()).toEqual({
            kind: CATALOG_ITEM_ERROR_KIND.AlreadyArchived,
        });
    });

    it('does not mutate the original item', () => {
        const item = draft();

        item.archive();

        expect(item.status).toBe(CATALOG_ITEM_STATUS.Draft);
    });
});
