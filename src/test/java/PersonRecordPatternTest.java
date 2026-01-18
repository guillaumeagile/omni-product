import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Person Record Patterns - Java 25")
class PersonRecordPatternTest {

    record Person(String name, int age, Address address) {}

    record Address(String street, String city, String zipCode) {}

    @Test
    @DisplayName("should destructure deeply nested person and address records")
    void testDeeplyNestedRecordDestructuring() {
        Person person = new Person(
                "John Doe",
                30,
                new Address("123 Main St", "New York", "10001")
        );

        if (person instanceof Person(String name, int age, Address(String street, String city, String zip))) {
            assertThat(name).isEqualTo("John Doe");
            assertThat(age).isEqualTo(30);
            assertThat(street).isEqualTo("123 Main St");
            assertThat(city).isEqualTo("New York");
            assertThat(zip).isEqualTo("10001");
        } else {
            throw new AssertionError("Should destructure nested Person and Address");
        }
    }

    @Test
    @DisplayName("should validate voting eligibility using record patterns with guards")
    void testVotingEligibilityValidation() {
        Person bostonAdult = new Person("Alice", 25, new Address("456 Oak Ave", "Boston", "02101"));
        Person bostonMinor = new Person("Bob", 16, new Address("789 Pine Rd", "Boston", "02102"));
        Person nycAdult = new Person("Charlie", 30, new Address("321 5th Ave", "New York", "10001"));

        assertThat(canVote(bostonAdult)).isTrue();
        assertThat(canVote(bostonMinor)).isFalse();
        assertThat(canVote(nycAdult)).isTrue();
    }

    private boolean canVote(Person person) {
        return switch (person) {
            case Person(String name, int age, Address(String city, _, _))
                    when age >= 18 && city.equals("Boston") -> true;
            case Person(_, int age, _)
                    when age >= 18 -> true;
            default -> false;
        };
    }

    @Test
    @DisplayName("should validate address records using wildcards and guards")
    void testAddressValidation() {
        Address validAddress = new Address("123 Main St", "New York", "10001");
        Address invalidAddress = new Address("", "New York", "");

        assertThat(isValidAddress(validAddress)).isTrue();
        assertThat(isValidAddress(invalidAddress)).isFalse();
    }

    private boolean isValidAddress(Address address) {
        return switch (address) {
            case Address(String street, String city, String zip) when
                    !street.isBlank() && !city.isBlank() && !zip.isBlank() && zip.length() == 5 ->
                    true;
            default -> false;
        };
    }
}
