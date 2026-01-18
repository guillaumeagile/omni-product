import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order Record Patterns - Java 25")
class OrderRecordPatternTest {

    record Order(String id, Customer customer, Item item) {}

    record Customer(String name, String email) {}

    record Item(String productName, double price, int quantity) {}

    record ProcessedOrder(
            String orderId,
            String customerName,
            String productName,
            int quantity,
            double unitPrice,
            double totalPrice,
            boolean discountApplied
    ) {}

    @Test
    @DisplayName("should process orders with complex nested record patterns and apply discount for bulk orders")
    void testOrderProcessing() {
        Order singleItemOrder = new Order(
                "ORD-001",
                new Customer("John", "john@example.com"),
                new Item("Laptop", 999.99, 1)
        );

        Order bulkOrder = new Order(
                "ORD-002",
                new Customer("Jane", "jane@example.com"),
                new Item("Mouse", 29.99, 5)
        );

        ProcessedOrder processed1 = processOrder(singleItemOrder);
        ProcessedOrder processed2 = processOrder(bulkOrder);

        assertThat(processed1.orderId()).isEqualTo("ORD-001");
        assertThat(processed1.customerName()).isEqualTo("John");
        assertThat(processed1.productName()).isEqualTo("Laptop");
        assertThat(processed1.quantity()).isEqualTo(1);
        assertThat(processed1.totalPrice()).isCloseTo(999.99, org.assertj.core.data.Offset.offset(0.01));
        assertThat(processed1.discountApplied()).isFalse();

        assertThat(processed2.orderId()).isEqualTo("ORD-002");
        assertThat(processed2.customerName()).isEqualTo("Jane");
        assertThat(processed2.productName()).isEqualTo("Mouse");
        assertThat(processed2.quantity()).isEqualTo(5);
        assertThat(processed2.totalPrice()).isCloseTo(134.955, org.assertj.core.data.Offset.offset(0.01));
        assertThat(processed2.discountApplied()).isTrue();
    }

    private ProcessedOrder processOrder(Order order) {
        return switch (order) {
            case Order(String id, Customer(String name, _), Item(String product, double price, int qty)) when qty > 2 -> {
                double totalPrice = price * qty * 0.9;
                yield new ProcessedOrder(id, name, product, qty, price, totalPrice, true);
            }
            case Order(String id, Customer(String name, _), Item(String product, double price, int qty)) ->
                    new ProcessedOrder(id, name, product, qty, price, price * qty, false);
            default -> throw new IllegalArgumentException("Invalid order");
        };
    }

    @Test
    @DisplayName("should validate orders using pattern matching with guards")
    void testOrderValidation() {
        Order validOrder = new Order("ORD-001", new Customer("John", "john@example.com"), new Item("Product", 99.99, 1));
        Order invalidOrder = new Order("", new Customer("", ""), new Item("", 0, 0));

        assertThat(isValidOrder(validOrder)).isTrue();
        assertThat(isValidOrder(invalidOrder)).isFalse();
    }

    private boolean isValidOrder(Order order) {
        return switch (order) {
            case Order(String id, Customer(String name, String email), Item(String product, double price, int qty))
                    when !id.isBlank() && !name.isBlank() && email.contains("@") && !product.isBlank() && price > 0 && qty > 0 ->
                    true;
            default -> false;
        };
    }
}
