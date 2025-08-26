package ubic.gemma.apps;

import gemma.gsec.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.auditAndSecurity.eventType.MakePrivateEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public class MakeExperimentPrivateCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private SecurityService securityService;

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
        this.auditTrailService.addUpdateEvent( ee, MakePrivateEvent.class, "Made private from command line" );
        addSuccessObject( ee, "Experiment was made private." );
    }
}