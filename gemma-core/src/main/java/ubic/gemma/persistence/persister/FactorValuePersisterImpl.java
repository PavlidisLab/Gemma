package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.measurement.Unit;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueDao;

@Service
public class FactorValuePersisterImpl extends AbstractPersister<FactorValue> implements FactorValuePersister {

    @Autowired
    private FactorValueDao factorValueDao;

    @Autowired
    private ExperimentalFactorPersister experimentalFactorPersister;

    @Autowired
    private Persister<Unit> unitPersister;

    @Autowired
    public FactorValuePersisterImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    /**
     * If we get here first (e.g., via bioAssay->bioMaterial) we have to override the cascade.
     */
    @Override
    @Transactional
    public FactorValue persist( FactorValue factorValue ) {
        if ( factorValue == null )
            return null;
        if ( !this.isTransient( factorValue ) )
            return null;
        if ( this.experimentalFactorPersister.isTransient( factorValue.getExperimentalFactor() ) ) {
            throw new IllegalArgumentException(
                    "You must fill in the experimental factor before persisting a factorvalue" );
        }
        this.fillInFactorValueAssociations( factorValue );
        return factorValueDao.findOrCreate( factorValue );
    }

    @Override
    public void fillInFactorValueAssociations( FactorValue factorValue ) {

        this.experimentalFactorPersister.fillInExperimentalFactorAssociations( factorValue.getExperimentalFactor() );

        factorValue.setExperimentalFactor( this.experimentalFactorPersister.persist( factorValue.getExperimentalFactor() ) );

        if ( factorValue.getCharacteristics().size() > 0 ) {
            if ( factorValue.getMeasurement() != null ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
        } else if ( factorValue.getValue() != null ) {
            if ( factorValue.getMeasurement() != null || factorValue.getCharacteristics().size() > 0 ) {
                throw new IllegalStateException(
                        "FactorValue can only have one of a value, ontology entry, or measurement." );
            }
        }

        // measurement will cascade, but not unit.
        if ( factorValue.getMeasurement() != null && factorValue.getMeasurement().getUnit() != null ) {
            factorValue.getMeasurement().setUnit( this.unitPersister.persist( factorValue.getMeasurement().getUnit() ) );
        }

    }
}
