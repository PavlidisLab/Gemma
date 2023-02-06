package ubic.gemma.persistence.util;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.model.Securable;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.hibernate.type.StringType;
import org.springframework.security.acls.domain.BasePermission;

/**
 * Utilities for integrating ACLs with Hibernate {@link Criteria} API.
 * @author poirigui
 */
public class AclCriteriaUtils {

    /**
     * Form a restriction clause for ACL.
     *
     * @see AclQueryUtils#formAclRestrictionClause()
     */
    public static Criterion formAclRestrictionClause( String alias, Class<? extends Securable> aoiType ) {
        if ( StringUtils.isBlank( alias ) )
            throw new IllegalArgumentException( "Alias cannot be empty." );

        DetachedCriteria dc = DetachedCriteria.forClass( AclObjectIdentity.class, "aoi" )
                .setProjection( Projections.property( "aoi.identifier" ) )
                .add( Restrictions.eqProperty( "aoi.identifier", alias + ".id" ) )
                .add( Restrictions.eq( "aoi.type", aoiType.getCanonicalName() ) );
        if ( SecurityUtil.isUserAdmin() ) {
            dc.createAlias( "aoi.ownerSid", "sid" );
        } else {
            dc.createAlias( "aoi.entries", "ace" );
        }

        String userName = SecurityUtil.getCurrentUsername();
        int readMask = BasePermission.READ.getMask();
        int writeMask = BasePermission.WRITE.getMask();
        if ( SecurityUtil.isUserAnonymous() ) {
            // the object is public
            dc.add( Restrictions.conjunction()
                    .add( Restrictions.eq( "ace.sid.id", 4L ) )
                    .add( Restrictions.eq( "ace.mask", readMask ) ) );
        } else {
            if ( SecurityUtil.isUserAdmin() ) {
                // no restriction applies
            } else {
                dc.add( Restrictions.disjunction()
                        // user own the object
                        .add( Restrictions.eq( "sid.principal", userName ) )
                        // user has specific rights to the object
                        .add( Restrictions.conjunction()
                                .add( Restrictions.sqlRestriction( "ace.sid.id in (" + AclQueryUtils.CURRENT_USER_SIDS_HQL + ")", userName, StringType.INSTANCE ) )
                                .add( Restrictions.in( "ace.mask", new Object[] { readMask, writeMask } ) ) )
                        // the object is public
                        .add( Restrictions.conjunction()
                                .add( Restrictions.eq( "ace.sid.id", 4L ) )
                                .add( Restrictions.eq( "ace.mask", readMask ) ) ) );
            }
        }

        return Subqueries.propertyIn( alias + ".id", dc );
    }
}
