import {err, ok, type Result} from 'neverthrow';
import {CATALOG_ITEM_STATUS, type CatalogItemStatus} from './catalog-item-status';
import {CATALOG_ITEM_ERROR_KIND, type CatalogItemError} from './catalog-item-error';
import type {CreateCatalogItemProps} from './create-catalog-item-props';
import type {Slug} from './slug';
import type {ProductTitle} from './product-title';
import type {ImageCollection} from './image-collection';

/**
 * Catalog aggregate for a product's storefront presentation.
 *
 * Always valid: the private constructor means every `CatalogItem` carries a
 * valid {@link Slug} and {@link ProductTitle}, and the publish rule — a
 * `PUBLISHED` item always has at least one image — is enforced by `publish`
 * rather than trusted to the caller.
 *
 * Immutable: `publish` / `updateSlug` / `archive` return a new `CatalogItem`,
 * or an `err` describing why the transition is not allowed, and never mutate
 * the instance they are called on.
 */
export class CatalogItem {
    private constructor(
        readonly productId: string,
        readonly title: ProductTitle,
        readonly slug: Slug,
        readonly images: ImageCollection | undefined,
        readonly status: CatalogItemStatus,
    ) {}

    static create(props: CreateCatalogItemProps): Result<CatalogItem, CatalogItemError> {
        if (props.productId.trim().length === 0) {
            return err({kind: CATALOG_ITEM_ERROR_KIND.ProductIdEmpty});
        }

        return ok(
            new CatalogItem(
                props.productId,
                props.title,
                props.slug,
                props.images,
                CATALOG_ITEM_STATUS.Draft,
            ),
        );
    }

    /**
     * Publishes the item to the storefront. Requires at least one image and
     * rejects a re-publish or a publish of an archived item.
     */
    publish(): Result<CatalogItem, CatalogItemError> {
        if (this.status === CATALOG_ITEM_STATUS.Published) {
            return err({kind: CATALOG_ITEM_ERROR_KIND.AlreadyPublished});
        }
        if (this.status === CATALOG_ITEM_STATUS.Archived) {
            return err({kind: CATALOG_ITEM_ERROR_KIND.PublishArchived});
        }
        // An ImageCollection cannot be empty — its factory rejects that — so a
        // present `images` already means "at least one image".
        if (this.images === undefined) {
            return err({kind: CATALOG_ITEM_ERROR_KIND.PublishWithoutImages});
        }

        return ok(this.withStatus(CATALOG_ITEM_STATUS.Published));
    }

    /**
     * Replaces the slug. Allowed while `DRAFT` or `PUBLISHED`; an archived item
     * is frozen and its slug can no longer change.
     */
    updateSlug(newSlug: Slug): Result<CatalogItem, CatalogItemError> {
        if (this.status === CATALOG_ITEM_STATUS.Archived) {
            return err({kind: CATALOG_ITEM_ERROR_KIND.SlugChangeOnArchived, status: this.status});
        }

        return ok(
            new CatalogItem(this.productId, this.title, newSlug, this.images, this.status),
        );
    }

    /** Archives the item, removing it from the storefront. Idempotency is not assumed: archiving twice is an error. */
    archive(): Result<CatalogItem, CatalogItemError> {
        if (this.status === CATALOG_ITEM_STATUS.Archived) {
            return err({kind: CATALOG_ITEM_ERROR_KIND.AlreadyArchived});
        }

        return ok(this.withStatus(CATALOG_ITEM_STATUS.Archived));
    }

    private withStatus(status: CatalogItemStatus): CatalogItem {
        return new CatalogItem(this.productId, this.title, this.slug, this.images, status);
    }
}
