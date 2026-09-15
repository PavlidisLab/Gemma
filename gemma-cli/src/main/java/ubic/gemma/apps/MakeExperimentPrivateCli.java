package ubic.gemma.apps;

import ubic.gemma.cli.audit.CliExpressionExperimentAuditService;
import ubic.gemma.core.security.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class MakeExperimentPrivateCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private CliExpressionExperimentAuditService cliExpressionExperimentAuditService;

    @Override
    public String getCommandName() {
        return "makePrivate";
    }

    @Override
    public String getShortDesc() {
        return "Make experiments private";
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        securityService.makePrivate( ee );
        cliExpressionExperimentAuditService.recordMadePrivate( ee, "Made private from command line" );
        addSuccessObject( ee, "Experiment was made private." );
    }
}