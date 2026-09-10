import {Product} from '@prisma/client';

export const PRODUCT_READER = Symbol('PRODUCT_READER');

export interface ProductReader {
    findAll(region?: string): Promise<Product[]>;

    findOne(id: string): Promise<Product>;
}
