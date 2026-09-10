import {Product} from '@prisma/client';

export const STOCK_RESERVER = Symbol('STOCK_RESERVER');

export interface StockReserver {
    reserveStock(id: string, quantity: number): Promise<Product>;
}
