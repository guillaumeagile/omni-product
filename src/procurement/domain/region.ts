import {err, ok, type Result} from 'neverthrow';
import {REGION_ERROR_KIND, type RegionError} from './region-error';

// The only regions the business procures from. A supplier can only be assigned
// to one of these — the legacy free-form `Record<string, unknown>` dictionary
// let any string through.
const SUPPORTED_REGIONS = ['EU', 'UK', 'US', 'APAC'] as const;

export type SupportedRegion = (typeof SUPPORTED_REGIONS)[number];

/**
 * A procurement region, always valid: it is one of {@link SUPPORTED_REGIONS}.
 * Input is upper-cased and trimmed before the check, so `" eu "` resolves to
 * `EU`.
 */
export class Region {
    private constructor(readonly code: SupportedRegion) {}

    static create(raw: string): Result<Region, RegionError> {
        const value = raw.trim().toUpperCase();

        if (!Region.isSupported(value)) {
            return err({kind: REGION_ERROR_KIND.Unsupported, value});
        }

        return ok(new Region(value));
    }

    static supported(): readonly SupportedRegion[] {
        return SUPPORTED_REGIONS;
    }

    equals(other: Region): boolean {
        return this.code === other.code;
    }

    private static isSupported(value: string): value is SupportedRegion {
        return (SUPPORTED_REGIONS as readonly string[]).includes(value);
    }
}
