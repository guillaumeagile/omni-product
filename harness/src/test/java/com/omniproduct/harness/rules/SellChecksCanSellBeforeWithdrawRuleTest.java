package com.omniproduct.harness.rules;

import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SellChecksCanSellBeforeWithdrawRuleTest {

    private static List<Violation> check(String service) {
        return new SellChecksCanSellBeforeWithdrawRule(pkg -> pkg.contains(".service"))
                .check(SourceSet.ofSources(Map.of("SellService.java", service), null));
    }

    @Test
    void flagsAWithdrawWithoutAPrecedingCanSellWithItsLine() {
        String service = """
                package demo.service;

                class SellService {
                    void sellProduct(Product product) {
                        stock.withdraw(product);
                        if (product.canSell(today)) { }
                    }
                }
                """;

        assertThat(check(service)).anySatisfy(v -> {
            assertThat(v.line()).isEqualTo(5);
            assertThat(v.message()).contains("withdraws stock before checking canSell");
        });
    }

    @Test
    void acceptsCanSellCalledBeforeWithdrawWhateverItsWordingIs() {
        String service = """
                package demo.service;

                class SellService {
                    void sell(Product product) {
                        checkCanSell(product);
                        product.withdrawStock(1);
                    }
                }
                """;

        assertThat(check(service)).isEmpty();
    }

    @Test
    void ignoresMethodsThatDoNotSell() {
        String service = """
                package demo.service;

                class StockService {
                    void restock(Product product) {
                        product.withdraw(1);
                    }
                }
                """;

        assertThat(check(service)).isEmpty();
    }
}
