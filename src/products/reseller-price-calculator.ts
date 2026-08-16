export const RESELLER_PRICE_CALCULATOR = Symbol('RESELLER_PRICE_CALCULATOR');

export interface ResellerPriceCalculator {
    calculateResellerPrice(id: string, region: string): Promise<number>;
}
