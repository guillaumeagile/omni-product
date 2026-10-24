package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.SourceTypes;
import com.omniproduct.harness.Violation;
import com.omniproduct.harness.Words;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

// HA4.2 (moteur AST) - un constructeur ne lève pas d'exception. Une entrée invalide est un résultat, renvoyé par une
// méthode de fabrication (HA4.4), pas une exception levée par `new`.
// Signalés dans le corps d'un constructeur (classique ou compact de record) : une instruction `throw`, un `assert`, et
// un appel dont le nom parle de garde : requireNonNull, checkArgument, throwIfNull, ...
public final class NoThrowInConstructorRule implements HarnessRule {

    private static final List<String> GUARD_WORDS = List.of("throw", "require", "check", "assert");
    private static final List<String> CONSTRUCTORS = List.of("constructor_declaration", "compact_constructor_declaration");

    private final Predicate<String> packageFilter;

    public NoThrowInConstructorRule(Predicate<String> packageFilter) {
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA4.2";
    }

    @Override
    public String name() {
        return "No exception thrown in constructors (return a value from a create/build factory instead)";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        return sources.files().stream()
                .filter(file -> packageFilter.test(file.packageName()))
                .flatMap(NoThrowInConstructorRule::violations)
                .toList();
    }

    private static Stream<Violation> violations(SourceFile file) {
        return SourceTypes.declarations(file).flatMap(type -> SourceTypes.members(type)
                .filter(member -> CONSTRUCTORS.contains(member.type()))
                .flatMap(constructor -> constructor.descendants()
                        .flatMap(node -> guard(node).stream()
                                .map(what -> new Violation(file.path(), node.line(),
                                        "Constructor of '" + SourceTypes.name(type) + "' contains " + what + ". "
                                                + "Validate in a create/build factory returning a sealed result instead (HA4.4)."))))
                .distinct());
    }

    private static java.util.Optional<String> guard(Node node) {
        return switch (node.type()) {
            case "throw_statement" -> java.util.Optional.of("a 'throw' statement");
            case "assert_statement" -> java.util.Optional.of("an 'assert' statement");
            case "method_invocation" -> node.field("name")
                    .filter(name -> Words.of(name.text()).stream().anyMatch(GUARD_WORDS::contains))
                    .map(name -> "a guard call '" + name.text() + "'");
            default -> java.util.Optional.empty();
        };
    }
}
