import {describe, expect, it} from 'vitest';
import {Price} from './price';

describe('Price.create', () => {
    it('returns Ok for a non-negative amount', () => {
        const result = Price.create(19.99);

        expect(result.isOk()).toBe(true);
    });
});
