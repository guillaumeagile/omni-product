import { SupplierRegionInfo } from './supplier-region-info';

export interface CreateProductInput {
  name: string;
  slug: string;
  priceBase: number;
  priceTax: number;
  priceTaxRate: number;
  discounts?: string[];
  images?: Record<string, string>;
  suppliersRegions?: Record<string, SupplierRegionInfo>;
  kilos?: number;
  volume?: string;
  quantity: number;
  stock: number;
  warehouseLocation?: string;
  supplierId?: string;
}
