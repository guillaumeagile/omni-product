import {ok, type Result} from 'neverthrow';
import type {DomainEvent} from './domain-event';
import type {EventBus, EventHandler} from './event-bus';
import type {EventBusError} from './event-bus-error';

/** Reports a handler that threw while reacting to an event. Defaults to a no-op: this bus never lets a subscriber's failure surface to the publisher (see `EventBus.publish`), so a caller who wants visibility must opt in explicitly. */
export type HandlerFailureListener = (eventName: string, error: unknown) => void;

const noopListener: HandlerFailureListener = () => undefined;

/**
 * Synchronous, in-process `EventBus`. Publishing never fails at the
 * transport level — there is no transport — so `publish` always returns
 * `Ok`. Built for tests and this workshop: deterministic, no I/O, every
 * handler runs to completion before `publish` returns.
 */
export class InMemoryEventBus implements EventBus {
    private readonly handlersByEventName = new Map<string, EventHandler[]>();

    constructor(private readonly onHandlerFailure: HandlerFailureListener = noopListener) {
    }

    subscribe<E extends DomainEvent>(eventName: string, handler: EventHandler<E>): void {
        const existing = this.handlersByEventName.get(eventName) ?? [];

        this.handlersByEventName.set(eventName, [...existing, handler as EventHandler]);
    }

    publish(events: readonly DomainEvent[]): Result<void, EventBusError> {
        for (const event of events) {
            this.dispatch(event);
        }

        return ok(undefined);
    }

    private dispatch(event: DomainEvent): void {
        const handlers = this.handlersByEventName.get(event.name) ?? [];

        for (const handler of handlers) {
            this.runHandler(handler, event);
        }
    }

    private runHandler(handler: EventHandler, event: DomainEvent): void {
        try {
            handler(event);
        } catch (error) {
            this.onHandlerFailure(event.name, error);
        }
    }
}
