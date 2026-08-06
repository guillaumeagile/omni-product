-- CreateTable
CREATE TABLE "Product" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "slug" TEXT NOT NULL,
    "priceBase" DOUBLE PRECISION NOT NULL,
    "priceTax" DOUBLE PRECISION NOT NULL,
    "priceTaxRate" DOUBLE PRECISION NOT NULL,
    "discounts" TEXT[],
    "images" JSONB NOT NULL,
    "suppliersRegions" JSONB NOT NULL,
    "kilos" DOUBLE PRECISION,
    "volume" TEXT,
    "quantity" INTEGER NOT NULL,
    "stock" INTEGER NOT NULL,
    "warehouseLocation" TEXT,
    "supplierId" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Product_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Supplier" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "contactEmail" TEXT NOT NULL,
    "contactPhone" TEXT NOT NULL,
    "country" TEXT NOT NULL,
    "region" TEXT NOT NULL,

    CONSTRAINT "Supplier_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "Product_slug_key" ON "Product"("slug");

-- AddForeignKey
ALTER TABLE "Product" ADD CONSTRAINT "Product_supplierId_fkey" FOREIGN KEY ("supplierId") REFERENCES "Supplier"("id") ON DELETE SET NULL ON UPDATE CASCADE;
