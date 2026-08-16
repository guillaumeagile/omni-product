import {Module} from '@nestjs/common';
import {ProductsController} from './products.controller';
import {ProductService} from './product.service';
import {PrismaService} from '../prisma.service';
import {PRODUCT_CREATOR} from './product-creator';
import {PRODUCT_READER} from './product-reader';
import {STOCK_RESERVER} from './stock-reserver';
import {RESELLER_PRICE_CALCULATOR} from './reseller-price-calculator';

@Module({
  controllers: [ProductsController],
  providers: [
    PrismaService,
    ProductService,
    {provide: PRODUCT_CREATOR, useExisting: ProductService},
    {provide: PRODUCT_READER, useExisting: ProductService},
    {provide: STOCK_RESERVER, useExisting: ProductService},
    {provide: RESELLER_PRICE_CALCULATOR, useExisting: ProductService},
  ],
  exports: [
    ProductService,
    PRODUCT_CREATOR,
    PRODUCT_READER,
    STOCK_RESERVER,
    RESELLER_PRICE_CALCULATOR,
  ],
})
export class ProductsModule {}
