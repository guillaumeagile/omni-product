import { Test } from '@nestjs/testing';
import { BadRequestException, NotFoundException } from '@nestjs/common';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProductService } from './product.service';
import { PrismaService } from '../prisma.service';

describe('ProductService', () => {
  let service: ProductService;
  let prisma: {
    product: {
      create: ReturnType<typeof vi.fn>;
      findMany: ReturnType<typeof vi.fn>;
      findUnique: ReturnType<typeof vi.fn>;
      update: ReturnType<typeof vi.fn>;
    };
  };

  beforeEach(async () => {
    prisma = {
      product: {
        create: vi.fn(),
        findMany: vi.fn(),
        findUnique: vi.fn(),
        update: vi.fn(),
      },
    };

    const moduleRef = await Test.createTestingModule({
      providers: [ProductService, { provide: PrismaService, useValue: prisma }],
    }).compile();

    service = moduleRef.get(ProductService);
  });

  it('rejects products with no name', async () => {
    await expect(
      service.create({
        name: '',
        slug: 'blender',
        priceBase: 20,
        priceTax: 4,
        priceTaxRate: 0.2,
        quantity: 10,
        stock: 10,
      }),
    ).rejects.toBeInstanceOf(BadRequestException);
  });

  it('creates a product when input is valid', async () => {
    prisma.product.create.mockResolvedValue({ id: '1', name: 'Blender' });

    const result = await service.create({
      name: 'Blender',
      slug: 'blender',
      priceBase: 20,
      priceTax: 4,
      priceTaxRate: 0.2,
      quantity: 10,
      stock: 10,
    });

    expect(prisma.product.create).toHaveBeenCalled();
    expect(result).toEqual({ id: '1', name: 'Blender' });
  });

  it('throws NotFoundException when the product does not exist', async () => {
    prisma.product.findUnique.mockResolvedValue(null);

    await expect(service.findOne('missing')).rejects.toBeInstanceOf(NotFoundException);
  });

  it('throws when reserving more than available stock', async () => {
    prisma.product.findUnique.mockResolvedValue({ id: '1', stock: 2 });

    await expect(service.reserveStock('1', 5)).rejects.toBeInstanceOf(BadRequestException);
  });

  it('reserves stock by decrementing the quantity', async () => {
    prisma.product.findUnique.mockResolvedValue({ id: '1', stock: 6 });
    prisma.product.update.mockResolvedValue({ id: '1', stock: 2 });

    const result = await service.reserveStock('1', 4);

    expect(prisma.product.update).toHaveBeenCalledWith({
      where: { id: '1' },
      data: { stock: 2 },
    });
    expect(result).toEqual({ id: '1', stock: 2 });
  });

  it('calculates a reseller price using the regional margin and VAT-on-margin', async () => {
    prisma.product.findUnique.mockResolvedValue({
      id: '1',
      priceBase: 100,
      priceTax: 10,
      priceTaxRate: 0.2,
    });

    const price = await service.calculateResellerPrice('1', 'EU');

    // base 100 + margin(18) + tax 10 + vatOnMargin(18 * 0.2 = 3.6) = 131.6
    expect(price).toBeCloseTo(131.6, 5);
  });

  it('falls back to the default margin for an unknown region', async () => {
    prisma.product.findUnique.mockResolvedValue({
      id: '1',
      priceBase: 100,
      priceTax: 0,
      priceTaxRate: 0,
    });

    const price = await service.calculateResellerPrice('1', 'MARS');

    // base 100 + default margin(20) + tax 0 + vatOnMargin(0) = 120
    expect(price).toBeCloseTo(120, 5);
  });
});
