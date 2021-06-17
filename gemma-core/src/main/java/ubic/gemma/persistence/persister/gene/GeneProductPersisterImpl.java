package ubic.gemma.persistence.persister.gene;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.persistence.persister.AbstractPersister;
import ubic.gemma.persistence.persister.ChromosomePersister;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.genome.gene.GeneProductDao;

import java.util.HashMap;
import java.util.Map;

@Service
public class GeneProductPersisterImpl extends AbstractPersister<GeneProduct> implements GeneProductPersister {

    @Autowired
    private GeneProductDao geneProductDao;

    @Autowired
    private Persister<Gene> genePersister;

    @Autowired
    private Persister<DatabaseEntry> databaseEntryPersister;

    @Autowired
    private ChromosomePersister chromosomePersister;

    @Autowired
    private Persister<ExternalDatabase> externalDatabasePersister;

    @Autowired
    public GeneProductPersisterImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public GeneProduct persist( GeneProduct geneProduct ) {
        if ( geneProduct == null )
            return null;
        if ( !this.isTransient( geneProduct ) )
            return geneProduct;

        GeneProduct existing = geneProductDao.find( geneProduct );

        if ( existing != null ) {
            if ( AbstractPersister.log.isDebugEnabled() )
                AbstractPersister.log.debug( geneProduct + " exists, will not update" );
            return existing;
        }

        if ( AbstractPersister.log.isDebugEnabled() )
            AbstractPersister.log.debug( "*** New: " + geneProduct + " *** " );

        this.fillInGeneProductAssociations( geneProduct );

        if ( genePersister.isTransient( geneProduct.getGene() ) ) {
            // this results in the persistence of the gene products, but only if the gene is transient.
            geneProduct.setGene( genePersister.persist( geneProduct.getGene() ) );
        } else {
            geneProduct = geneProductDao.create( geneProduct );
        }

        if ( geneProduct.getId() == null ) {
            return geneProductDao.create( geneProduct );
        }

        return geneProduct;

    }

    /**
     * @param updatedGeneProductInfo information from this is copied onto the 'existing' gene product.
     */
    @Override
    public void updateGeneProduct( GeneProduct existingGeneProduct, GeneProduct updatedGeneProductInfo ) {
        Gene geneForExistingGeneProduct = existingGeneProduct.getGene();
        assert !this.genePersister.isTransient( geneForExistingGeneProduct );

        existingGeneProduct = geneProductDao.thaw( existingGeneProduct );

        // Update all the fields. Note that usually, some of these can't have changed or we wouldn't have even
        // found the 'existing' one (name GI in particular); however, sometimes we are updating this information

        existingGeneProduct.setName( updatedGeneProductInfo.getName() );
        existingGeneProduct.setDescription( updatedGeneProductInfo.getDescription() );
        existingGeneProduct.setNcbiGi( updatedGeneProductInfo.getNcbiGi() );

        this.addAnyNewAccessions( existingGeneProduct, updatedGeneProductInfo );

        existingGeneProduct.setPhysicalLocation( updatedGeneProductInfo.getPhysicalLocation() );
        if ( existingGeneProduct.getPhysicalLocation() != null ) {
            existingGeneProduct.getPhysicalLocation().setChromosome(
                    this.chromosomePersister.persistChromosome( existingGeneProduct.getPhysicalLocation().getChromosome(),
                            geneForExistingGeneProduct.getTaxon() ) );
        }

    }

    @Override
    public GeneProduct persistOrUpdate( GeneProduct geneProduct ) {
        if ( geneProduct == null )
            return null;

        GeneProduct existing;
        if ( geneProduct.getId() != null ) {
            existing = geneProductDao.load( geneProduct.getId() );
        } else {
            existing = geneProductDao.find( geneProduct );
        }

        if ( existing == null ) {
            this.persist( geneProduct );
        }

        this.updateGeneProduct( existing, geneProduct );

        return existing;
    }

    @Override
    public void fillInGeneProductAssociations( GeneProduct geneProduct ) {

        if ( geneProduct.getPhysicalLocation() != null ) {
            geneProduct.getPhysicalLocation().setChromosome(
                    this.chromosomePersister.persistChromosome( geneProduct.getPhysicalLocation().getChromosome(),
                            geneProduct.getGene().getTaxon() ) );
        }

        if ( geneProduct.getAccessions() != null ) {
            for ( DatabaseEntry de : geneProduct.getAccessions() ) {
                de.setExternalDatabase( externalDatabasePersister.persist( de.getExternalDatabase() ) );
            }
        }
    }

    private void addAnyNewAccessions( GeneProduct existing, GeneProduct geneProduct ) {
        Map<String, DatabaseEntry> updatedGpMap = new HashMap<>();
        existing = geneProductDao.thaw( existing );
        for ( DatabaseEntry de : existing.getAccessions() ) {
            updatedGpMap.put( de.getAccession(), de );
        }
        for ( DatabaseEntry de : geneProduct.getAccessions() ) {
            if ( !updatedGpMap.containsKey( de.getAccession() ) ) {
                databaseEntryPersister.persist( de );
                existing.getAccessions().add( de );
            }
        }
    }

}
