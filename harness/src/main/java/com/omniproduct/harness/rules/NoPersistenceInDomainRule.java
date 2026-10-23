package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.Node;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// HA8 (moteurs BYTECODE + AST) - un modèle de domaine ne sait pas comment il est stocké :
// aucune dépendance vers JPA (jakarta.persistence) ni Spring Data. Les repositories et la
// configuration JPA sont de l'infrastructure, hors du package du domaine.
//
// ArchUnit (bytecode) trouve les violations mais ne connaît pas la ligne d'une annotation
// (":0") : tree-sitter la retrouve dans le source, d'après le champ ou la classe que le message désigne.
public final class NoPersistenceInDomainRule implements HarnessRule {

    // "Field <pkg.Outer$Inner.name> is annotated with <jakarta.persistence.Column> in (Product.java:0)"
    private static final Pattern DETAIL =
            Pattern.compile("^(\\w+) <([^>]+)> .*?<([^>]+)>.* in \\((\\w+\\.java):(\\d+)\\)");

    private final String domainPackage;

    // domainPackage au format ArchUnit, par exemple "..model.."
    public NoPersistenceInDomainRule(String domainPackage) {
        this.domainPackage = domainPackage;
    }

    @Override
    public String id() {
        return "HA8";
    }

    @Override
    public String name() {
        return "No Persistence in Domain (no JPA / Spring Data in " + domainPackage + ")";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage(domainPackage)
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..");

        return rule.evaluate(sources.classes()).getFailureReport().getDetails().stream()
                .map(detail -> toViolation(sources, detail))
                .toList();
    }

    private static Violation toViolation(SourceSet sources, String detail) {
        Matcher match = DETAIL.matcher(detail);
        if (!match.find()) {
            return new Violation("?", 0, detail);
        }
        String fileName = match.group(4);
        int line = Optional.of(Integer.parseInt(match.group(5)))
                .filter(archUnitLine -> archUnitLine > 0)
                .or(() -> sourceLine(sources, fileName, match.group(1), match.group(2), match.group(3)))
                .orElse(0);
        return new Violation(fileName, line, detail);
    }

    // kind = "Class" | "Field" ; member = "pkg.Outer$Inner" ou "pkg.Outer$Inner.field" ; target = "jakarta.persistence.Id"
    private static Optional<Integer> sourceLine(SourceSet sources, String fileName, String kind, String member, String target) {
        String annotation = target.substring(target.lastIndexOf('.') + 1);
        boolean isField = kind.equals("Field");
        String typeName = simpleName(isField ? member.substring(0, member.lastIndexOf('.')) : member);
        Optional<String> fieldName = Optional.of(member).filter(m -> isField).map(m -> m.substring(m.lastIndexOf('.') + 1));

        return sources.files().stream()
                .filter(file -> file.fileName().equals(fileName))
                .flatMap(SourceFile::descendants)
                .filter(node -> node.is("class_declaration", "record_declaration", "interface_declaration", "enum_declaration"))
                .filter(type -> type.field("name").map(Node::text).filter(typeName::equals).isPresent())
                .findFirst()
                .flatMap(type -> fieldName
                        .map(field -> fieldDeclaration(type, field))
                        .orElseGet(() -> Optional.of(type)))
                .map(scope -> scope.annotations()
                        .filter(a -> a.field("name").map(Node::text).filter(n -> n.endsWith(annotation)).isPresent())
                        .findFirst()
                        .map(Node::line)
                        .orElse(scope.line()));
    }

    private static Optional<Node> fieldDeclaration(Node type, String field) {
        return type.field("body").stream()
                .flatMap(Node::children)
                .filter(member -> member.is("field_declaration"))
                .filter(declaration -> declaration.children()
                        .filter(declarator -> declarator.is("variable_declarator"))
                        .flatMap(declarator -> declarator.field("name").stream())
                        .anyMatch(name -> name.text().equals(field)))
                .findFirst();
    }

    // "com.x.Product$Price" -> "Price"
    private static String simpleName(String qualified) {
        return qualified.substring(Math.max(qualified.lastIndexOf('.'), qualified.lastIndexOf('$')) + 1);
    }
}
