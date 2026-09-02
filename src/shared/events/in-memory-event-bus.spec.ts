import {describe, expect, it, vi} from 'vitest';
import type {DomainEvent} from './domain-event';
import {InMemoryEventBus} from './in-memory-event-bus';

const eventNamed = (name: string): DomainEvent => ({name});

describe('InMemoryEventBus.publish', () => {
    it('returns Ok when there are no subscribers', () => {
        const bus = new InMemoryEventBus();

        const result = bus.publish([eventNamed('inventory.stock-depleted')]);

        expect(result.isOk()).toBe(true);
    });

    it('reports nothing to the failure listener when there are no subscribers', () => {
        const onHandlerFailure = vi.fn();
        const bus = new InMemoryEventBus(onHandlerFailure);

        bus.publish([eventNamed('inventory.stock-depleted')]);

        expect(onHandlerFailure).not.toHaveBeenCalled();
    });

    it('calls a subscriber registered for the published event name', () => {
        const bus = new InMemoryEventBus();
        const handler = vi.fn();
        bus.subscribe('inventory.stock-depleted', handler);
        const event = eventNamed('inventory.stock-depleted');

        bus.publish([event]);

        expect(handler).toHaveBeenCalledTimes(1);
        expect(handler).toHaveBeenCalledWith(event);
    });

    it('does not call a subscriber registered for a different event name', () => {
        const bus = new InMemoryEventBus();
        const handler = vi.fn();
        bus.subscribe('inventory.stock-depleted', handler);

        bus.publish([eventNamed('catalog.item-archived')]);

        expect(handler).not.toHaveBeenCalled();
    });

    it('calls every subscriber registered for the same event name', () => {
        const bus = new InMemoryEventBus();
        const first = vi.fn();
        const second = vi.fn();
        bus.subscribe('inventory.stock-depleted', first);
        bus.subscribe('inventory.stock-depleted', second);

        bus.publish([eventNamed('inventory.stock-depleted')]);

        expect(first).toHaveBeenCalledTimes(1);
        expect(second).toHaveBeenCalledTimes(1);
    });

    it('dispatches each published event to its own subscribers, in publish order', () => {
        const bus = new InMemoryEventBus();
        const depletedHandler = vi.fn();
        const archivedHandler = vi.fn();
        bus.subscribe('inventory.stock-depleted', depletedHandler);
        bus.subscribe('catalog.item-archived', archivedHandler);
        const depleted = eventNamed('inventory.stock-depleted');
        const archived = eventNamed('catalog.item-archived');

        bus.publish([depleted, archived]);

        expect(depletedHandler).toHaveBeenCalledTimes(1);
        expect(depletedHandler).toHaveBeenCalledWith(depleted);
        expect(archivedHandler).toHaveBeenCalledTimes(1);
        expect(archivedHandler).toHaveBeenCalledWith(archived);
    });

    it('calls a handler once per publish, not once per subscribe', () => {
        const bus = new InMemoryEventBus();
        const handler = vi.fn();
        bus.subscribe('inventory.stock-depleted', handler);

        bus.publish([eventNamed('inventory.stock-depleted')]);
        bus.publish([eventNamed('inventory.stock-depleted')]);

        expect(handler).toHaveBeenCalledTimes(2);
    });

    it('still returns Ok when a subscriber throws', () => {
        const bus = new InMemoryEventBus();
        bus.subscribe('inventory.stock-depleted', () => {
            throw new Error('consumer exploded');
        });

        const result = bus.publish([eventNamed('inventory.stock-depleted')]);

        expect(result.isOk()).toBe(true);
    });

    it('still calls the remaining subscribers when an earlier one throws', () => {
        const bus = new InMemoryEventBus();
        const survivor = vi.fn();
        bus.subscribe('inventory.stock-depleted', () => {
            throw new Error('consumer exploded');
        });
        bus.subscribe('inventory.stock-depleted', survivor);

        bus.publish([eventNamed('inventory.stock-depleted')]);

        expect(survivor).toHaveBeenCalledTimes(1);
    });

    it('still dispatches a later event when an earlier event\'s handler throws', () => {
        const bus = new InMemoryEventBus();
        const secondHandler = vi.fn();
        bus.subscribe('inventory.stock-depleted', () => {
            throw new Error('consumer exploded');
        });
        bus.subscribe('catalog.item-archived', secondHandler);
        const archived = eventNamed('catalog.item-archived');

        bus.publish([eventNamed('inventory.stock-depleted'), archived]);

        expect(secondHandler).toHaveBeenCalledTimes(1);
        expect(secondHandler).toHaveBeenCalledWith(archived);
    });

    it('reports a thrown handler failure to the configured listener with the event name and the error', () => {
        const onHandlerFailure = vi.fn();
        const bus = new InMemoryEventBus(onHandlerFailure);
        const thrown = new Error('consumer exploded');
        bus.subscribe('inventory.stock-depleted', () => {
            throw thrown;
        });

        bus.publish([eventNamed('inventory.stock-depleted')]);

        expect(onHandlerFailure).toHaveBeenCalledTimes(1);
        expect(onHandlerFailure).toHaveBeenCalledWith('inventory.stock-depleted', thrown);
    });

    it('does not report anything to the failure listener when no subscriber throws', () => {
        const onHandlerFailure = vi.fn();
        const bus = new InMemoryEventBus(onHandlerFailure);
        bus.subscribe('inventory.stock-depleted', () => undefined);

        bus.publish([eventNamed('inventory.stock-depleted')]);

        expect(onHandlerFailure).not.toHaveBeenCalled();
    });

    it('defaults to a silent listener when none is provided', () => {
        const bus = new InMemoryEventBus();
        bus.subscribe('inventory.stock-depleted', () => {
            throw new Error('consumer exploded');
        });

        expect(() => bus.publish([eventNamed('inventory.stock-depleted')])).not.toThrow();
    });
});
