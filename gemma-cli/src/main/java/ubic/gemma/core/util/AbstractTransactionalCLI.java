package ubic.gemma.core.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public abstract class AbstractTransactionalCLI extends AbstractCLI {

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    public final int executeCommand( String[] args ) {
        return new TransactionTemplate( platformTransactionManager ).execute( ( status ) -> {
            int exitCode = super.executeCommand( args );
            if ( exitCode != CLI.SUCCESS ) {
                status.setRollbackOnly();
            }
            return exitCode;
        } );
    }
}
