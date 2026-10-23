package com.omniproduct.harness;

import com.tngtech.archunit.lang.ArchRule;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Pont entre les deux moteurs : ArchUnit (bytecode) trouve la violation, tree-sitter (source) la localise.
// Le bytecode ne connaît pas la ligne d'une annotation, d'un champ ou d'une classe (":0") ; le message
// ArchUnit désigne en revanche l'élément fautif, qu'on retrouve dans l'AST.
public final class ArchFindings {

    // "Field <pkg.Outer$Inner.name> is annotated with <jakarta.persistence.Column> in (Product.java:0)"
    // "Constructor <pkg.Product.<init>(pkg.ProductService)> has parameter of type <pkg.ProductService> in (Product.java:0)"
    private static final Pattern DETAIL =
            Pattern.compile("^(\\w+) <([^>]+(?:<init>[^>]*)?)> .*?<([^>]+)>.* in \\((\\w+\\.java):(\\d+)\\)");
    private static final List<String> TYPE_DECLARATIONS =
            List.of("class_declaration", "record_declaration", "interface_declaration", "enum_declaration");

    private ArchFindings() {
    }

    public static List<Violation> of(SourceSet sources, ArchRule rule) {
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
                .or(() -> sourceLine(sources, fileName, match.group(1), match.group(2), match.group(3), detail))
                .orElse(0);
        return new Violation(fileName, line, detail);
    }

    // kind = Class | Field | Method | Constructor ; member = "pkg.Outer$Inner" | "pkg.Outer.field" | "pkg.Outer.method(args)"
    private static Optional<Integer> sourceLine(SourceSet sources, String fileName, String kind, String member,
                                                String target, String detail) {
        return sources.files().stream()
                .filter(file -> file.fileName().equals(fileName))
                .flatMap(file -> scope(file, kind, member).stream())
                .findFirst()
                .map(scope -> detail.contains("annotat")
                        ? annotationLine(scope, target)
                        : scope.field("name").orElse(scope).line());
    }

    private static int annotationLine(Node scope, String target) {
        String annotation = target.substring(target.lastIndexOf('.') + 1);
        return scope.annotations()
                .filter(a -> a.field("name").map(Node::text).filter(name -> name.endsWith(annotation)).isPresent())
                .findFirst()
                .map(Node::line)
                .orElse(scope.line());
    }

    private static Optional<Node> scope(SourceFile file, String kind, String member) {
        return switch (kind) {
            case "Class" -> typeDeclaration(file, simpleName(member));
            case "Field" -> typeDeclaration(file, simpleName(before(member, member.lastIndexOf('.'))))
                    .flatMap(type -> fieldDeclaration(type, member.substring(member.lastIndexOf('.') + 1)));
            case "Method", "Constructor" -> {
                int paren = member.indexOf('(');
                String owner = before(member, member.lastIndexOf('.', paren));
                String name = member.substring(member.lastIndexOf('.', paren) + 1, paren);
                yield typeDeclaration(file, simpleName(owner)).flatMap(type -> methodDeclaration(type, name));
            }
            default -> Optional.empty();
        };
    }

    private static Optional<Node> typeDeclaration(SourceFile file, String simpleName) {
        return file.descendants()
                .filter(node -> TYPE_DECLARATIONS.contains(node.type()))
                .filter(type -> type.field("name").map(Node::text).filter(simpleName::equals).isPresent())
                .findFirst();
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

    private static Optional<Node> methodDeclaration(Node type, String name) {
        return type.field("body").stream()
                .flatMap(Node::children)
                .filter(member -> name.equals("<init>")
                        ? member.is("constructor_declaration")
                        : member.is("method_declaration") && member.field("name").map(Node::text).filter(name::equals).isPresent())
                .findFirst();
    }

    private static String before(String text, int index) {
        return text.substring(0, index);
    }

    // "com.x.Product$Price" -> "Price"
    private static String simpleName(String qualified) {
        return qualified.substring(Math.max(qualified.lastIndexOf('.'), qualified.lastIndexOf('$')) + 1);
    }
}
