import { Test } from '@nestjs/testing';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProductsController } from './products.controller';
import { ProductService } from './product.service';

describe('ProductsController', () => {
  let controller: ProductsController;
  let productService: {
    create: ReturnType<typeof vi.fn>;
    findAll: ReturnType<typeof vi.fn>;
    findOne: ReturnType<typeof vi.fn>;
    reserveStock: ReturnType<typeof vi.fn>;
    calculateResellerPrice: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    productService = {
      create: vi.fn(),
      findAll: vi.fn(),
      findOne: vi.fn(),
      reserveStock: vi.fn(),
      calculateResellerPrice: vi.fn(),
    };

    const moduleRef = await Test.createTestingModule({
      controllers: [ProductsController],
      providers: [{ provide: ProductService, useValue: productService }],
    }).compile();

    controller = moduleRef.get(ProductsController);
  });

  it('delegates product creation to the service', async () => {
    productService.create.mockResolvedValue({ id: '1' });

    const result = await controller.create({
      name: 'Blender',
      slug: 'blender',
      priceBase: 20,
      priceTax: 4,
      priceTaxRate: 0.2,
      quantity: 10,
      stock: 10,
    });

    expect(productService.create).toHaveBeenCalled();
    expect(result).toEqual({ id: '1' });
  });

  it('delegates reservation to the service', async () => {
    productService.reserveStock.mockResolvedValue({ id: '1', stock: 2 });

    const result = await controller.reserve('1', 4);

    expect(productService.reserveStock).toHaveBeenCalledWith('1', 4);
    expect(result).toEqual({ id: '1', stock: 2 });
  });

  it('wraps the reseller price in a response object', async () => {
    productService.calculateResellerPrice.mockResolvedValue(131.6);

    const result = await controller.resellerPrice('1', 'EU');

    expect(productService.calculateResellerPrice).toHaveBeenCalledWith('1', 'EU');
    expect(result).toEqual({ resellerPrice: 131.6 });
  });
});
