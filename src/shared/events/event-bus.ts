import type {Result} from 'neverthrow';
import type {DomainEvent} from './domain-event';
import type {EventBusError} from './event-bus-error';

export type EventHandler<E extends DomainEvent = DomainEvent> = (event: E) => void;

/**
 * The port a Bounded Context publishes facts through. Domain code never
 * imports an implementation of this — only application services do,
 * keeping the domain framework- and infrastructure-free.
 *
 * Any transport can implement it: `InMemoryEventBus` for tests and this
 * workshop, or a production adapter (RabbitMQ, Kafka, SQS, an outbox
 * relay...) without either side of a Bounded Context boundary changing.
 */
export interface EventBus {
    /**
     * Registers `handler` to run for every future event named `eventName`.
     * A production adapter may subscribe lazily (e.g. on connect) but the
     * call itself is synchronous and never fails.
     */
    subscribe<E extends DomainEvent>(eventName: string, handler: EventHandler<E>): void;

    /**
     * Publishes `events` to their subscribers. `publish` only ever reports
     * `Err` for a transport failure (broker unreachable, write rejected) —
     * never for a subscriber's handler throwing. A subscriber's failure is
     * that subscriber's problem: it must not roll back or block the
     * publisher, which has already committed the fact as true.
     */
    publish(events: readonly DomainEvent[]): Result<void, EventBusError>;
}
