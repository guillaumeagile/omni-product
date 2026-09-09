import type {Slug} from './slug';
import type {ProductTitle} from './product-title';
import type {ImageCollection} from './image-collection';

/**
 * Input shape for {@link CatalogItem.create}. A freshly created item is always
 * `DRAFT`; images are optional at this point and only become mandatory when the
 * item is published.
 */
export interface CreateCatalogItemProps {
    readonly productId: string;
    readonly title: ProductTitle;
    readonly slug: Slug;
    readonly images?: ImageCollection;
}
