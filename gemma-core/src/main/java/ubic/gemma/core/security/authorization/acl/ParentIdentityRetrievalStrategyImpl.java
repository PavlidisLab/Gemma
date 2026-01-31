package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.domain.AclObjectIdentity;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
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
 * Use domain-specific logic to resolve parent ACL identities.
 *
 * @author poirigui
 */
@Service
@CommonsLog
public class ParentIdentityRetrievalStrategyImpl implements ParentIdentityRetrievalStrategy {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Override
    @Transactional(readOnly = true)
    public ObjectIdentity getParentIdentity( Object domainObject ) {
        Assert.isInstanceOf( SecuredChild.class, domainObject, "Domain object must be of type SecuredChild." );
        Assert.notNull( ( ( SecuredChild<?> ) domainObject ).getId(), "Domain object must have a non-null identifier." );

        Class<? extends Securable> parentType;
        Long parentIdentifier;
        if ( ( ( SecuredChild<?> ) domainObject ).getSecurityOwner() != null ) {
            SecuredChild<?> sc = ( SecuredChild<?> ) domainObject;
            // this is necessary because we rely on the class name for querying
            //noinspection unchecked
            parentType = Hibernate.getClass( sc.getSecurityOwner() );
            parentIdentifier = sc.getSecurityOwner().getId();
        } else if ( domainObject instanceof ExperimentalDesign ) {
            ExperimentalDesign design = ( ExperimentalDesign ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByDesign( design );
        } else if ( domainObject instanceof ExperimentalFactor ) {
            ExperimentalFactor factor = ( ExperimentalFactor ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByFactor( factor );
        } else if ( domainObject instanceof FactorValue ) {
            FactorValue factor = ( FactorValue ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByFactorValue( factor );
        } else if ( domainObject instanceof BioAssay ) {
            BioAssay ba = ( BioAssay ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByBioAssay( ba, true );
        } else if ( domainObject instanceof BioMaterial ) {
            BioMaterial bm = ( BioMaterial ) domainObject;
            Collection<Long> eeIds = expressionExperimentService.findIdsByBioMaterial( bm, true );
            parentType = ExpressionExperiment.class;
            if ( eeIds.size() == 1 ) {
                parentIdentifier = eeIds.iterator().next();
            } else if ( eeIds.size() > 1 ) {
                log.warn( "More than one ExpressionExperiment refer to " + bm + ", cannot pick its parent ACL identity." );
                parentIdentifier = null;
            } else {
                log.warn( "Could not find an ExpressionExperiment associated to " + bm + "." );
                parentIdentifier = null;
            }
        } else if ( domainObject instanceof MeanVarianceRelation ) {
            MeanVarianceRelation mvr = ( MeanVarianceRelation ) domainObject;
            parentType = ExpressionExperiment.class;
            parentIdentifier = expressionExperimentService.findIdByMeanVarianceRelation( mvr );
        } else {
            throw new UnsupportedOperationException( "Resolving parent identity for " + domainObject + " is not supported." );
        }

        if ( parentIdentifier != null ) {
            return ( AclObjectIdentity ) sessionFactory.getCurrentSession()
                    .createQuery( "select aoi from AclObjectIdentity aoi where aoi.type = :type and aoi.identifier = :identifier" )
                    .setParameter( "type", parentType.getName() )
                    .setParameter( "identifier", parentIdentifier )
                    .uniqueResult();
        } else {
            log.warn( String.format( "Could not locate parent identifier for %s.", domainObject ) );
            return null;
        }
    }
}
