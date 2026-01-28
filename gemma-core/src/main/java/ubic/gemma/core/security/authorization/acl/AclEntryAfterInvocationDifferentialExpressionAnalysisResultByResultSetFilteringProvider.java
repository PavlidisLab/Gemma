package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationByAssociationCollectionFilteringProvider;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultValueObject;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;

import java.util.List;

/**
 * Filter analysis results by the result set they belong to.
 *
 * @author poirigui
 */
public class AclEntryAfterInvocationDifferentialExpressionAnalysisResultByResultSetFilteringProvider extends AclEntryAfterInvocationByAssociationCollectionFilteringProvider {

    public AclEntryAfterInvocationDifferentialExpressionAnalysisResultByResultSetFilteringProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, "AFTER_ACL_DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_COLLECTION_READ", requirePermission );
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
