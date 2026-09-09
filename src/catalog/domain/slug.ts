import {err, ok, type Result} from 'neverthrow';
import {SLUG_ERROR_KIND, type SlugError} from './slug-error';

const MIN_LENGTH = 3;

// Lowercase alphanumeric words joined by single hyphens: no leading/trailing
// hyphen, no double hyphen, no uppercase, no spaces or punctuation.
const SLUG_PATTERN = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;

/**
 * A URL slug, always valid: lowercase, alphanumeric with single hyphens,
 * at least {@link MIN_LENGTH} characters. The private constructor means no
 * `Slug` can exist without having passed `create`.
 */
export class Slug {
    private constructor(readonly value: string) {}

    static create(raw: string): Result<Slug, SlugError> {
        const value = raw.trim();

        if (value.length < MIN_LENGTH) {
            return err({kind: SLUG_ERROR_KIND.TooShort, value, length: value.length});
        }

        if (!SLUG_PATTERN.test(value)) {
            return err({kind: SLUG_ERROR_KIND.InvalidFormat, value});
        }

        return ok(new Slug(value));
    }

    /** Value equality: two slugs with the same text are the same Slug. */
    equals(other: Slug): boolean {
        return this.value === other.value;
    }
}
