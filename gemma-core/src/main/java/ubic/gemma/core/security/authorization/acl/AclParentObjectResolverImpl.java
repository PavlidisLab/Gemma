package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.ParentIdentityRetrievalStrategy;
import gemma.gsec.acl.domain.AclObjectIdentity;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Collection;

/**
 * Use domain-specific logic to resolve parent ACL identities
 *
 * @author poirigui
 */
@Service
@CommonsLog
public class AclParentObjectResolverImpl implements ParentIdentityRetrievalStrategy {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Override
    @Transactional(readOnly = true)
    public ObjectIdentity locateParentIdentity( ObjectIdentity aoi ) {
        //noinspection unchecked
        Class<? extends SecuredChild<?>> clazz = sessionFactory.getClassMetadata( aoi.getType() ).getMappedClass();
        Class<? extends Securable> parentType;
        Long parentIdentifier;
        if ( ExperimentalDesign.class.isAssignableFrom( clazz ) ) {
            parentType = ExpressionExperiment.class;
            ExperimentalDesign design = ( ExperimentalDesign ) sessionFactory.getCurrentSession()
                    .get( ExperimentalDesign.class, aoi.getIdentifier() );
            if ( design == null ) {
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByDesign( design );
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( ExperimentalFactor.class.isAssignableFrom( clazz ) ) {
            parentType = ExpressionExperiment.class;
            ExperimentalFactor factor = ( ExperimentalFactor ) sessionFactory.getCurrentSession()
                    .get( ExperimentalFactor.class, aoi.getIdentifier() );
            if ( factor == null ) {
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByFactor( factor );
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( FactorValue.class.isAssignableFrom( clazz ) ) {
            parentType = ExpressionExperiment.class;
            FactorValue factor = ( FactorValue ) sessionFactory.getCurrentSession()
                    .get( FactorValue.class, aoi.getIdentifier() );
            if ( factor == null ) {
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByFactorValue( factor );
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( BioAssay.class.isAssignableFrom( clazz ) ) {
            parentType = ExpressionExperiment.class;
            BioAssay ba = ( BioAssay ) sessionFactory.getCurrentSession()
                    .get( BioAssay.class, aoi.getIdentifier() );
            if ( ba == null ) {
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByBioAssay( ba, true );
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( BioMaterial.class.isAssignableFrom( clazz ) ) {
            parentType = ExpressionExperiment.class;
            BioMaterial bm = ( BioMaterial ) sessionFactory.getCurrentSession()
                    .get( BioMaterial.class, aoi.getIdentifier() );
            if ( bm == null ) {
                return null;
            }
            Collection<ExpressionExperiment> ees = expressionExperimentService.findByBioMaterial( bm, true );
            if ( ees.size() == 1 ) {
                parentIdentifier = ees.iterator().next().getId();
            } else if ( ees.size() > 1 ) {
                log.warn( "More than one ExpressionExperiment refer to " + bm + ", cannot pick its parent ACL identity." );
                parentIdentifier = null;
            } else {
                parentIdentifier = null;
            }
        } else if ( MeanVarianceRelation.class.isAssignableFrom( clazz ) ) {
            MeanVarianceRelation mvr = ( MeanVarianceRelation ) sessionFactory.getCurrentSession()
                    .get( MeanVarianceRelation.class, aoi.getIdentifier() );
            if ( mvr == null ) {
                return null;
            }
            ExpressionExperiment ee = expressionExperimentService.findByMeanVarianceRelation( mvr );
            parentType = ExpressionExperiment.class;
            parentIdentifier = ee != null ? ee.getId() : null;
        } else {
            // try automated!
            SecuredChild<?> sc = ( SecuredChild<?> ) sessionFactory.getCurrentSession()
                    .get( clazz, aoi.getIdentifier() );
            if ( sc == null ) {
                return null;
            }
            if ( sc.getSecurityOwner() != null ) {
                // this is necessary because we rely on the class name for querying
                //noinspection unchecked
                parentType = Hibernate.getClass( sc.getSecurityOwner().getClass() );
                parentIdentifier = sc.getSecurityOwner().getId();
            } else {
                log.warn( "Cannot resolve parent ACL identity for " + aoi.getType() + "." );
                parentType = null;
                parentIdentifier = null;
            }
        }

        if ( parentIdentifier != null ) {
            return ( AclObjectIdentity ) sessionFactory.getCurrentSession()
                    .createQuery( "select aoi from AclObjectIdentity aoi where aoi.type = :type and aoi.identifier = :identifier" )
                    .setParameter( "type", parentType.getName() )
                    .setParameter( "identifier", parentIdentifier )
                    .uniqueResult();
        } else {
            return null;
        }
    }
}
