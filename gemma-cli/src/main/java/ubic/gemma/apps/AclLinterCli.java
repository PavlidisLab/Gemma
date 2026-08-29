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

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    private List<Long> identifiers = new ArrayList<>();

    private boolean lintPermissions;

    /**
     * Indicate if fixes should be applied.
     */
    private boolean applyFixes;

    @Override
    protected void buildOptions( Options options ) {
        OptionsUtils.addEnumOption( options, "type", "type", "Type of securable entities to lint.", SecurableType.class );
        options.addOption( Option.builder( "identifier" ).longOpt( "identifier" ).hasArgs().valueSeparator( ',' ).type( Long.class )
                .desc( "One or more identifiers (comma-separated) of securable entities to lint. Requires the -type,--type option to be set." ).get() );
        options.addOption( "lintPermissions", "lint-permissions", false, "Lint permissions." );
        options.addOption( "applyFixes", "apply-fixes", false, "Apply fixes to ACLs" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        SecurableType st = OptionsUtils.getEnumOptionValue( commandLine, "type" );
        this.clazz = st != null ? st.getClazz() : null;
        this.identifiers = new ArrayList<>();
        String[] rawIds = commandLine.getOptionValues( "identifier" );
        if ( rawIds != null && rawIds.length > 0 ) {
            if ( clazz == null ) {
                throw new ParseException( "The -type,--type option is required when -identifier,--identifier is set." );
            }
            for ( String raw : rawIds ) {
                this.identifiers.add( Long.parseLong( raw.trim() ) );
            }
        }
        this.lintPermissions = commandLine.hasOption( "lintPermissions" );
        this.applyFixes = commandLine.hasOption( "applyFixes" );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        AclLinterConfig config = AclLinterConfig.builder()
                .lintDanglingIdentities( true )
                .lintSecurablesLackingIdentities( true )
                .lintChildWithoutParent( true )
                .lintChildWithIncorrectParent( true )
                .lintNotChildWithParent( true )
                .lintPermissions( lintPermissions )
                .applyFixes( applyFixes )
                .build();
        Collection<AclLinterService.LintResult> results;
        if ( !identifiers.isEmpty() ) {
            List<AclLinterService.LintResult> acc = new ArrayList<>();
            for ( Long id : identifiers ) {
                acc.addAll( aclLinterService.lintAcls( clazz, id, config ) );
            }
            results = acc;
        } else if ( clazz != null ) {
            results = aclLinterService.lintAcls( clazz, config );
        } else {
            results = aclLinterService.lintAcls( config );
        }
        // Per-type tally of what happened, so a long run ends with a legible summary instead of only
        // the scrolled-past per-row lines. Bucketed by an action derived from the message
        // (created / deleted / missing / dangling / other) and by whether a fix was applied.
        java.util.Map<String, java.util.Map<String, Integer>> tally = new java.util.TreeMap<>();
        int fixes = 0, findings = 0;
        for ( AclLinterService.LintResult result : results ) {
            String o = result.getType().getSimpleName() + " Id=" + result.getIdentifier();
            if ( result.isFixed() ) {
                addSuccessObject( o, result.getMessage() );
                fixes++;
            } else {
                addWarningObject( o, result.getMessage() );
                findings++;
            }
            String action = classifyLintAction( result.getMessage() );
            tally.computeIfAbsent( result.getType().getSimpleName(), k -> new java.util.TreeMap<>() )
                    .merge( action, 1, Integer::sum );
        }

        log.info( "===== lintAcls summary =====" );
        if ( tally.isEmpty() ) {
            log.info( "Nothing to report: every linted type was clean." );
        } else {
            for ( java.util.Map.Entry<String, java.util.Map<String, Integer>> e : tally.entrySet() ) {
                StringBuilder line = new StringBuilder( e.getKey() ).append( ": " );
                boolean first = true;
                for ( java.util.Map.Entry<String, Integer> a : e.getValue().entrySet() ) {
                    if ( !first ) line.append( ", " );
                    line.append( a.getValue() ).append( ' ' ).append( a.getKey() );
                    first = false;
                }
                log.info( line.toString() );
            }
        }
        log.info( applyFixes
                ? ( fixes + " fix(es) applied, " + findings + " left unfixed" )
                : ( findings + " finding(s); re-run with --apply-fixes to act on them" ) );
    }

    /**
     * Coarse action bucket for the summary, read from the {@link AclLinterService.LintResult} message
     * (which may carry a row id, so a substring match rather than equality). Keeps the tally readable
     * without the service having to expose an enum.
     */
    private static String classifyLintAction( String message ) {
        if ( message == null ) return "other";
        String m = message.toLowerCase();
        if ( m.contains( "created" ) ) return "created";
        if ( m.contains( "deleted" ) ) return "deleted";
        if ( m.contains( "lacks an acl identity" ) || m.contains( "lacking" ) ) return "missing";
        if ( m.contains( "no corresponding entity" ) || m.contains( "dangling" ) ) return "dangling";
        if ( m.contains( "parent" ) ) return "parent";
        return "other";
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
