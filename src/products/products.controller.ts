import { Body, Controller, Get, Param, Patch, Post, Query } from '@nestjs/common';
import { ProductService } from './product.service';
import { CreateProductInput } from './create-product-input';

@Controller('products')
export class ProductsController {
  constructor(private readonly productService: ProductService) {}

  @Post()
  async create(@Body() body: CreateProductInput) {
    return this.productService.create(body);
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
