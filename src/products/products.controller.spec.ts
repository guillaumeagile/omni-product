import {Test} from '@nestjs/testing';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProductsController} from './products.controller';
import {PRODUCT_CREATOR} from './product-creator';
import {PRODUCT_READER} from './product-reader';
import {RESELLER_PRICE_CALCULATOR} from './reseller-price-calculator';
import {STOCK_RESERVER} from './stock-reserver';

describe('ProductsController', () => {
  let controller: ProductsController;
  let productCreator: { create: ReturnType<typeof vi.fn> };
  let productReader: { findAll: ReturnType<typeof vi.fn>; findOne: ReturnType<typeof vi.fn> };
  let stockReserver: { reserveStock: ReturnType<typeof vi.fn> };
  let resellerPriceCalculator: { calculateResellerPrice: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    productCreator = {create: vi.fn()};
    productReader = {findAll: vi.fn(), findOne: vi.fn()};
    stockReserver = {reserveStock: vi.fn()};
    resellerPriceCalculator = {calculateResellerPrice: vi.fn()};

    const moduleRef = await Test.createTestingModule({
      controllers: [ProductsController],
      providers: [
        {provide: PRODUCT_CREATOR, useValue: productCreator},
        {provide: PRODUCT_READER, useValue: productReader},
        {provide: STOCK_RESERVER, useValue: stockReserver},
        {provide: RESELLER_PRICE_CALCULATOR, useValue: resellerPriceCalculator},
      ],
    }).compile();

    controller = moduleRef.get(ProductsController);
  });

  it('delegates product creation to the creator interface', async () => {
    productCreator.create.mockResolvedValue({id: '1'});

    const result = await controller.create({
      name: 'Blender',
      slug: 'blender',
      priceBase: 20,
      priceTax: 4,
      priceTaxRate: 0.2,
      quantity: 10,
      stock: 10,
    });

    expect(productCreator.create).toHaveBeenCalled();
    expect(result).toEqual({ id: '1' });
  });

  it('delegates reservation to the reserver interface', async () => {
    stockReserver.reserveStock.mockResolvedValue({id: '1', stock: 2});

    const result = await controller.reserve('1', 4);

    expect(stockReserver.reserveStock).toHaveBeenCalledWith('1', 4);
    expect(result).toEqual({ id: '1', stock: 2 });
  });

  it('wraps the reseller price in a response object', async () => {
    resellerPriceCalculator.calculateResellerPrice.mockResolvedValue(131.6);

    const result = await controller.resellerPrice('1', 'EU');

    expect(resellerPriceCalculator.calculateResellerPrice).toHaveBeenCalledWith('1', 'EU');
    expect(result).toEqual({ resellerPrice: 131.6 });
  });
});
