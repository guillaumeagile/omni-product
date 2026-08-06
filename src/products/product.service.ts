import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { Prisma, Product } from '@prisma/client';
import { PrismaService } from '../prisma.service';
import { CreateProductInput } from './create-product-input';

// Regional margin applied on top of the supplier's base price before VAT.
// Pulled straight out of the legacy Spring Boot pricing job.
const REGIONAL_MARGIN: Record<string, number> = {
  EU: 0.18,
  UK: 0.22,
  US: 0.15,
  APAC: 0.25,
};

const DEFAULT_MARGIN = 0.2;

@Injectable()
export class ProductService {
  constructor(private readonly prisma: PrismaService) {}

  async create(input: CreateProductInput): Promise<Product> {
    if (!input.name || input.name.trim().length === 0) {
      throw new BadRequestException('name is required');
    }
    if (!input.slug || input.slug.trim().length === 0) {
      throw new BadRequestException('slug is required');
    }
    if (input.priceBase == null || input.priceBase <= 0) {
      throw new BadRequestException('priceBase must be a positive number');
    }
    if (input.stock == null || input.stock < 0) {
      throw new BadRequestException('stock cannot be negative');
    }

    return this.prisma.product.create({
      data: {
        name: input.name.trim(),
        slug: input.slug.trim(),
        priceBase: input.priceBase,
        priceTax: input.priceTax,
        priceTaxRate: input.priceTaxRate,
        discounts: input.discounts ?? [],
        images: (input.images ?? {}) as Prisma.InputJsonValue,
        suppliersRegions: (input.suppliersRegions ?? {}) as unknown as Prisma.InputJsonValue,
        kilos: input.kilos,
        volume: input.volume,
        quantity: input.quantity,
        stock: input.stock,
        warehouseLocation: input.warehouseLocation,
        supplierId: input.supplierId,
      },
    });
  }

  async findAll(region?: string): Promise<Product[]> {
    if (!region) {
      return this.prisma.product.findMany({ orderBy: { createdAt: 'desc' } });
    }
    const all = await this.prisma.product.findMany({ orderBy: { createdAt: 'desc' } });
    return all.filter((p) => Object.keys(p.suppliersRegions as object).includes(region));
  }

  async findOne(id: string): Promise<Product> {
    const product = await this.prisma.product.findUnique({ where: { id } });
    if (!product) {
      throw new NotFoundException(`product ${id} not found`);
    }
    return product;
  }

  async reserveStock(id: string, quantity: number): Promise<Product> {
    if (!quantity || quantity <= 0) {
      throw new BadRequestException('quantity must be a positive number');
    }

    const product = await this.findOne(id);

    if (product.stock < quantity) {
      throw new BadRequestException('not enough stock to reserve');
    }

    return this.prisma.product.update({
      where: { id },
      data: { stock: product.stock - quantity },
    });
  }

  // Mixes supplier base price, a regional margin, and VAT charged on top of
  // that margin into a single number. This kind of pricing rule is exactly
  // what belongs on a domain object instead of buried in a service method.
  async calculateResellerPrice(id: string, region: string): Promise<number> {
    const product = await this.findOne(id);

    const margin = REGIONAL_MARGIN[region] ?? DEFAULT_MARGIN;
    const marginAmount = product.priceBase * margin;
    const vatOnMargin = marginAmount * product.priceTaxRate;

    const resellerPrice = product.priceBase + marginAmount + product.priceTax + vatOnMargin;

    return Math.round(resellerPrice * 100) / 100;
  }
}
