import {err, ok, type Result} from 'neverthrow';
import {STOCK_ITEM_ERROR_KIND, type StockItemError} from './stock-item-error';
import type {CreateStockItemProps} from './create-stock-item-props';

/**
 * Inventory aggregate for a single product's stock position.
 *
 * Always valid: the private constructor means no `StockItem` exists without
 * having passed `create`, and every invariant (`availableQuantity >= 0`,
 * `reservedQuantity >= 0`, both integers) holds by construction.
 *
 * Immutable: `reserve` / `release` / `restock` never mutate the instance they
 * are called on — they return a new `StockItem`, or an `err` describing why the
 * transition is not allowed. This is what makes the legacy race condition —
 * "read stock, subtract, write back" — unrepresentable here.
 */
export class StockItem {
    private constructor(
        readonly productId: string,
        readonly availableQuantity: number,
        readonly reservedQuantity: number,
    ) {}

    static create(props: CreateStockItemProps): Result<StockItem, StockItemError> {
        if (props.productId.trim().length === 0) {
            return err({kind: STOCK_ITEM_ERROR_KIND.ProductIdEmpty});
        }

        const available = props.availableQuantity;
        const reserved = props.reservedQuantity ?? 0;

        for (const quantity of [available, reserved]) {
            if (!Number.isInteger(quantity)) {
                return err({kind: STOCK_ITEM_ERROR_KIND.AvailableQuantityNotInteger, availableQuantity: quantity});
            }
            if (quantity < 0) {
                return err({kind: STOCK_ITEM_ERROR_KIND.AvailableQuantityNegative, availableQuantity: quantity});
            }
        }

        return ok(new StockItem(props.productId, available, reserved));
    }

    /**
     * Moves `quantity` units from available to reserved.
     * Fails when there is not enough available stock to cover the request.
     */
    reserve(quantity: number): Result<StockItem, StockItemError> {
        return StockItem.requirePositiveInteger(quantity).andThen(() => {
            if (quantity > this.availableQuantity) {
                return err({
                    kind: STOCK_ITEM_ERROR_KIND.InsufficientStock,
                    requested: quantity,
                    available: this.availableQuantity,
                });
            }

            return ok(
                new StockItem(
                    this.productId,
                    this.availableQuantity - quantity,
                    this.reservedQuantity + quantity,
                ),
            );
        });
    }

    /**
     * Moves `quantity` units back from reserved to available, e.g. when an
     * order is cancelled. Fails when more is released than is currently reserved.
     */
    release(quantity: number): Result<StockItem, StockItemError> {
        return StockItem.requirePositiveInteger(quantity).andThen(() => {
            if (quantity > this.reservedQuantity) {
                return err({
                    kind: STOCK_ITEM_ERROR_KIND.InsufficientReservation,
                    requested: quantity,
                    reserved: this.reservedQuantity,
                });
            }

            return ok(
                new StockItem(
                    this.productId,
                    this.availableQuantity + quantity,
                    this.reservedQuantity - quantity,
                ),
            );
        });
    }

    /** Adds `quantity` units of fresh stock to the available pool. */
    restock(quantity: number): Result<StockItem, StockItemError> {
        return StockItem.requirePositiveInteger(quantity).map(
            () =>
                new StockItem(
                    this.productId,
                    this.availableQuantity + quantity,
                    this.reservedQuantity,
                ),
        );
    }

    private static requirePositiveInteger(quantity: number): Result<number, StockItemError> {
        if (!Number.isInteger(quantity)) {
            return err({kind: STOCK_ITEM_ERROR_KIND.QuantityNotInteger, quantity});
        }
        if (quantity <= 0) {
            return err({kind: STOCK_ITEM_ERROR_KIND.QuantityNotPositive, quantity});
        }
        return ok(quantity);
    }
}
