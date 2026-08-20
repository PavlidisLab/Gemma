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
 * Resolves the corpus-side seed set for the CHEBI slim build (consumed by
 * {@code ubic.gemma.core.ontology.providers.OntologySlimExtractor}): every CHEBI
 * URI the gemd corpus uses, in any of the subject, object or second-object slots.
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
     * Read the set of distinct CHEBI URIs the corpus uses, across every slot a term can occupy.
     * Result is cached only by the caller — this method always hits the database.
     * <p>
     * 🛑 <b>The object slots are not an afterthought for CHEBI specifically.</b> A Statement has three
     * annotatable slots, and a drug or treatment is most often the OBJECT of one ("treated with" → object)
     * rather than its subject. Seeding from {@code valueUri} alone therefore misses CHEBI terms in the position
     * CHEBI terms most commonly occupy, and the slim then cannot resolve a compound the corpus demonstrably uses.
     * <p>
     * This widens the seed set without widening the ontology: every URI added here is one the corpus already
     * annotates, so the seed count stays bounded by our own curation rather than by CHEBI's ~200k classes. It is
     * deliberately NOT "seed everything" — the slim exists because the full source is a 250 MB+ parse.
     */
    @Transactional(readOnly = true)
    public Set<String> resolveCorpusSeeds() {
        long start = System.currentTimeMillis();
        Set<String> seeds = new HashSet<>();
        int fromValue = collect( seeds, "select distinct c.valueUri from Characteristic c "
                + "where c.valueUri like :prefix" );
        int fromObject = collect( seeds, "select distinct s.objectUri from Statement s "
                + "where s.objectUri like :prefix" );
        int fromSecondObject = collect( seeds, "select distinct s.secondObjectUri from Statement s "
                + "where s.secondObjectUri like :prefix" );
        // Per-slot counts are logged because the seed count is the thing that governs how big the slim gets;
        // a surprise here is the early warning that the slim is drifting back towards the full ontology.
        log.info( "Resolved {} corpus CHEBI seeds in {} ms (value={}, object={}, secondObject={}; "
                        + "slots overlap, so the total is smaller than the sum).",
                seeds.size(), System.currentTimeMillis() - start, fromValue, fromObject, fromSecondObject );
        return seeds;
    }

    /**
     * @return how many distinct CHEBI URIs this slot contributed, before de-duplication against the other slots
     */
    private int collect( Set<String> seeds, String hql ) {
        @SuppressWarnings("unchecked")
        List<String> rows = sessionFactory.getCurrentSession()
                .createQuery( hql )
                .setParameter( "prefix", CHEBI_PREFIX + "%" )
                .list();
        int n = 0;
        for ( String uri : rows ) {
            if ( uri != null && uri.startsWith( CHEBI_PREFIX ) ) {
                seeds.add( uri );
                n++;
            }
        }
        return n;
    }
}
