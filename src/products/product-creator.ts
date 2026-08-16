import {Product} from '@prisma/client';
import {CreateProductInput} from './create-product-input';

export const PRODUCT_CREATOR = Symbol('PRODUCT_CREATOR');

export interface ProductCreator {
    create(input: CreateProductInput): Promise<Product>;
}
