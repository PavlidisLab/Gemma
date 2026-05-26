package ubic.gemma.core.ontology.providers.chebi;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the corpus-side seed set for {@link ChebiSlimExtractor}: every CHEBI URI
 * currently used as a {@code Characteristic.valueUri} anywhere in the gemd corpus.
 *
 * <p>The slim extraction wants a seed of "every CHEBI term we have annotated or might
 * annotate". The corpus query covers "have annotated"; the {@code has_role} closure
 * (the {@code might} part for role traversal) is handled by the extractor itself
 * walking the source ontology after this seed lands.
 *
 * <p>Splitting the query into its own bean (rather than inlining into
 * {@code ChebiOntologyService}) keeps the Hibernate dependency surface localized and
 * lets test contexts substitute a fixed seed set without touching the service.
 */
@Component
public class ChebiSeedResolver {

    private static final Logger log = LoggerFactory.getLogger( ChebiSeedResolver.class );

    private static final String CHEBI_PREFIX = "http://purl.obolibrary.org/obo/CHEBI_";

    private final SessionFactory sessionFactory;

    @Autowired
    public ChebiSeedResolver( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Read the set of distinct CHEBI URIs currently used as {@code Characteristic.valueUri}.
     * Result is cached only by the caller — this method always hits the database.
     */
    @Transactional(readOnly = true)
    public Set<String> resolveCorpusSeeds() {
        long start = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        List<String> rows = sessionFactory.getCurrentSession()
                .createQuery( "select distinct c.valueUri from Characteristic c "
                        + "where c.valueUri like :prefix" )
                .setParameter( "prefix", CHEBI_PREFIX + "%" )
                .list();
        Set<String> seeds = new HashSet<>( rows.size() );
        for ( String uri : rows ) {
            if ( uri != null && uri.startsWith( CHEBI_PREFIX ) ) {
                seeds.add( uri );
            }
        }
        log.info( "Resolved {} corpus CHEBI seeds in {} ms.",
                seeds.size(), System.currentTimeMillis() - start );
        return seeds;
    }
}
