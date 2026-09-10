export const EVENT_BUS_ERROR_KIND = {
    PublishFailed: 'EventBusPublishFailed',
} as const;

/**
 * A transport-level failure to publish — the broker/queue itself is
 * unreachable or rejected the write. This is distinct from a subscriber's
 * handler throwing: a handler failure is that subscriber's problem, never
 * the publisher's (see `EventBus.publish` doc).
 */
export type EventBusError =
    | { readonly kind: typeof EVENT_BUS_ERROR_KIND.PublishFailed; readonly eventName: string; readonly cause: unknown };
