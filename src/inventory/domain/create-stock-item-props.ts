/** Input shape for {@link StockItem.create}. */
export interface CreateStockItemProps {
    readonly productId: string;
    readonly availableQuantity: number;
    /** Defaults to 0 when the item has no outstanding reservations. */
    readonly reservedQuantity?: number;
}
