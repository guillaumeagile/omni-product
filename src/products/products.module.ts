import { Module } from '@nestjs/common';
import { ProductsController } from './products.controller';
import { ProductService } from './product.service';
import { PrismaService } from '../prisma.service';

@Module({
  controllers: [ProductsController],
  providers: [PrismaService, ProductService],
})
export class ProductsModule {}
