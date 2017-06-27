package ubic.gemma.persistence.service.genome.sequenceAnalysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.persistence.service.AbstractService;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class AnnotationAssociationServiceImpl extends AbstractService<AnnotationAssociation>
        implements AnnotationAssociationService {

    private final AnnotationAssociationDao annotationAssociationDao;

    @Autowired
    public AnnotationAssociationServiceImpl( AnnotationAssociationDao annotationAssociationDao ) {
        super( annotationAssociationDao );
        this.annotationAssociationDao = annotationAssociationDao;
    }

    @Override
    public Collection<AnnotationAssociation> find( BioSequence bioSequence ) {
        return this.annotationAssociationDao.find( bioSequence );
    }

    @Override
    public Collection<AnnotationAssociation> find( Gene gene ) {
        return this.annotationAssociationDao.find( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( AnnotationAssociation annotationAssociation ) {
        this.annotationAssociationDao.thaw( annotationAssociation );

    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( Collection<AnnotationAssociation> anCollection ) {
        this.annotationAssociationDao.thaw( anCollection );

    }

    /**
     * Remove root terms, like "molecular_function", "biological_process" and "cellular_component" Also removes any null
     * objects.
     *
     * @return cleaned up associations
     */
    @Override
    @Transactional
    public Collection<AnnotationValueObject> removeRootTerms( Collection<AnnotationValueObject> associations ) {
        Collection<AnnotationValueObject> cleanedUp = new ArrayList<>();
        for ( AnnotationValueObject avo : associations ) {
            String term = avo.getTermName();
            if ( term == null )
                continue;
            if ( !( term.equals( "molecular_function" ) || term.equals( "biological_process" ) || term
                    .equals( "cellular_component" ) ) ) {
                cleanedUp.add( avo );
            }
        }
        return cleanedUp;
    }
}
