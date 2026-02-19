package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationByAssociationCollectionFilteringProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import ubic.gemma.model.analysis.expression.diff.*;

import java.util.List;

/**
 * Filter collections of {@link DifferentialExpressionAnalysisResult} by the {@link ExpressionAnalysisResultSet} they
 * belong to.
 *
 * @author poirigui
 */
public class AclEntryAfterInvocationDifferentialExpressionAnalysisResultCollectionByResultSetFilteringProvider extends AclEntryAfterInvocationByAssociationCollectionFilteringProvider {

    public AclEntryAfterInvocationDifferentialExpressionAnalysisResultCollectionByResultSetFilteringProvider( AclService aclService, String processConfigAttribute, List<Permission> requirePermission ) {
        super( aclService, processConfigAttribute, requirePermission );
    }

    @Override
    protected Object getActualDomainObject( Object targetDomainObject ) {
        if ( targetDomainObject instanceof DifferentialExpressionAnalysisResult ) {
            return ( ( DifferentialExpressionAnalysisResult ) targetDomainObject ).getResultSet();
        } else if ( targetDomainObject instanceof DifferentialExpressionAnalysisResultValueObject ) {
            return new DifferentialExpressionAnalysisResultSetValueObject( ( ( DifferentialExpressionAnalysisResultValueObject ) targetDomainObject ).getResultSetId() );
        } else if ( targetDomainObject instanceof DifferentialExpressionValueObject ) {
            return new DifferentialExpressionAnalysisResultSetValueObject( ( ( DifferentialExpressionValueObject ) targetDomainObject ).getResultSetId() );
        } else {
            throw new IllegalArgumentException( "Unsupported domain object type: " + targetDomainObject.getClass().getName() );
        }
    }
}
