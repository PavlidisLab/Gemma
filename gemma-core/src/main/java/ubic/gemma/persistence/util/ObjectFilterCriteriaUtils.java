package ubic.gemma.persistence.util;

import com.google.common.base.Strings;
import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.util.SecurityUtil;
import org.hibernate.Criteria;
import org.hibernate.criterion.*;
import org.hibernate.type.StringType;
import org.springframework.security.acls.domain.BasePermission;

import java.util.Collection;
import java.util.List;

/**
 * Utilities for integrating {@link ObjectFilter} with Hibernate {@link Criteria} API.
 */
public class ObjectFilterCriteriaUtils {

    /**
     * Form a restriction clause using a {@link Criterion}.
     * @see ObjectFilterQueryUtils#formRestrictionClause(List)
     * @param objectFilters the filters to use to create the clause
     * @return a restriction clause that can be appended to a {@link Criteria} using {@link Criteria#add(Criterion)}
     */
    public static Criterion formRestrictionClause( List<ObjectFilter[]> objectFilters ) {
        Conjunction c = Restrictions.conjunction();
        if ( objectFilters == null || objectFilters.isEmpty() )
            return c;
        for ( ObjectFilter[] filters : objectFilters ) {
            if ( filters == null || filters.length == 0 )
                continue;
            Disjunction d = Restrictions.disjunction();
            for ( ObjectFilter filter : filters ) {
                d.add( formRestrictionClause( filter ) );
            }
            c.add( d );
        }
        return c;
    }

    private static Criterion formRestrictionClause( ObjectFilter filter ) {
        switch ( filter.getOperator() ) {
            case is:
                return Restrictions.eq( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case isNot:
                return Restrictions.ne( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case like:
                return Restrictions.like( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case lessThan:
                return Restrictions.lt( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case greaterThan:
                return Restrictions.gt( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case lessOrEq:
                return Restrictions.le( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case greaterOrEq:
                return Restrictions.ge( ObjectFilterQueryUtils.formPropertyName( filter ), filter.getRequiredValue() );
            case in:
                return Restrictions.in( ObjectFilterQueryUtils.formPropertyName( filter ), ( Collection<?> ) filter.getRequiredValue() );
            default:
                throw new IllegalStateException( "Unexpected operator for filter: " + filter.getOperator() );
        }
    }

    /**
     * Form a restriction clause for ACL.
     *
     * @see AclQueryUtils#formAclRestrictionClause()
     */
    public static Criterion formAclRestrictionClause( String alias, String aoiType ) {
        if ( Strings.isNullOrEmpty( alias ) || Strings.isNullOrEmpty( aoiType ) )
            throw new IllegalArgumentException( "Alias and aoiType can not be empty." );

        DetachedCriteria dc = DetachedCriteria.forClass( AclObjectIdentity.class, "aoi" )
                .setProjection( Projections.property( "aoi.identifier" ) )
                .add( Restrictions.eqProperty( "aoi.identifier", alias + ".id" ) )
                .add( Restrictions.eq( "aoi.type", aoiType ) );
        if ( SecurityUtil.isUserAdmin() ) {
            dc.createAlias( "aoi.ownerSid", "sid", Criteria.INNER_JOIN );
        } else {
            dc.createAlias( "aoi.entries", "ace", Criteria.INNER_JOIN );
        }

        String userName = SecurityUtil.getCurrentUsername();
        int readMask = BasePermission.READ.getMask();
        int writeMask = BasePermission.WRITE.getMask();
        if ( !SecurityUtil.isUserAnonymous() ) {
            if ( !SecurityUtil.isUserAdmin() ) {
                dc.add( Restrictions.disjunction()
                        // user own the object
                        .add( Restrictions.eq( "sid.principal", userName ) )
                        // user has specific rights to the object
                        .add( Restrictions.conjunction()
                                .add( Restrictions.sqlRestriction( "ace.sid.id in (" + AclQueryUtils.CURRENT_USER_SIDS_SQL + ")", userName, StringType.INSTANCE ) )
                                .add( Restrictions.in( "ace.mask", new Object[] { readMask, writeMask } ) ) )
                        // the object is public
                        .add( Restrictions.conjunction()
                                .add( Restrictions.eq( "ace.sid.id", 4L ) )
                                .add( Restrictions.eq( "ace.mask", readMask ) ) ) );
            } else {
                //
            }
        } else {
            // the object is public
            dc.add( Restrictions.conjunction()
                    .add( Restrictions.eq( "ace.sid.id", 4L ) )
                    .add( Restrictions.eq( "ace.mask", readMask ) ) );
        }

        return Subqueries.propertyIn( alias + ".id", dc );
    }
}
