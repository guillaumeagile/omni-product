import { BadRequestException, Body, Controller, Get, Param, Patch, Post, Query } from '@nestjs/common';
import { ZodError } from 'zod';
import { ProductService } from './product.service';
import { CreateProductSchema } from './create-product.schema';

@Controller('products')
export class ProductsController {
  constructor(private readonly productService: ProductService) {}

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
    return this.productService.create(input);
  }

  @Get()
  async findAll(@Query('region') region?: string) {
    return this.productService.findAll(region);
  }

  @Get(':id')
  async findOne(@Param('id') id: string) {
    return this.productService.findOne(id);
  }

  @Get(':id/reseller-price')
  async resellerPrice(@Param('id') id: string, @Query('region') region: string) {
    return { resellerPrice: await this.productService.calculateResellerPrice(id, region) };
  }

  @Patch(':id/reserve')
  async reserve(@Param('id') id: string, @Body('quantity') quantity: number) {
    return this.productService.reserveStock(id, quantity);
  }
}
