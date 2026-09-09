import type {SupplierId} from './supplier-id';

/**
 * Input shape for {@link RegionalSupplier.create}. A freshly created supplier
 * has no region assignments yet — they are added via `assignRegion`.
 */
export interface CreateRegionalSupplierProps {
    readonly supplierId: SupplierId;
    readonly minimumOrderQuantity: number;
}
