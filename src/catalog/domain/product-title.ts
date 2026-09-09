import {err, ok, type Result} from 'neverthrow';
import {PRODUCT_TITLE_ERROR_KIND, type ProductTitleError} from './product-title-error';

const MAX_LENGTH = 200;

/**
 * A human-facing product title, always valid: non-empty once trimmed and no
 * longer than {@link MAX_LENGTH} characters. Internal whitespace is collapsed
 * so `"  Red   Bike  "` and `"Red Bike"` are the same title.
 */
export class ProductTitle {
    private constructor(readonly value: string) {}

    static create(raw: string): Result<ProductTitle, ProductTitleError> {
        const value = raw.trim().replace(/\s+/g, ' ');

        if (value.length === 0) {
            return err({kind: PRODUCT_TITLE_ERROR_KIND.Empty});
        }

        if (value.length > MAX_LENGTH) {
            return err({kind: PRODUCT_TITLE_ERROR_KIND.TooLong, length: value.length});
        }

        return ok(new ProductTitle(value));
    }

    equals(other: ProductTitle): boolean {
        return this.value === other.value;
    }
}
