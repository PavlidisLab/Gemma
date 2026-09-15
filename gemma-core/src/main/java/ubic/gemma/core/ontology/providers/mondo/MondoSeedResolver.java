package ubic.gemma.core.ontology.providers.mondo;

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
 * MONDO counterpart of {@code ChebiSeedResolver}: every MONDO URI currently used as
 * {@code Characteristic.valueUri} anywhere in the gemd corpus. Drives the corpus-
 * tailored slim cache for {@code MondoOntologyService}.
 */
@Component
public class MondoSeedResolver {

    private static final Logger log = LoggerFactory.getLogger( MondoSeedResolver.class );

    private static final String MONDO_PREFIX = "http://purl.obolibrary.org/obo/MONDO_";

    private final SessionFactory sessionFactory;

    @Autowired
    public MondoSeedResolver( SessionFactory sessionFactory ) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Read the set of distinct MONDO URIs the corpus uses, across every slot a term can occupy.
     * <p>
     * The MONDO slim is disabled by default (see {@code OntologyConfig#mondoOntologyServiceOntologyService}),
     * so nothing calls this on a normal boot. It is kept correct rather than left blind, because the failure it
     * used to cause was silent: a seed set built from {@code valueUri} alone omits terms used as the object of a
     * statement, and the slim then cannot resolve a disease the corpus demonstrably annotates.
     * <p>
     * 🛑 Widening the seeds does not widen the ontology — every URI here is one the corpus already uses, so the
     * count stays bounded by our curation. What this cannot fix is the reason the slim was turned off: the
     * successor of an obsolete term is by definition a term we do NOT use yet, so no corpus-derived seed set can
     * contain it.
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
        log.info( "Resolved {} corpus MONDO seeds in {} ms (value={}, object={}, secondObject={}; "
                        + "slots overlap, so the total is smaller than the sum).",
                seeds.size(), System.currentTimeMillis() - start, fromValue, fromObject, fromSecondObject );
        return seeds;
    }

    /**
     * @return how many distinct MONDO URIs this slot contributed, before de-duplication against the other slots
     */
    private int collect( Set<String> seeds, String hql ) {
        @SuppressWarnings("unchecked")
        List<String> rows = sessionFactory.getCurrentSession()
                .createQuery( hql )
                .setParameter( "prefix", MONDO_PREFIX + "%" )
                .list();
        int n = 0;
        for ( String uri : rows ) {
            if ( uri != null && uri.startsWith( MONDO_PREFIX ) ) {
                seeds.add( uri );
                n++;
            }
        }
        return n;
    }
}
