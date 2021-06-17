package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayDao;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

@Service
public class BioAssayPersisterImpl extends AbstractPersister<BioAssay> implements BioAssayPersister {

    @Autowired
    private BioAssayDao bioAssayDao;

    @Autowired
    private Persister<BioMaterial> bioMaterialPersister;
    @Autowired
    private Persister<ExternalDatabase> externalDatabasePersister;
    @Autowired
    private FactorValuePersister factorValuePersister;
    @Autowired
    private Persister<ArrayDesign> arrayDesignPersister;

    @Autowired
    public BioAssayPersisterImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public BioAssay persist( BioAssay entity ) {
        return persistBioAssay( entity, null );
    }

    @Override
    public BioAssay persistBioAssay( BioAssay assay, ArrayDesignsForExperimentCache c ) {
        if ( assay == null )
            return null;
        if ( !this.isTransient( assay ) ) {
            return assay;
        }
        AbstractPersister.log.debug( "Persisting " + assay );
        this.fillInBioAssayAssociations( assay, c );

        return bioAssayDao.create( assay );
    }

    @Override
    public void fillInBioAssayAssociations( BioAssay bioAssay, ArrayDesignsForExperimentCache c ) {

        ArrayDesign arrayDesign = bioAssay.getArrayDesignUsed();
        ArrayDesign arrayDesignUsed;
        if ( !this.arrayDesignPersister.isTransient( arrayDesign ) ) {
            arrayDesignUsed = arrayDesign;
        } else if ( c == null || !c.getArrayDesignCache().containsKey( arrayDesign.getShortName() ) ) {
            throw new UnsupportedOperationException( "You must provide the persistent platforms in a cache object" );
        } else {
            arrayDesignUsed = c.getArrayDesignCache().get( arrayDesign.getShortName() );

            if ( arrayDesignUsed == null || arrayDesignUsed.getId() == null ) {
                throw new IllegalStateException( "You must provide the platform in the cache object" );
            }

            arrayDesignUsed = ( ArrayDesign ) this.getSessionFactory().getCurrentSession()
                    .load( ArrayDesign.class, arrayDesignUsed.getId() );

            if ( arrayDesignUsed == null ) {
                throw new IllegalStateException( "No platform matching " + arrayDesign.getShortName() );
            }

            AbstractPersister.log.debug( "Setting platform used for bioassay to " + arrayDesignUsed.getId() );
        }

        assert !this.arrayDesignPersister.isTransient( arrayDesignUsed );

        bioAssay.setArrayDesignUsed( arrayDesignUsed );

        boolean hadFactors = false;
        BioMaterial material = bioAssay.getSampleUsed();
        for ( FactorValue factorValue : material.getFactorValues() ) {
            // Factors are not compositioned in any more, but by association with the ExperimentalFactor.
            this.factorValuePersister.fillInFactorValueAssociations( factorValue );
            this.factorValuePersister.persist( factorValue );
            hadFactors = true;
        }

        if ( hadFactors )
            AbstractPersister.log.debug( "factor values done" );

        // DatabaseEntries are persisted by composition, so we just need to fill in the ExternalDatabase.
        if ( bioAssay.getAccession() != null ) {
            bioAssay.getAccession().setExternalDatabase(
                    this.externalDatabasePersister.persist( bioAssay.getAccession().getExternalDatabase() ) );
            bioAssay.getAccession().setId( null ); // IN CASE we are retrying.
            AbstractPersister.log.debug( "external database done" );
        }

        // BioMaterials
        bioAssay.setSampleUsed( this.bioMaterialPersister.persist( bioAssay.getSampleUsed() ) );

        AbstractPersister.log.debug( "Done with " + bioAssay );

    }
}
