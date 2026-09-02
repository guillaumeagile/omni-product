import type {DomainEvent} from '../../../shared/events/domain-event';

export class StockDepleted implements DomainEvent {
    public readonly name = 'inventory.stock-depleted';

    constructor(public readonly productId: string) {
    }
}
