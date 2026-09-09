import {describe, it} from 'vitest';
import * as fc from 'fast-check';
import {Slug} from './slug';

// Generates strings that ARE valid slugs by construction: lowercase-alnum
// segments joined by single hyphens, total length >= 3.
const validSlug = fc
    .array(
        fc.stringMatching(/^[a-z0-9]+$/).filter(s => s.length > 0 && s.length <= 12),
        {minLength: 1, maxLength: 5},
    )
    .map(segments => segments.join('-'))
    .filter(s => s.length >= 3);

describe('Slug properties', () => {
    it('property: every generated valid slug is accepted and round-trips its value', () => {
        fc.assert(
            fc.property(validSlug, raw => {
                const result = Slug.create(raw);

                return result.isOk() && result.value.value === raw;
            }),
        );
    });

    it('property: an accepted slug always matches the documented format', () => {
        fc.assert(
            fc.property(validSlug, raw => {
                const result = Slug.create(raw);

                return result.isOk() && /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(result.value.value);
            }),
        );
    });

    it('property: any string containing an uppercase letter is rejected', () => {
        fc.assert(
            fc.property(
                fc.string({minLength: 3, maxLength: 30}).filter(s => /[A-Z]/.test(s)),
                raw => Slug.create(raw).isErr(),
            ),
        );
    });

    it('property: any trimmed string shorter than 3 characters is rejected', () => {
        fc.assert(
            fc.property(
                fc.string({maxLength: 2}),
                raw => Slug.create(raw).isErr(),
            ),
        );
    });

    it('property: create is idempotent — feeding an accepted slug back in yields the same value', () => {
        fc.assert(
            fc.property(validSlug, raw => {
                const once = Slug.create(raw);
                if (once.isErr()) {
                    return false;
                }
                const twice = Slug.create(once.value.value);

                return twice.isOk() && twice.value.value === once.value.value;
            }),
        );
    });
});
