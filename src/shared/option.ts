export const OPTION_KIND = {
    Some: 'Some',
    None: 'None',
} as const;

export type Option<T> = { kind: typeof OPTION_KIND.Some; value: T } | { kind: typeof OPTION_KIND.None };

export const some = <T>(value: T): Option<T> => ({kind: OPTION_KIND.Some, value});

export const none = <T = never>(): Option<T> => ({kind: OPTION_KIND.None});

export const isSome = <T>(option: Option<T>): option is { kind: typeof OPTION_KIND.Some; value: T } =>
    option.kind === OPTION_KIND.Some;

export const isNone = <T>(option: Option<T>): option is { kind: typeof OPTION_KIND.None } =>
    option.kind === OPTION_KIND.None;

export const fromNullable = <T>(value: T | null | undefined): Option<NonNullable<T>> =>
    value == null ? none() : some(value as NonNullable<T>);

export const map = <T, TResult>(option: Option<T>, callback: (value: T) => TResult): Option<TResult> =>
    (isSome(option) ? some(callback(option.value)) : none());

export const asyncMap = async <T, TResult>(
    option: Option<T>,
    callback: (value: T) => Promise<TResult>,
): Promise<Option<TResult>> => (isSome(option) ? some(await callback(option.value)) : none());

export const andThen = <T, TResult>(option: Option<T>, callback: (value: T) => Option<TResult>): Option<TResult> =>
    (isSome(option) ? callback(option.value) : none());

export const asyncAndThen = async <T, TResult>(
    option: Option<T>,
    callback: (value: T) => Promise<Option<TResult>>,
): Promise<Option<TResult>> => (isSome(option) ? callback(option.value) : none());

export const orElse = <T>(option: Option<T>, callback: () => Option<T>): Option<T> =>
    (isSome(option) ? option : callback());

export const match = <T, TResult>(
    option: Option<T>,
    onSome: (value: T) => TResult,
    onNone: () => TResult,
): TResult => (isSome(option) ? onSome(option.value) : onNone());

export const unwrapOr = <T>(option: Option<T>, defaultValue: T): T =>
    (isSome(option) ? option.value : defaultValue);

export const andTee = <T>(option: Option<T>, callback: (value: T) => void): Option<T> => {
    if (isSome(option)) {
        callback(option.value);
    }

    return option;
};

export const orTee = <T>(option: Option<T>, callback: () => void): Option<T> => {
    if (isNone(option)) {
        callback();
    }

    return option;
};

export const andThrough = <T>(option: Option<T>, callback: (value: T) => Option<unknown>): Option<T> => {
    if (isNone(option)) {
        return option;
    }

    return isSome(callback(option.value)) ? option : none();
};

export const asyncAndThrough = async <T>(
    option: Option<T>,
    callback: (value: T) => Promise<Option<unknown>>,
): Promise<Option<T>> => {
    if (isNone(option)) {
        return option;
    }

    return isSome(await callback(option.value)) ? option : none();
};
