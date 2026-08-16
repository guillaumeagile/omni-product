import {BadRequestException, Body, Controller, Get, Inject, Param, Patch, Post, Query} from '@nestjs/common';
import {ZodError} from 'zod';
import {CreateProductSchema} from './create-product.schema';
import {PRODUCT_CREATOR, ProductCreator} from './product-creator';
import {PRODUCT_READER, ProductReader} from './product-reader';
import {RESELLER_PRICE_CALCULATOR, ResellerPriceCalculator} from './reseller-price-calculator';
import {STOCK_RESERVER, StockReserver} from './stock-reserver';

@Controller('products')
export class ProductsController {
  constructor(
      @Inject(PRODUCT_CREATOR) private readonly productCreator: ProductCreator,
      @Inject(PRODUCT_READER) private readonly productReader: ProductReader,
      @Inject(STOCK_RESERVER) private readonly stockReserver: StockReserver,
      @Inject(RESELLER_PRICE_CALCULATOR) private readonly resellerPriceCalculator: ResellerPriceCalculator,
  ) {
  }

  @Post()
  async create(@Body() body: unknown) {
    // Naive boundary validation: parse, and turn a failed parse into a 400
    // the default way. Shape is checked here; nothing downstream knows or
    // cares whether the numbers inside make business sense.
    let input;
    try {
      input = CreateProductSchema.parse(body);
    } catch (err) {
      if (err instanceof ZodError) {
        throw new BadRequestException(err.issues);
      }
      throw err;
    }
    return this.productCreator.create(input);
  }

  @Get()
  async findAll(@Query('region') region?: string) {
    return this.productReader.findAll(region);
  }

  @Get(':id')
  async findOne(@Param('id') id: string) {
    return this.productReader.findOne(id);
  }

  @Get(':id/reseller-price')
  async resellerPrice(@Param('id') id: string, @Query('region') region: string) {
    return {resellerPrice: await this.resellerPriceCalculator.calculateResellerPrice(id, region)};
  }

  @Patch(':id/reserve')
  async reserve(@Param('id') id: string, @Body('quantity') quantity: number) {
    return this.stockReserver.reserveStock(id, quantity);
  }
}
