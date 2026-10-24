package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProductHasAdtPropertyRuleTest {

    private static List<Violation> check(String product) {
        return new ProductHasAdtPropertyRule(pkg -> pkg.contains(".model"))
                .check(SourceSet.ofSources(Map.of(
                        "Shelf.java", SHELF,
                        "Product.java", product), null));
    }

    private static final String SHELF = """
            package demo.model;

            public sealed interface Shelf permits Perishable, NonExpiring {
            }

            record Perishable(String sellBy) implements Shelf {
            }

            record NonExpiring(String bestBefore) implements Shelf {
            }
            """;

    @Test
    void acceptsAProductComposedWithAnAdtProperty() {
        String product = """
                package demo.model;

                public class Product {
                    private final Shelf shelf;
                    public Product(Shelf shelf) { this.shelf = shelf; }
                    public Shelf getShelf() { return shelf; }
                }
                """;

        assertThat(check(product)).isEmpty();
    }

    @Test
    void flagsAProductWithoutAnAdtPropertyOrWithoutProduct() {
        String product = """
                package demo.model;

                public class Product {
                    private String name;
                }
                """;

        assertThat(check(product)).anySatisfy(v -> assertThat(v.message()).contains("'Product' has no property typed by an ADT"));
        assertThat(check("package demo.model;\npublic class Other {}\n")).anySatisfy(v ->
                assertThat(v.message()).contains("No 'Product' type found"));
    }

    @Test
    void flagsASetterOnTheAdtProperty() {
        String product = """
                package demo.model;

                public class Product {
                    private Shelf shelf;
                    public void setShelf(Shelf shelf) { this.shelf = shelf; }
                }
                """;

        assertThat(check(product)).anySatisfy(v -> {
            assertThat(v.line()).isEqualTo(5);
            assertThat(v.message()).contains("'Product.shelf' has a setter");
        });
    }

    @Test
    void evasionsOfTheFieldNameAndShapeAreStillCaught() {
        String recordComponent = """
                package demo.model;

                public record Product(String name, Shelf conservation) {
                }
                """;
        String getterOnly = """
                package demo.model;

                public class Product {
                    private final Shelf storage;
                    public Shelf conservation() { return storage; }
                    public void changeStorageShelf(Shelf s) { }
                }
                """;

        assertThat(check(recordComponent)).isEmpty();
        assertThat(check(getterOnly)).anySatisfy(v -> assertThat(v.message()).contains("has a setter"));
    }

    @Test
    void flagsAnAlternativeThatIsNotAValueObject() {
        String shelfWithEntity = """
                package demo.model;

                public sealed interface Shelf permits Perishable, Bin {
                }

                record Perishable(String sellBy) implements Shelf {
                }

                record Bin(String id) implements Shelf, Identifiable {
                }
                """;
        String product = """
                package demo.model;

                public class Product {
                    private final Shelf shelf;
                }
                """;

        assertThat(new ProductHasAdtPropertyRule(pkg -> true)
                .check(SourceSet.ofSources(Map.of("Shelf.java", shelfWithEntity, "Product.java", product), null)))
                .anySatisfy(v -> assertThat(v.message()).contains("alternative 'Bin' of 'Shelf' is not a value object"));
    }
}
