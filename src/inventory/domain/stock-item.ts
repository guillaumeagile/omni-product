import {ok, type Result} from 'neverthrow';
import type {DomainEvent} from '../../shared/events/domain-event';
import type {InsufficientStockError} from './insufficient-stock-error';

export class StockItem {

    private constructor(readonly productId: string, readonly availableQuantity: number) {
    }

    static create(props: { productId: string; availableQuantity: number }): Result<StockItem, never> {
        return ok(new StockItem(props.productId, props.availableQuantity));
    }

    reserve(_qty: number): Result<StockItem, InsufficientStockError> {
        throw new Error('StockItem#reserve not implemented');
    }

    pullDomainEvents(): DomainEvent[] {
        throw new Error('StockItem#pullDomainEvents not implemented');
    }
}
