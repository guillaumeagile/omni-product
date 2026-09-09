import {err, ok, type Result} from 'neverthrow';
import {SUPPLIER_ID_ERROR_KIND, type SupplierIdError} from './supplier-id-error';

/**
 * A supplier's identity, always valid: non-empty once trimmed. The private
 * constructor means no `SupplierId` can exist without having passed `create`.
 */
export class SupplierId {
    private constructor(readonly value: string) {}

    static create(raw: string): Result<SupplierId, SupplierIdError> {
        const value = raw.trim();

        if (value.length === 0) {
            return err({kind: SUPPLIER_ID_ERROR_KIND.Empty});
        }

        return ok(new SupplierId(value));
    }

    equals(other: SupplierId): boolean {
        return this.value === other.value;
    }
}
