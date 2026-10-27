package com.omniproduct.harness;

import com.omniproduct.harness.rules.BareIdentifierIsWeakRule;
import com.omniproduct.harness.rules.CollectionsArePipelinesRule;
import com.omniproduct.harness.rules.EntityMustImplementIdentifiableRule;
import com.omniproduct.harness.rules.FactoryValidatesByCompositionRule;
import com.omniproduct.harness.rules.FanOutRule;
import com.omniproduct.harness.rules.ImmutableDataStructuresRule;
import com.omniproduct.harness.rules.MatchOverAdtRule;
import com.omniproduct.harness.rules.NoClockInModelRule;
import com.omniproduct.harness.rules.NoGodClassRule;
import com.omniproduct.harness.rules.NoInheritanceRule;
import com.omniproduct.harness.rules.NoPersistenceInDomainRule;
import com.omniproduct.harness.rules.NoServicesInEntityConstructorRule;
import com.omniproduct.harness.rules.NoThrowInConstructorRule;
import com.omniproduct.harness.rules.PrimitiveObsessionRule;
import com.omniproduct.harness.rules.ProductHasAdtPropertyRule;
import com.omniproduct.harness.rules.RecordsAreValueObjectsRule;
import com.omniproduct.harness.rules.RichIdentifierRule;
import com.omniproduct.harness.rules.SellChecksCanSellBeforeWithdrawRule;
import com.omniproduct.harness.rules.SingleConcernRule;
import com.omniproduct.harness.rules.ValueObjectFactoryRule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

// Les règles que chaque phase de l'atelier fait passer. La phase 1 (CUPID-phase-1.md) couvre HA5 à HA12.1 ;
// HA4 (pas d'héritage) relève de la phase 2 : sa règle reste dans le harnais, elle n'est simplement pas câblée ici.
public final class Phases {

    // Le code de l'exercice : le modèle et les services, pas le port carbone (domain/) ni l'infrastructure.
    private static final String MODEL_PACKAGE = "com.omniproduct.model";
    private static final String SERVICE_PACKAGE = "com.omniproduct.service";

    private static final Predicate<String> IS_MODEL = pkg -> isIn(pkg, MODEL_PACKAGE);
    private static final Predicate<String> IS_MODEL_OR_SERVICE = IS_MODEL.or(pkg -> isIn(pkg, SERVICE_PACKAGE));

    public static final String ALL = "all";

    private Phases() {
    }

    public static Set<String> names() {
        return Set.of("1", "2", ALL);
    }

    public static List<HarnessRule> rules(String phase) {
        return Map.<String, List<HarnessRule>>of("1", phase1(), "2", phase2(), ALL, phase2()).get(phase);
    }

    // HA5, HA6, HA7, HA8, HA9, HA10, HA11, HA12, HA12.1 (avertissement)
    private static List<HarnessRule> phase1() {
        UbiquitousLanguageMap language = UbiquitousLanguageMap.load();
        return List.of(
                new NoGodClassRule(4, IS_MODEL),
                new SingleConcernRule(language, 1, IS_MODEL_OR_SERVICE),
                new FanOutRule(MODEL_PACKAGE + ".."),
                new NoPersistenceInDomainRule(MODEL_PACKAGE + "..", IS_MODEL),
                new EntityMustImplementIdentifiableRule(MODEL_PACKAGE + ".."),
                new NoServicesInEntityConstructorRule(MODEL_PACKAGE + ".."),
                new PrimitiveObsessionRule(language, IS_MODEL),
                new RichIdentifierRule(IS_MODEL),
                new BareIdentifierIsWeakRule(IS_MODEL_OR_SERVICE));
    }

    // La phase 1 plus HA2 (données immuables), HA3 (value objects), HA4.1 à HA4.9 (composable, ADT, horloge, vente,
    // pipelines, fabrique par composition).
    private static List<HarnessRule> phase2() {
        return Stream.concat(phase1().stream(), List.<HarnessRule>of(
                new ImmutableDataStructuresRule(IS_MODEL),
                new RecordsAreValueObjectsRule(IS_MODEL),
                new NoInheritanceRule(MODEL_PACKAGE + "..", SERVICE_PACKAGE + ".."),
                new NoThrowInConstructorRule(IS_MODEL),
                new ProductHasAdtPropertyRule(IS_MODEL),
                new ValueObjectFactoryRule(IS_MODEL),
                new MatchOverAdtRule(IS_MODEL_OR_SERVICE),
                new NoClockInModelRule(MODEL_PACKAGE + ".."),
                new SellChecksCanSellBeforeWithdrawRule(IS_MODEL_OR_SERVICE),
                new CollectionsArePipelinesRule(IS_MODEL_OR_SERVICE),
                new FactoryValidatesByCompositionRule(IS_MODEL)).stream()).toList();
    }

    private static boolean isIn(String pkg, String base) {
        return pkg.equals(base) || pkg.startsWith(base + ".");
    }
}
