package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorDao;

import java.util.Collection;

@Service
public class ExperimentalFactorPersisterImpl extends AbstractPersister<ExperimentalFactor> implements ExperimentalFactorPersister {

    @Autowired
    private ExperimentalFactorDao experimentalFactorDao;

    @Autowired
    private Persister<Characteristic> characteristicPersister;

    @Autowired
    private Persister<ExperimentalDesign> experimentalDesignPersister;

    @Autowired
    public ExperimentalFactorPersisterImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    /**
     * Note that this uses 'create', not 'findOrCreate'.
     */
    @Override
    @Transactional
    public ExperimentalFactor persist( ExperimentalFactor experimentalFactor ) {
        if ( !this.isTransient( experimentalFactor ) || experimentalFactor == null )
            return experimentalFactor;
        assert experimentalFactor.getType() != null;
        this.fillInExperimentalFactorAssociations( experimentalFactor );

        // in case of retry
        Characteristic category = experimentalFactor.getCategory();
        if ( this.characteristicPersister.isTransient( category ) ) {
            category.setId( null );
        }

        assert ( !this.experimentalDesignPersister.isTransient( experimentalFactor.getExperimentalDesign() ) );
        return experimentalFactorDao.create( experimentalFactor );
    }

    @Override
    public void fillInExperimentalFactorAssociations( ExperimentalFactor experimentalFactor ) {
        if ( experimentalFactor == null )
            return;
        if ( !this.isTransient( experimentalFactor ) )
            return;

        Collection<Characteristic> annotations = experimentalFactor.getAnnotations();
        for ( Characteristic c : annotations ) {
            // in case of retry.
            c.setId( null );
        }

        this.characteristicPersister.persistCollectionElements( annotations );
    }
}
