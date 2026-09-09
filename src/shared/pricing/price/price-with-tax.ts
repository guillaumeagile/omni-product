/** A price with tax applied, kept distinct from the net `Price` it was derived from. */
export class PriceWithTax {
    constructor(readonly amount: number) {}
}
