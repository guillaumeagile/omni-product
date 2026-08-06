import { INestApplication } from '@nestjs/common';
import { Test } from '@nestjs/testing';
import request from 'supertest';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { AppModule } from '../src/app.module';

// Deliberately minimal: e2e tests here hit a real Postgres instance (see
// docker-compose.yml) with no reset between runs, so state leaks between
// tests and across runs. That's the point for this workshop — attendees
// will feel the slowness/flakiness firsthand and see why unit tests around
// an isolated domain (once extracted) are cheaper and more reliable.
describe('Products (e2e)', () => {
  let app: INestApplication;

  beforeAll(async () => {
    const moduleRef = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleRef.createNestApplication();
    await app.init();
  });

  afterAll(async () => {
    await app.close();
  });

  it('rejects invalid product payloads', () => {
    return request(app.getHttpServer())
      .post('/products')
      .send({ name: '', slug: '', priceBase: -1, priceTax: 0, priceTaxRate: 0, quantity: -1, stock: -1 })
      .expect(400);
  });

  it('creates a product then reads it back by id', async () => {
    // Uses a timestamped slug to dodge the unique constraint across repeated
    // runs against the same database — a symptom of no test-data isolation.
    const slug = `blender-${Date.now()}`;

    const created = await request(app.getHttpServer())
      .post('/products')
      .send({
        name: 'Blender',
        slug,
        priceBase: 20,
        priceTax: 4,
        priceTaxRate: 0.2,
        quantity: 10,
        stock: 10,
      })
      .expect(201);

    await request(app.getHttpServer())
      .get(`/products/${created.body.id}`)
      .expect(200)
      .expect((res) => {
        expect(res.body.slug).toBe(slug);
      });
  });
});
