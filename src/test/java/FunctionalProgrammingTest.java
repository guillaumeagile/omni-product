import io.vavr.collection.List;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Functional Programming in Java 25")
class FunctionalProgrammingTest {

    sealed interface Result<T> permits Success, Failure {}

    record Success<T>(T value) implements Result<T> {}

    record Failure<T>(String error) implements Result<T> {}

    sealed interface Payment permits CreditCard, PayPal, BankTransfer {}

    record CreditCard(String cardNumber, double amount) implements Payment {}

    record PayPal(String email, double amount) implements Payment {}

    record BankTransfer(String iban, double amount) implements Payment {}

    @Test
    @DisplayName("should process payments using sealed types and functional composition")
    void testFunctionalPaymentProcessing() {
        List<Payment> payments = List.of(
                new CreditCard("1234-5678-9012-3456", 100.0),
                new PayPal("user@example.com", 50.0),
                new BankTransfer("DE89370400440532013000", 200.0),
                new CreditCard("9876-5432-1098-7654", 75.0)
        );

        double totalAmount = payments
                .map(this::extractAmount)
                .fold(0.0, Double::sum);

        assertThat(totalAmount).isEqualTo(425.0);
    }

    private double extractAmount(Payment payment) {
        return switch (payment) {
            case CreditCard(_, double amount) -> amount;
            case PayPal(_, double amount) -> amount;
            case BankTransfer(_, double amount) -> amount;
        };
    }

    @Test
    @DisplayName("should validate and process payments with Option")
    void testPaymentValidationWithOption() {
        Payment payment = new CreditCard("1234-5678-9012-3456", 100.0);

        Option<String> result = validatePayment(payment)
                .flatMap(this::processPayment);

        assertThat(result.isDefined()).isTrue();
        assertThat(result.get()).contains("Payment processed: 100.0");
    }

    private Option<Payment> validatePayment(Payment payment) {
        return switch (payment) {
            case CreditCard(String card, double amount) when amount > 0 && card.length() == 19 ->
                    Option.of(payment);
            case PayPal(String email, double amount) when amount > 0 && email.contains("@") ->
                    Option.of(payment);
            case BankTransfer(String iban, double amount) when amount > 0 && iban.length() >= 15 ->
                    Option.of(payment);
            default -> Option.none();
        };
    }

    private Option<String> processPayment(Payment payment) {
        return Option.of(switch (payment) {
            case CreditCard(String card, double amount) ->
                    "Payment processed: " + amount + " via card ending in " + card.substring(15);
            case PayPal(String email, double amount) ->
                    "Payment processed: " + amount + " via PayPal (" + email + ")";
            case BankTransfer(String iban, double amount) ->
                    "Payment processed: " + amount + " via bank transfer";
        });
    }

    @Test
    @DisplayName("should handle errors with Try")
    void testErrorHandlingWithTry() {
        List<String> cardNumbers = List.of("1234-5678-9012-3456", "invalid", "9876-5432-1098-7654");

        List<String> validCards = cardNumbers
                .map(this::validateCardNumber)
                .filter(Try::isSuccess)
                .map(Try::get);

        assertThat(validCards).hasSize(2);
    }

    private Try<String> validateCardNumber(String card) {
        return Try.of(() -> {
            if (card.length() != 19 || !card.matches("\\d{4}-\\d{4}-\\d{4}-\\d{4}")) {
                throw new IllegalArgumentException("Invalid card format");
            }
            return card;
        });
    }

    @Test
    @DisplayName("should compose functions with sealed result types")
    void testFunctionalComposition() {
        List<Integer> numbers = List.of(2, 4);

        List<Result<Integer>> results = numbers
                .map(this::divideByTwo)
                .map(this::multiplyByThree);

        List<Integer> successValues = results
                .filter(r -> r instanceof Success)
                .map(r -> ((Success<Integer>) r).value());

        assertThat(successValues).isEqualTo(List.of(3, 6));
    }

    private Result<Integer> divideByTwo(Integer n) {
        return n % 2 == 0
                ? new Success<>(n / 2)
                : new Failure<>("Cannot divide " + n + " by 2");
    }

    private Result<Integer> multiplyByThree(Result<Integer> result) {
        return switch (result) {
            case Success<Integer>(Integer value) -> new Success<>(value * 3);
            case Failure<Integer>(String error) -> new Failure<>(error);
        };
    }

    @Test
    @DisplayName("should filter and transform with functional style")
    void testFunctionalFilterAndTransform() {
        List<Payment> payments = List.of(
                new CreditCard("1234-5678-9012-3456", 100.0),
                new PayPal("user@example.com", 50.0),
                new BankTransfer("DE89370400440532013000", 200.0),
                new CreditCard("9876-5432-1098-7654", 0.0)
        );

        List<String> descriptions = payments
                .filter(p -> extractAmount(p) > 0)
                .map(this::describePayment);

        assertThat(descriptions).hasSize(3);
        assertThat(descriptions.get(0)).contains("CreditCard");
        assertThat(descriptions.get(1)).contains("PayPal");
        assertThat(descriptions.get(2)).contains("BankTransfer");
    }

    private String describePayment(Payment payment) {
        return switch (payment) {
            case CreditCard(String card, double amount) ->
                    "CreditCard: " + card.substring(15) + " - $" + amount;
            case PayPal(String email, double amount) ->
                    "PayPal: " + email + " - $" + amount;
            case BankTransfer(String iban, double amount) ->
                    "BankTransfer: " + iban + " - $" + amount;
        };
    }

    @Test
    @DisplayName("should use Option for safe null handling")
    void testSafeNullHandling() {
        Option<Payment> maybePayment = Option.of(new CreditCard("1234-5678-9012-3456", 100.0));
        Option<Payment> nothingPayment = Option.none();

        String result1 = maybePayment
                .map(this::describePayment)
                .getOrElse("No payment");

        String result2 = nothingPayment
                .map(this::describePayment)
                .getOrElse("No payment");

        assertThat(result1).contains("CreditCard");
        assertThat(result2).isEqualTo("No payment");
    }

    @Test
    @DisplayName("should chain operations with flatMap")
    void testFlatMapChaining() {
        Option<Payment> payment = Option.of(new CreditCard("1234-5678-9012-3456", 100.0));

        Option<String> result = payment
                .flatMap(p -> validatePayment(p).toOption())
                .flatMap(this::processPayment);

        assertThat(result.isDefined()).isTrue();
        assertThat(result.get()).contains("100.0");
    }
}
