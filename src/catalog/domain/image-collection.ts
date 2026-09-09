import {err, ok, type Result} from 'neverthrow';
import {IMAGE_COLLECTION_ERROR_KIND, type ImageCollectionError} from './image-collection-error';

// http(s) URLs only. Replaces the legacy untyped `images` JSON blob, where any
// shape at all could be persisted against a product.
const IMAGE_URL_PATTERN = /^https?:\/\/[^\s]+$/;

/**
 * A non-empty, ordered, duplicate-free list of image URLs for a catalog item.
 * Always valid: `of` rejects an empty list, a malformed URL, or a repeated URL,
 * so a `PUBLISHED` product can never end up with zero images.
 */
export class ImageCollection {
    private constructor(readonly urls: readonly string[]) {}

    static of(rawUrls: readonly string[]): Result<ImageCollection, ImageCollectionError> {
        if (rawUrls.length === 0) {
            return err({kind: IMAGE_COLLECTION_ERROR_KIND.Empty});
        }

        const urls: string[] = [];

        for (const raw of rawUrls) {
            const value = raw.trim();

            if (!IMAGE_URL_PATTERN.test(value)) {
                return err({kind: IMAGE_COLLECTION_ERROR_KIND.InvalidImageUrl, value});
            }
            if (urls.includes(value)) {
                return err({kind: IMAGE_COLLECTION_ERROR_KIND.DuplicateImageUrl, value});
            }

            urls.push(value);
        }

        return ok(new ImageCollection(urls));
    }

    get count(): number {
        return this.urls.length;
    }

    equals(other: ImageCollection): boolean {
        return (
            this.urls.length === other.urls.length &&
            this.urls.every((url, index) => url === other.urls[index])
        );
    }
}
