package com.omniproduct.service;

import com.omniproduct.model.Amount;
import com.omniproduct.model.NonExpiring;
import com.omniproduct.model.Perishable;
import com.omniproduct.model.Product;
import com.omniproduct.model.ProductId;
import com.omniproduct.model.TaxRate;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Filet de non-régression du refactoring de catalogLabels (exercice HA14) : il décrit le comportement, pas la forme.
// Toute construction du modèle passe par les trois fabriques en bas du fichier : si le modèle change (ADT, records),
// elles seules s'adaptent, les assertions ne bougent pas.
class CatalogServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final String PASSED = " [BEST BEFORE PASSED]";

    private final CatalogService service = new CatalogService();

    @Test
    void anEmptyCatalogHasNoLabel() {
        assertThat(service.catalogLabels(List.of(), TODAY)).isEmpty();
    }

    @Test
    void aNonExpiringProductBeforeItsBestBeforeDateKeepsItsName() {
        assertThat(service.catalogLabels(List.of(nonExpiring("Rice", TOMORROW)), TODAY)).containsExactly("Rice");
    }

    @Test
    void aNonExpiringProductOnItsBestBeforeDateKeepsItsName() {
        assertThat(service.catalogLabels(List.of(nonExpiring("Rice", TODAY)), TODAY)).containsExactly("Rice");
    }

    @Test
    void aNonExpiringProductAfterItsBestBeforeDateIsStillListedWithAWarning() {
        assertThat(service.catalogLabels(List.of(nonExpiring("Rice", YESTERDAY)), TODAY)).containsExactly("Rice" + PASSED);
    }

    @Test
    void aPerishableProductBeforeItsSellByDateIsListedWithItsName() {
        assertThat(service.catalogLabels(List.of(perishable("Milk", TOMORROW)), TODAY)).containsExactly("Milk");
    }

    @Test
    void aPerishableProductOnItsSellByDateIsStillListed() {
        assertThat(service.catalogLabels(List.of(perishable("Milk", TODAY)), TODAY)).containsExactly("Milk");
    }

    @Test
    void aPerishableProductAfterItsSellByDateIsLeftOutOfTheCatalog() {
        assertThat(service.catalogLabels(List.of(perishable("Milk", YESTERDAY)), TODAY)).isEmpty();
    }

    @Test
    void aProductWithoutShelfLifeKeepsItsName() {
        assertThat(service.catalogLabels(List.of(plain("Gift card")), TODAY)).containsExactly("Gift card");
    }

    @Test
    void labelsKeepTheOrderOfTheProductsAndSkipOnlyTheExpiredPerishables() {
        List<Product> products = List.of(
                nonExpiring("Rice", YESTERDAY),
                perishable("Milk", YESTERDAY),
                plain("Gift card"),
                perishable("Yogurt", TOMORROW),
                nonExpiring("Pasta", TOMORROW));

        assertThat(service.catalogLabels(products, TODAY))
                .containsExactly("Rice" + PASSED, "Gift card", "Yogurt", "Pasta");
    }

    @Test
    void theVerdictDependsOnTodayNotOnTheClock() {
        List<Product> products = List.of(perishable("Milk", TODAY), nonExpiring("Rice", TODAY));

        assertThat(service.catalogLabels(products, TODAY)).containsExactly("Milk", "Rice");
        assertThat(service.catalogLabels(products, TOMORROW)).containsExactly("Rice" + PASSED);
    }

    @Test
    void theProductsGivenAreLeftUntouched() {
        List<Product> products = List.of(perishable("Milk", YESTERDAY), nonExpiring("Rice", YESTERDAY));

        service.catalogLabels(products, TODAY);

        assertThat(products).extracting(Product::getName).containsExactly("Milk", "Rice");
    }

    // --- Seules ces fabriques connaissent la forme du modèle. ---

    private static Product plain(String name) {
        return new Product(id(), name, price());
    }

    private static Product perishable(String name, LocalDate sellBy) {
        return new Perishable(id(), name, price(), sellBy, sellBy.plusDays(3));
    }

    private static Product nonExpiring(String name, LocalDate bestBefore) {
        return new NonExpiring(id(), name, price(), bestBefore);
    }

    private static ProductId id() {
        return new ProductId(UUID.randomUUID());
    }

    private static Product.Price price() {
        return new Product.Price(new Amount(10.0), new Amount(2.0), new TaxRate(0.2));
    }
}
