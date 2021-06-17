package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionDao;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

import java.util.ArrayList;
import java.util.List;

@Service
public class BioAssayDimensionPersisterImpl extends AbstractPersister<BioAssayDimension> implements BioAssayDimensionPersister {

    @Autowired
    private BioAssayDimensionDao bioAssayDimensionDao;

    @Autowired
    private BioAssayPersister bioAssayPersister;

    @Autowired
    public BioAssayDimensionPersisterImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public BioAssayDimension persist( BioAssayDimension entity ) {
        return persistBioAssayDimension( entity, null );
    }

    @Override
    public BioAssayDimension persistBioAssayDimension( BioAssayDimension bioAssayDimension,
            ArrayDesignsForExperimentCache c ) {
        if ( bioAssayDimension == null )
            return null;
        if ( !this.isTransient( bioAssayDimension ) )
            return bioAssayDimension;
        AbstractPersister.log.debug( "Persisting bioAssayDimension" );
        List<BioAssay> persistedBioAssays = new ArrayList<>();
        for ( BioAssay bioAssay : bioAssayDimension.getBioAssays() ) {
            assert bioAssay != null;
            bioAssay.setId( null ); // in case of retry.
            persistedBioAssays.add( this.bioAssayPersister.persistBioAssay( bioAssay, c ) );
            if ( persistedBioAssays.size() % 10 == 0 ) {
                AbstractPersister.log.info( "Persisted: " + persistedBioAssays.size() + " bioassays" );
            }
        }
        AbstractPersister.log.debug( "Done persisting " + persistedBioAssays.size() + " bioassays" );
        assert persistedBioAssays.size() > 0;
        bioAssayDimension.setBioAssays( persistedBioAssays );
        bioAssayDimension.setId( null ); // in case of retry.
        return bioAssayDimensionDao.findOrCreate( bioAssayDimension );
    }
}
