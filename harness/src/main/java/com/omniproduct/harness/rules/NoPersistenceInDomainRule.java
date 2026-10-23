package com.omniproduct.harness.rules;

import com.omniproduct.harness.HarnessRule;
import com.omniproduct.harness.SourceFile;
import com.omniproduct.harness.SourceSet;
import com.omniproduct.harness.Violation;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import org.treesitter.TSNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// HA8 (moteur BYTECODE, ArchUnit) - un modèle de domaine ne sait pas comment il est stocké :
// aucune dépendance vers JPA (jakarta.persistence) ni Spring Data. Les repositories et la
// configuration JPA sont de l'infrastructure, hors du package du domaine.
public final class NoPersistenceInDomainRule implements HarnessRule {

    private static final Pattern LOCATION = Pattern.compile("\\((\\w+\\.java):(\\d+)\\)");
    private static final Pattern ANNOTATION = Pattern.compile("annotated with <([\\w.$]+)>");

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
        JavaClasses classes = sources.classes();
        ArchRule rule = ArchRuleDefinition.noClasses()
                .that().resideInAPackage(domainPackage)
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.springframework.data..");
        EvaluationResult result = rule.evaluate(classes);

        List<Violation> violations = new ArrayList<>();
        Map<String, Integer> occurrences = new HashMap<>();
        for (String detail : result.getFailureReport().getDetails()) {
            Matcher location = LOCATION.matcher(detail);
            boolean located = location.find();
            String file = located ? location.group(1) : "?";
            int line = located ? Integer.parseInt(location.group(2)) : 0;
            // Le bytecode ne porte pas la ligne d'une annotation (":0") : le moteur AST la retrouve dans le source.
            if (line == 0) {
                line = annotationLine(sources, file, detail, occurrences);
            }
            violations.add(new Violation(file, line, detail));
        }
        return violations;
    }

    // Ligne de la N-ième annotation de ce nom dans le fichier (N = nombre de violations déjà rapportées pour elle).
    private static int annotationLine(SourceSet sources, String fileName, String detail, Map<String, Integer> occurrences) {
        Matcher annotation = ANNOTATION.matcher(detail);
        if (!annotation.find()) {
            return 0;
        }
        String qualified = annotation.group(1);
        String simpleName = qualified.substring(qualified.lastIndexOf('.') + 1);
        for (SourceFile source : sources.files()) {
            if (Path.of(source.path()).getFileName().toString().equals(fileName)) {
                List<Integer> lines = new ArrayList<>();
                collectAnnotationLines(source, source.root(), simpleName, lines);
                int index = occurrences.merge(fileName + "@" + simpleName, 1, Integer::sum) - 1;
                return lines.isEmpty() ? 0 : lines.get(Math.min(index, lines.size() - 1));
            }
        }
        return 0;
    }

    private static void collectAnnotationLines(SourceFile source, TSNode node, String simpleName, List<Integer> lines) {
        if (node.getType().equals("marker_annotation") || node.getType().equals("annotation")) {
            TSNode name = node.getChildByFieldName("name");
            if (name != null && !name.isNull() && source.text(name).endsWith(simpleName)) {
                lines.add(source.line(node));
            }
        }
        for (int i = 0; i < node.getNamedChildCount(); i++) {
            collectAnnotationLines(source, node.getNamedChild(i), simpleName, lines);
        }
    }
}
