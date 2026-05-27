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

    @Transactional(readOnly = true)
    public Set<String> resolveCorpusSeeds() {
        long start = System.currentTimeMillis();
        @SuppressWarnings("unchecked")
        List<String> rows = sessionFactory.getCurrentSession()
                .createQuery( "select distinct c.valueUri from Characteristic c "
                        + "where c.valueUri like :prefix" )
                .setParameter( "prefix", MONDO_PREFIX + "%" )
                .list();
        Set<String> seeds = new HashSet<>( rows.size() );
        for ( String uri : rows ) {
            if ( uri != null && uri.startsWith( MONDO_PREFIX ) ) {
                seeds.add( uri );
            }
        }
        log.info( "Resolved {} corpus MONDO seeds in {} ms.",
                seeds.size(), System.currentTimeMillis() - start );
        return seeds;
    }
}
