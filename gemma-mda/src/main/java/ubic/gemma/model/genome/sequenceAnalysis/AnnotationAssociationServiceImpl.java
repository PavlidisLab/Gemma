package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

public class AnnotationAssociationServiceImpl implements AnnotationAssociationService {

    AnnotationAssociationDao annotationAssociationDao;

    /**
     * @return the annotationAssociationDao
     */
    public AnnotationAssociationDao getAnnotationAssociationDao() {
        return annotationAssociationDao;
    }

    /**
     * @param annotationAssociationDao the annotationAssociationDao to set
     */
    public void setAnnotationAssociationDao( AnnotationAssociationDao annotationAssociationDao ) {
        this.annotationAssociationDao = annotationAssociationDao;
    }

    public AnnotationAssociation create( AnnotationAssociation annotationAssociation ) {
        return this.getAnnotationAssociationDao().create( annotationAssociation );
    }

    public Collection<AnnotationAssociation> create( Collection<AnnotationAssociation> anCollection ) {
        return this.getAnnotationAssociationDao().create( anCollection );
    }

    public Collection<AnnotationAssociation> find( Gene gene ) {
        return this.getAnnotationAssociationDao().find( gene );
    }

    public Collection<AnnotationAssociation> find( BioSequence bioSequence ) {
        return this.getAnnotationAssociationDao().find( bioSequence );
    }

    public AnnotationAssociation load( Long id ) {
        return this.getAnnotationAssociationDao().load( id );
    }

    public Collection<AnnotationAssociation> load( Collection<Long> ids ) {
        return this.getAnnotationAssociationDao().load( ids );
    }

    public void remove( AnnotationAssociation annotationAssociation ) {
        this.getAnnotationAssociationDao().remove( annotationAssociation );

    }

    public void remove( Collection<AnnotationAssociation> anCollection ) {
        this.getAnnotationAssociationDao().remove( anCollection );

    }

    public void thaw( AnnotationAssociation annotationAssociation ) {
        this.getAnnotationAssociationDao().thaw( annotationAssociation );

    }

    public void thaw( Collection<AnnotationAssociation> anCollection ) {
        this.getAnnotationAssociationDao().thaw( anCollection );

    }

    public void update( AnnotationAssociation annotationAssociation ) {
        this.getAnnotationAssociationDao().update( annotationAssociation );

    }

    public void update( Collection<AnnotationAssociation> anCollection ) {
        this.getAnnotationAssociationDao().update( anCollection );

    }
}
