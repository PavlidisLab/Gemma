package ubic.gemma.apps;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.security.authorization.acl.AclLinterConfig;
import ubic.gemma.core.security.authorization.acl.AclLinterService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.common.auditAndSecurity.JobInfo;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.gene.GeneSet;

import javax.annotation.Nullable;
import java.util.Collection;

import static ubic.gemma.cli.util.OptionsUtils.*;

/**
 * @author poirigui
 */
public class AclLinterCli extends AbstractAuthenticatedCLI {

    @Getter
    @AllArgsConstructor
    private enum SecurableType {
        PLATFORM( ArrayDesign.class ),
        DATASET( ExpressionExperiment.class ),
        DATASET_SUBSET( ExpressionExperimentSubSet.class ),
        DATASET_GROUP( ExpressionExperimentSet.class ),
        EXTERNAL_DATABASE( ExternalDatabase.class ),
        EXPERIMENTAL_FACTOR( ExperimentalFactor.class ),
        EXPERIMENTAL_DESIGN( ExperimentalDesign.class ),
        FACTOR_VALUE( FactorValue.class ),
        ASSAY( BioAssay.class ),
        SAMPLE( BioMaterial.class ),
        COEXPRESSION_ANALYSIS( CoexpressionAnalysis.class ),
        SAMPLE_COEXPRESSION_ANALYSIS( SampleCoexpressionAnalysis.class ),
        DIFFERENTIAL_EXPRESSION_ANALYSIS( DifferentialExpressionAnalysis.class ),
        DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_SET( ExpressionAnalysisResultSet.class ),
        GENE_DIFFERENTIAL_EXPRESSION_META_ANALYSIS( GeneDifferentialExpressionMetaAnalysis.class ),
        PRINCIPAL_COMPONENT_ANALYSIS( ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis.class ),
        USER_GROUP( ubic.gemma.model.common.auditAndSecurity.UserGroup.class ),
        PROTOCOL( Protocol.class ),
        MEAN_VARIANCE_RELATION( MeanVarianceRelation.class ),
        GENE_GROUP( GeneSet.class ),
        JOB_INFO( JobInfo.class );
        private final Class<? extends Securable> clazz;
    }

    @Autowired
    private AclLinterService aclLinterService;

    private Class<? extends Securable> clazz;

    private Long identifier;

    private boolean lintPermissions;

    /**
     * Indicate if fixes should be applied.
     */
    private boolean applyFixes;

    @Override
    protected void buildOptions( Options options ) {
        OptionsUtils.addEnumOption( options, "type", "type", "Type of securable entities to lint.", SecurableType.class );
        options.addOption( Option.builder( "identifier" ).longOpt( "identifier" ).hasArg().type( Long.class )
                .desc( "Identifier of the securable entity to lint. Requires the -type,--type option to be set." ).get() );
        options.addOption( "lintPermissions", "lint-permissions", false, "Lint permissions." );
        options.addOption( "applyFixes", "apply-fixes", false, "Apply fixes to ACLs" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        SecurableType st = OptionsUtils.getEnumOptionValue( commandLine, "type" );
        this.clazz = st != null ? st.getClazz() : null;
        this.identifier = getParsedOptionValue( commandLine, "identifier",
                requires( toBeSet( "type" ) ) );
        this.lintPermissions = commandLine.hasOption( "lintPermissions" );
        this.applyFixes = commandLine.hasOption( "applyFixes" );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        AclLinterConfig config = AclLinterConfig.builder()
                .lintDanglingIdentities( true )
                .lintSecurablesLackingIdentities( true )
                .lintChildWithoutParent( true )
                .lintNotChildWithParent( true )
                .lintPermissions( lintPermissions )
                .applyFixes( applyFixes )
                .build();
        Collection<AclLinterService.LintResult> results;
        if ( identifier != null ) {
            results = aclLinterService.lintAcls( clazz, identifier, config );
        } else if ( clazz != null ) {
            results = aclLinterService.lintAcls( clazz, config );
        } else {
            results = aclLinterService.lintAcls( config );
        }
        for ( AclLinterService.LintResult result : results ) {
            String o = result.getType().getSimpleName() + " Id=" + result.getIdentifier();
            if ( result.isFixed() ) {
                addSuccessObject( o, result.getProblem() );
            } else {
                addWarningObject( o, result.getProblem() );
            }
        }
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "lintAcls";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.SYSTEM;
    }
}
