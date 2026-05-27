package ubic.gemma.core.ontology.providers.chebi;

import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChebiSeedResolverTest {

    /**
     * Regression: ChebiSeedResolver must carry {@code @Component} so
     * {@code OntologyConfig.chebiOntologyService} can autowire it (required=false).
     * Without the stereotype the bean disappears silently and the slim path falls
     * back to the full-source load on every boot.
     */
    @Test
    void hasSpringComponentStereotype() {
        assertTrue( ChebiSeedResolver.class.isAnnotationPresent( Component.class ),
                "ChebiSeedResolver must be @Component for OntologyConfig autowiring; "
                        + "without it the CHEBI slim path is silently disabled." );
    }
}
