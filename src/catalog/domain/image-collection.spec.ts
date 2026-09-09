import {describe, expect, it} from 'vitest';
import {ImageCollection} from './image-collection';
import {IMAGE_COLLECTION_ERROR_KIND} from './image-collection-error';

describe('ImageCollection.of', () => {
    it('accepts a single valid image URL', () => {
        const result = ImageCollection.of(['https://cdn.example.com/a.jpg']);

        expect(result._unsafeUnwrap().urls).toEqual(['https://cdn.example.com/a.jpg']);
        expect(result._unsafeUnwrap().count).toBe(1);
    });

    it('preserves order across multiple URLs', () => {
        const urls = ['https://cdn.example.com/a.jpg', 'http://cdn.example.com/b.png'];

        expect(ImageCollection.of(urls)._unsafeUnwrap().urls).toEqual(urls);
    });

    it('trims each URL before validating', () => {
        expect(ImageCollection.of(['  https://cdn.example.com/a.jpg '])._unsafeUnwrap().urls).toEqual([
            'https://cdn.example.com/a.jpg',
        ]);
    });

    it('rejects an empty list', () => {
        expect(ImageCollection.of([])._unsafeUnwrapErr()).toEqual({
            kind: IMAGE_COLLECTION_ERROR_KIND.Empty,
        });
    });

    it.each([
        'ftp://cdn.example.com/a.jpg',
        'cdn.example.com/a.jpg',
        'not a url',
        '',
        // whitespace *inside* the URL — the trailing `$` anchor must reject this
        'https://cdn.example.com/a.jpg and more',
        // junk *before* the scheme — the leading `^` anchor must reject this
        'see https://cdn.example.com/a.jpg',
    ])('rejects a malformed image URL: %s', bad => {
        expect(ImageCollection.of([bad])._unsafeUnwrapErr()).toEqual({
            kind: IMAGE_COLLECTION_ERROR_KIND.InvalidImageUrl,
            value: bad.trim(),
        });
    });

    it('rejects a duplicate URL and reports the repeated value', () => {
        const url = 'https://cdn.example.com/a.jpg';

        expect(ImageCollection.of([url, url])._unsafeUnwrapErr()).toEqual({
            kind: IMAGE_COLLECTION_ERROR_KIND.DuplicateImageUrl,
            value: url,
        });
    });
});

describe('ImageCollection#equals', () => {
    it('is true for the same URLs in the same order', () => {
        const a = ImageCollection.of(['https://cdn.example.com/a.jpg', 'https://cdn.example.com/b.jpg'])._unsafeUnwrap();
        const b = ImageCollection.of(['https://cdn.example.com/a.jpg', 'https://cdn.example.com/b.jpg'])._unsafeUnwrap();

        expect(a.equals(b)).toBe(true);
    });

    it('is false when a later URL differs even though the first matches', () => {
        const a = ImageCollection.of(['https://cdn.example.com/a.jpg', 'https://cdn.example.com/b.jpg'])._unsafeUnwrap();
        const b = ImageCollection.of(['https://cdn.example.com/a.jpg', 'https://cdn.example.com/c.jpg'])._unsafeUnwrap();

        expect(a.equals(b)).toBe(false);
    });

    it('is false when the order differs', () => {
        const a = ImageCollection.of(['https://cdn.example.com/a.jpg', 'https://cdn.example.com/b.jpg'])._unsafeUnwrap();
        const b = ImageCollection.of(['https://cdn.example.com/b.jpg', 'https://cdn.example.com/a.jpg'])._unsafeUnwrap();

        expect(a.equals(b)).toBe(false);
    });

    it('is false when the counts differ', () => {
        const a = ImageCollection.of(['https://cdn.example.com/a.jpg'])._unsafeUnwrap();
        const b = ImageCollection.of(['https://cdn.example.com/a.jpg', 'https://cdn.example.com/b.jpg'])._unsafeUnwrap();

        expect(a.equals(b)).toBe(false);
    });
});
