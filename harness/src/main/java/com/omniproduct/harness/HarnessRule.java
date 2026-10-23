package com.omniproduct.harness;

import java.util.List;

public interface HarnessRule {

    String id();

    String name();

    // Les règles WARNING sont affichées [WARN] et ne font jamais échouer le harnais.
    default HarnessSeverity severity() {
        return HarnessSeverity.ERROR;
    }

    List<Violation> check(SourceSet sources);
}
