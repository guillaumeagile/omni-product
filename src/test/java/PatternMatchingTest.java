import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pattern Matching Enhancements - Java 25")
class PatternMatchingTest {

    sealed interface Shape permits Circle, Rectangle, Triangle {}

    record Circle(double radius) implements Shape {}

    record Rectangle(double width, double height) implements Shape {}

    record Triangle(double a, double b, double c) implements Shape {}

    record ShapeContainer(Shape shape, String label) {}

    @Test
    @DisplayName("should calculate area using pattern matching with sealed types")
    void testPatternMatchingWithSealedTypes() {
        Shape circle = new Circle(5.0);
        Shape rectangle = new Rectangle(4.0, 6.0);
        Shape triangle = new Triangle(3.0, 4.0, 5.0);

        double circleArea = calculateArea(circle);
        double rectangleArea = calculateArea(rectangle);
        double triangleArea = calculateArea(triangle);

        assertThat(circleArea).isCloseTo(78.54, org.assertj.core.data.Offset.offset(0.01));
        assertThat(rectangleArea).isEqualTo(24.0);
        assertThat(triangleArea).isCloseTo(6.0, org.assertj.core.data.Offset.offset(0.01));
    }

    private double calculateArea(Shape shape) {
        return switch (shape) {
            case Circle(double radius) -> Math.PI * radius * radius;
            case Rectangle(double width, double height) -> width * height;
            case Triangle(double a, double b, double c) -> {
                double s = (a + b + c) / 2;
                yield Math.sqrt(s * (s - a) * (s - b) * (s - c));
            }
        };
    }

    @Test
    @DisplayName("should validate shapes using pattern matching with guards")
    void testPatternMatchingWithGuards() {
        Shape validCircle = new Circle(5.0);
        Shape invalidCircle = new Circle(-1.0);
        Shape validRectangle = new Rectangle(4.0, 6.0);

        assertThat(isValidShape(validCircle)).isTrue();
        assertThat(isValidShape(invalidCircle)).isFalse();
        assertThat(isValidShape(validRectangle)).isTrue();
    }

    private boolean isValidShape(Shape shape) {
        return switch (shape) {
            case Circle(double radius) when radius > 0 -> true;
            case Rectangle(double width, double height) when width > 0 && height > 0 -> true;
            case Triangle(double a, double b, double c) when a > 0 && b > 0 && c > 0 ->
                    a + b > c && b + c > a && a + c > b;
            default -> false;
        };
    }

    @Test
    @DisplayName("should describe shapes using pattern matching")
    void testPatternMatchingDescription() {
        Shape circle = new Circle(5.0);
        Shape rectangle = new Rectangle(4.0, 6.0);
        Shape triangle = new Triangle(3.0, 4.0, 5.0);

        assertThat(describeShape(circle)).isEqualTo("Circle with radius 5.0");
        assertThat(describeShape(rectangle)).isEqualTo("Rectangle 4.0 x 6.0");
        assertThat(describeShape(triangle)).isEqualTo("Triangle with sides 3.0, 4.0, 5.0");
    }

    private String describeShape(Shape shape) {
        return switch (shape) {
            case Circle(double radius) -> "Circle with radius " + radius;
            case Rectangle(double width, double height) -> "Rectangle " + width + " x " + height;
            case Triangle(double a, double b, double c) -> "Triangle with sides " + a + ", " + b + ", " + c;
        };
    }

    @Test
    @DisplayName("should handle nested pattern matching")
    void testNestedPatternMatching() {
        ShapeContainer container1 = new ShapeContainer(new Circle(5.0), "MyCircle");
        ShapeContainer container2 = new ShapeContainer(new Rectangle(4.0, 6.0), "MyRectangle");

        assertThat(getShapeInfo(container1)).isEqualTo("MyCircle: Circle with radius 5.0");
        assertThat(getShapeInfo(container2)).isEqualTo("MyRectangle: Rectangle 4.0 x 6.0");
    }

    private String getShapeInfo(Object obj) {
        return switch (obj) {
            case ShapeContainer(Circle(double radius), String label) ->
                    label + ": Circle with radius " + radius;
            case ShapeContainer(Rectangle(double width, double height), String label) ->
                    label + ": Rectangle " + width + " x " + height;
            case ShapeContainer(Triangle(double a, double b, double c), String label) ->
                    label + ": Triangle with sides " + a + ", " + b + ", " + c;
            default -> "Unknown";
        };
    }

    @Test
    @DisplayName("should use instanceof with pattern matching")
    void testInstanceofPatternMatching() {
        Object obj1 = new Circle(5.0);
        Object obj2 = "not a shape";

        if (obj1 instanceof Circle(double radius)) {
            assertThat(radius).isEqualTo(5.0);
        } else {
            throw new AssertionError("Should match Circle pattern");
        }

        if (obj2 instanceof Circle(double r)) {
            throw new AssertionError("Should not match Circle pattern");
        }
    }

    @Test
    @DisplayName("should use instanceof with guards")
    void testInstanceofWithGuards() {
        Object obj = new Circle(5.0);

        if (obj instanceof Circle(double radius) && radius > 3.0) {
            assertThat(true).isTrue();
        } else {
            throw new AssertionError("Should match Circle with radius > 3.0");
        }

        Object obj2 = new Circle(2.0);
        if (obj2 instanceof Circle(double r2) && r2 > 3.0) {
            throw new AssertionError("Should not match Circle with radius > 3.0");
        }
    }
}
