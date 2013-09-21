package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

@Service
public class AnnotationAssociationServiceImpl implements AnnotationAssociationService {

    @Autowired
    AnnotationAssociationDao annotationAssociationDao;

    @Override
    @Transactional
    public AnnotationAssociation create( AnnotationAssociation annotationAssociation ) {
        return this.getAnnotationAssociationDao().create( annotationAssociation );
    }

    @Override
    @Transactional
    public Collection<AnnotationAssociation> create( Collection<AnnotationAssociation> anCollection ) {
        return this.getAnnotationAssociationDao().create( anCollection );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<AnnotationAssociation> find( BioSequence bioSequence ) {
        return this.getAnnotationAssociationDao().find( bioSequence );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<AnnotationAssociation> find( Gene gene ) {
        return this.getAnnotationAssociationDao().find( gene );
    }

    /**
     * @return the annotationAssociationDao
     */
    public AnnotationAssociationDao getAnnotationAssociationDao() {
        return annotationAssociationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<AnnotationAssociation> load( Collection<Long> ids ) {
        return this.getAnnotationAssociationDao().load( ids );
    }

    @Override
    @Transactional(readOnly = true)
    public AnnotationAssociation load( Long id ) {
        return this.getAnnotationAssociationDao().load( id );
    }

    @Override
    @Transactional
    public void remove( AnnotationAssociation annotationAssociation ) {
        this.getAnnotationAssociationDao().remove( annotationAssociation );

    }

    @Override
    @Transactional
    public void remove( Collection<AnnotationAssociation> anCollection ) {
        this.getAnnotationAssociationDao().remove( anCollection );

    }

    /**
     * @param annotationAssociationDao the annotationAssociationDao to set
     */
    public void setAnnotationAssociationDao( AnnotationAssociationDao annotationAssociationDao ) {
        this.annotationAssociationDao = annotationAssociationDao;
    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( AnnotationAssociation annotationAssociation ) {
        this.getAnnotationAssociationDao().thaw( annotationAssociation );

    }

    @Override
    @Transactional(readOnly = true)
    public void thaw( Collection<AnnotationAssociation> anCollection ) {
        this.getAnnotationAssociationDao().thaw( anCollection );

    }

    @Override
    @Transactional
    public void update( AnnotationAssociation annotationAssociation ) {
        this.getAnnotationAssociationDao().update( annotationAssociation );

    }

    @Override
    @Transactional
    public void update( Collection<AnnotationAssociation> anCollection ) {
        this.getAnnotationAssociationDao().update( anCollection );

    }

    /**
     * Remove root terms, like "molecular_function", "biological_process" and "cellular_component" Also removes any null
     * objects.
     * 
     * @param associations
     * @return cleaned up associations
     */
    @Override
    @Transactional
    public Collection<AnnotationValueObject> removeRootTerms( Collection<AnnotationValueObject> associations ) {
        Collection<AnnotationValueObject> cleanedUp = new ArrayList<AnnotationValueObject>();
        for ( Iterator<AnnotationValueObject> it = associations.iterator(); it.hasNext(); ) {
            AnnotationValueObject avo = it.next();
            String term = avo.getTermName();
            if ( term == null ) continue;
            if ( !( term.equals( "molecular_function" ) || term.equals( "biological_process" ) || term
                    .equals( "cellular_component" ) ) ) {
                cleanedUp.add( avo );
            }
        }
        return cleanedUp;
    }
}
