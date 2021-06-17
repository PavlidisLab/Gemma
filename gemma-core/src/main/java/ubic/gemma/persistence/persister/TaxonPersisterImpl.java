package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonDao;

import java.util.HashMap;
import java.util.Map;

@Service
public class TaxonPersisterImpl extends AbstractPersister<Taxon> implements TaxonPersister {

    private final Map<Object, Taxon> seenTaxa = new HashMap<>();

    @Autowired
    private TaxonDao taxonDao;

    @Autowired
    public TaxonPersisterImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public Taxon persist( Taxon taxon ) {
        if ( taxon == null )
            return null;
        if ( !this.isTransient( taxon ) )
            return taxon;

        // Avoid trips to the database to get the taxon.
        String scientificName = taxon.getScientificName();
        String commonName = taxon.getCommonName();
        Integer ncbiId = taxon.getNcbiId();

        if ( ncbiId != null && seenTaxa.containsKey( ncbiId ) ) {
            return seenTaxa.get( ncbiId );
        } else if ( scientificName != null && seenTaxa.containsKey( scientificName.toLowerCase() ) ) {
            return seenTaxa.get( scientificName.toLowerCase() );
        } else if ( commonName != null && seenTaxa.containsKey( commonName.toLowerCase() ) ) {
            return seenTaxa.get( commonName.toLowerCase() );
        } else {
            Taxon fTaxon = taxonDao.findOrCreate( taxon );
            assert fTaxon != null;
            assert fTaxon.getId() != null;

            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log.debug( "Fetched or created taxon " + fTaxon );

            if ( fTaxon.getScientificName() != null ) {
                seenTaxa.put( fTaxon.getScientificName().toLowerCase(), fTaxon );
            }
            if ( fTaxon.getCommonName() != null ) {
                seenTaxa.put( fTaxon.getCommonName().toLowerCase(), fTaxon );
            }
            if ( fTaxon.getNcbiId() != null ) {
                seenTaxa.put( fTaxon.getNcbiId(), fTaxon );
            }

            return fTaxon;
        }
    }
}
