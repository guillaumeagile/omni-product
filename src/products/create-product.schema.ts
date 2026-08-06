import { z } from 'zod';

// SMELL: validates SHAPE, not DOMAIN. A priceTaxRate of 900 is a valid
// non-negative number and a nonsensical business value. Zod is doing exactly
// what we asked — the design is still rotten. The fix is where we enforce
// invariants (VOs, aggregate factories), not the tool.
//
// One big schema mirroring the God `Product` model column-for-column,
// because that's what the God object already does — tangles catalog
// presentation, stock, supplier/pricing, and warehouse into one shape.
//
// TODO(refactor): shape validation → domain invariants (VO factories / aggregate constructors).
export const CreateProductSchema = z.object({
  name: z.string().min(1),
  slug: z.string().min(1),

  priceBase: z.number().nonnegative(),
  priceTax: z.number().nonnegative(),
  priceTaxRate: z.number().nonnegative(),

  discounts: z.array(z.string()).optional(),
  images: z.record(z.string(), z.string()).optional(),
  suppliersRegions: z
    .record(
      z.string(),
      z.object({
        name: z.string(),
        siren: z.string(),
        tvaId: z.string(),
      }),
    )
    .optional(),

  kilos: z.number().nonnegative().optional(),
  volume: z.string().optional(),
  quantity: z.number().int().nonnegative(),
  stock: z.number().int().nonnegative(),

  warehouseLocation: z.string().optional(),
  supplierId: z.string().optional(),
});
