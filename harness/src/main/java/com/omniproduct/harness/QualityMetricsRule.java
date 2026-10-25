package com.omniproduct.harness;

import net.sourceforge.pmd.lang.ast.NodeStream;
import net.sourceforge.pmd.lang.java.ast.ASTAssignmentExpression;
import net.sourceforge.pmd.lang.java.ast.ASTCompilationUnit;
import net.sourceforge.pmd.lang.java.ast.ASTDoStatement;
import net.sourceforge.pmd.lang.java.ast.ASTExecutableDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTFieldDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTForStatement;
import net.sourceforge.pmd.lang.java.ast.ASTForeachStatement;
import net.sourceforge.pmd.lang.java.ast.ASTLambdaExpression;
import net.sourceforge.pmd.lang.java.ast.ASTTypeDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTUnaryExpression;
import net.sourceforge.pmd.lang.java.ast.ASTWhileStatement;
import net.sourceforge.pmd.lang.java.ast.JModifier;
import net.sourceforge.pmd.lang.java.ast.JavaNode;
import net.sourceforge.pmd.lang.java.metrics.JavaMetrics;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.metrics.Metric;
import net.sourceforge.pmd.lang.metrics.MetricsUtil;

import java.util.Locale;
import java.util.stream.IntStream;

// Règle PMD de mesure (essai, branche testPMD) : une « violation » par classe, record ou enum, dont le message porte
// ses mesures. Les métriques sont celles de PMD (JavaMetrics : WMC, TCC, FAN_OUT, COGNITIVE_COMPLEXITY), calculées sur
// l'AST de PMD, qui lit Java 21 (records, sealed, switch à motifs) ; les comptages (champs mutables, affectations,
// boucles, lambdas) parcourent ce même AST, lambdas comprises, sans les types imbriqués, mesurés à part.
public final class QualityMetricsRule extends AbstractJavaRule {

    @Override
    public Object visit(ASTCompilationUnit unit, Object data) {
        unit.descendants(ASTTypeDeclaration.class).crossFindBoundaries()
                .filter(type -> !type.isInterface() && !type.isAnnotation())
                .forEach(type -> asCtx(data).addViolationWithMessage(type, message(type)));
        return data;
    }

    private static String message(ASTTypeDeclaration type) {
        int[] cognitives = type.getOperations().toStream().mapToInt(QualityMetricsRule::cognitive).toArray();
        return String.format(Locale.ROOT,
                "quality type=%s wmc=%d tcc=%s fanout=%d mutable=%d assignments=%d loops=%d lambdas=%d cognitive=%d cognitiveMax=%d",
                type.getBinaryName(),
                metric(JavaMetrics.WEIGHED_METHOD_COUNT, type),
                tcc(type),
                metric(JavaMetrics.FAN_OUT, type),
                type.getDeclarations(ASTFieldDeclaration.class).filter(field -> !field.hasModifiers(JModifier.FINAL)).count(),
                own(type, ASTAssignmentExpression.class).count()
                        + own(type, ASTUnaryExpression.class).filter(unary -> unary.getOperator().isIncrement() || unary.getOperator().isDecrement()).count(),
                own(type, ASTForStatement.class).count() + own(type, ASTForeachStatement.class).count()
                        + own(type, ASTWhileStatement.class).count() + own(type, ASTDoStatement.class).count(),
                own(type, ASTLambdaExpression.class).count(),
                IntStream.of(cognitives).sum(),
                IntStream.of(cognitives).max().orElse(0));
    }

    // Les nœuds du type lui-même : tout le sous-arbre, lambdas comprises (PMD s'y arrête par défaut), sauf les types
    // imbriqués, mesurés à part.
    private static <N extends JavaNode> NodeStream<N> own(ASTTypeDeclaration type, Class<N> kind) {
        return type.descendants(kind).crossFindBoundaries().filter(node -> node.getEnclosingType() == type);
    }

    private static int cognitive(ASTExecutableDeclaration operation) {
        return MetricsUtil.supportsAll(operation, JavaMetrics.COGNITIVE_COMPLEXITY)
                ? MetricsUtil.computeMetric(JavaMetrics.COGNITIVE_COMPLEXITY, operation) : 0;
    }

    private static <N extends JavaNode> int metric(Metric<? super N, Integer> metric, N node) {
        return MetricsUtil.supportsAll(node, metric) ? MetricsUtil.computeMetric(metric, node) : 0;
    }

    // TCC : part des paires de méthodes qui partagent un champ ; indéfinie (NaN) sous deux méthodes : "na".
    private static String tcc(ASTTypeDeclaration type) {
        if (!MetricsUtil.supportsAll(type, JavaMetrics.TIGHT_CLASS_COHESION)) {
            return "na";
        }
        double value = MetricsUtil.computeMetric(JavaMetrics.TIGHT_CLASS_COHESION, type);
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.4f", value) : "na";
    }
}
