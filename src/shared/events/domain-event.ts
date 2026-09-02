/**
 * A fact that already happened, stated by the Bounded Context that owns it.
 * `name` is the published contract other BCs subscribe to — changing it is a
 * breaking change for unknown consumers, the same way a public API is.
 */
export interface DomainEvent {
    readonly name: string;
}
