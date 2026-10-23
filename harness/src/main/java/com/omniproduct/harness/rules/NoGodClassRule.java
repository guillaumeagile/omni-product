package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import org.treesitter.TSNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

// HA5 (moteur AST) - pas de god class : au plus N propriétés publiques par type.
// Une propriété publique est un composant de record, un champ public, ou un getter public
// sans paramètre (getX / isX). Les propriétés dont le nom finit par "Id" sont ignorées :
// ce sont des identifiants, pas de la surface comportementale.
public final class NoGodClassRule implements HarnessRule {

    private final int maxPublicProperties;
    private final Predicate<String> packageFilter;

    public NoGodClassRule(int maxPublicProperties, Predicate<String> packageFilter) {
        this.maxPublicProperties = maxPublicProperties;
        this.packageFilter = packageFilter;
    }

    @Override
    public String id() {
        return "HA5";
    }

    @Override
    public String name() {
        return "No god class (at most " + maxPublicProperties + " public properties)";
    }

    @Override
    public List<Violation> check(SourceSet sources) {
        List<Violation> violations = new ArrayList<>();
        for (SourceFile file : sources.files()) {
            if (packageFilter.test(file.packageName())) {
                visit(file, file.root(), violations);
            }
        }
        return violations;
    }

    private void visit(SourceFile file, TSNode node, List<Violation> violations) {
        String type = node.getType();
        if (type.equals("class_declaration") || type.equals("record_declaration")) {
            Set<String> properties = publicProperties(file, node);
            if (properties.size() > maxPublicProperties) {
                TSNode name = node.getChildByFieldName("name");
                violations.add(new Violation(file.path(), file.line(name),
                        "'" + file.text(name) + "' has " + properties.size() + " public properties "
                                + properties + " (max " + maxPublicProperties + "). Split into smaller, focused types."));
            }
        }
        for (int i = 0; i < node.getNamedChildCount(); i++) {
            visit(file, node.getNamedChild(i), violations);
        }
    }

    private Set<String> publicProperties(SourceFile file, TSNode typeNode) {
        Set<String> names = new LinkedHashSet<>();
        if (typeNode.getType().equals("record_declaration")) {
            TSNode parameters = typeNode.getChildByFieldName("parameters");
            for (int i = 0; parameters != null && !parameters.isNull() && i < parameters.getNamedChildCount(); i++) {
                TSNode name = parameters.getNamedChild(i).getChildByFieldName("name");
                if (name != null && !name.isNull()) {
                    names.add(file.text(name));
                }
            }
        }
        TSNode body = typeNode.getChildByFieldName("body");
        for (int i = 0; body != null && !body.isNull() && i < body.getNamedChildCount(); i++) {
            TSNode member = body.getNamedChild(i);
            if (!isPublic(file, member)) {
                continue;
            }
            if (member.getType().equals("field_declaration")) {
                for (int j = 0; j < member.getNamedChildCount(); j++) {
                    TSNode declarator = member.getNamedChild(j);
                    if (declarator.getType().equals("variable_declarator")) {
                        names.add(file.text(declarator.getChildByFieldName("name")));
                    }
                }
            } else if (member.getType().equals("method_declaration") && hasNoParameters(member)) {
                accessorProperty(file.text(member.getChildByFieldName("name"))).ifPresent(names::add);
            }
        }
        names.removeIf(n -> n.endsWith("Id"));
        return names;
    }

    private static boolean hasNoParameters(TSNode method) {
        TSNode parameters = method.getChildByFieldName("parameters");
        return parameters == null || parameters.isNull() || parameters.getNamedChildCount() == 0;
    }

    // getName -> name, isActive -> active ; tout autre nom n'est pas un accesseur.
    private static java.util.Optional<String> accessorProperty(String method) {
        for (String prefix : new String[]{"get", "is"}) {
            if (method.length() > prefix.length() && method.startsWith(prefix)
                    && Character.isUpperCase(method.charAt(prefix.length()))) {
                String rest = method.substring(prefix.length());
                return java.util.Optional.of(Character.toLowerCase(rest.charAt(0)) + rest.substring(1));
            }
        }
        return java.util.Optional.empty();
    }

    private static boolean isPublic(SourceFile file, TSNode member) {
        for (int i = 0; i < member.getChildCount(); i++) {
            TSNode child = member.getChild(i);
            if (child.getType().equals("modifiers")) {
                for (int j = 0; j < child.getChildCount(); j++) {
                    if (child.getChild(j).getType().equals("public")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
