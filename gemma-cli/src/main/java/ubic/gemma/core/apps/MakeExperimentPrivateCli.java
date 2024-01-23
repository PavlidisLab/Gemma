package ubic.gemma.core.apps;

import gemma.gsec.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.common.auditAndSecurity.eventType.MakePrivateEvent;
import ubic.gemma.model.expression.experiment.BioAssaySet;

public class MakeExperimentPrivateCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private SecurityService securityService;

    @Override
    public String getCommandName() {
        return "makePrivate";
    }

    @Override
    protected void doWork() throws Exception {
        for ( BioAssaySet ee : this.expressionExperiments ) {
            try {
                securityService.makePrivate( ee );
                this.auditTrailService.addUpdateEvent( ee, MakePrivateEvent.class, "Made private from command line" );
                addSuccessObject( ee );
            } catch ( Exception e ) {
                addErrorObject( ee, e );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Make experiments private";
    }
}