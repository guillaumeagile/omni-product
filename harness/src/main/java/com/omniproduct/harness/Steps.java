package com.omniproduct.harness;

import com.omniproduct.harness.rules.BareIdentifierIsWeakRule;
import com.omniproduct.harness.rules.EntityMustImplementIdentifiableRule;
import com.omniproduct.harness.rules.FanOutRule;
import com.omniproduct.harness.rules.NoGodClassRule;
import com.omniproduct.harness.rules.NoPersistenceInDomainRule;
import com.omniproduct.harness.rules.NoServicesInEntityConstructorRule;
import com.omniproduct.harness.rules.PrimitiveObsessionRule;
import com.omniproduct.harness.rules.RichIdentifierRule;
import com.omniproduct.harness.rules.SingleConcernRule;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

// Les règles que chaque étape de l'atelier fait passer. L'étape 1 (CUPID-step-1.md) couvre HA5 à HA12.1 ;
// HA4 (pas d'héritage) relève de l'étape 2 : sa règle reste dans le harnais, elle n'est simplement pas câblée ici.
public final class Steps {

    // Le code de l'exercice : le modèle et les services, pas le port carbone (domain/) ni l'infrastructure.
    private static final String MODEL_PACKAGE = "com.omniproduct.model";
    private static final String SERVICE_PACKAGE = "com.omniproduct.service";

    private static final Predicate<String> IS_MODEL = pkg -> isIn(pkg, MODEL_PACKAGE);
    private static final Predicate<String> IS_MODEL_OR_SERVICE = IS_MODEL.or(pkg -> isIn(pkg, SERVICE_PACKAGE));

    public static final String ALL = "all";

    private Steps() {
    }

    public static Set<String> names() {
        return Set.of("1", ALL);
    }

    public static List<HarnessRule> rules(String step) {
        return Map.<String, List<HarnessRule>>of("1", step1(), ALL, step1()).get(step);
    }

    // HA5, HA6, HA7, HA8, HA9, HA10, HA11, HA12, HA12.1 (avertissement)
    private static List<HarnessRule> step1() {
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

    private static boolean isIn(String pkg, String base) {
        return pkg.equals(base) || pkg.startsWith(base + ".");
    }
}
