package ubic.gemma.grid.javaspaces;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.springframework.security.context.SecurityContextHolder;

import ubic.gemma.util.SecurityUtil;

public class GenericSpacesWorkerCLI extends AbstractSpacesWorkerCLI {

    private Object delegate = null;

    private Class businessInterface = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI#setRegistrationEntryTask()
     */
    @Override
    protected void setRegistrationEntryTask() throws Exception {
        registrationEntry.message = this.businessInterface.newInstance().getClass().getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI#setWorker()
     */
    @Override
    protected void setWorker() {
        worker = ( CustomDelegatingWorker ) updatedContext.getBean( "genericWorker" );
        worker.setDelegate( delegate );
        worker.setBusinessInterface( businessInterface );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesWorkerCLI#start()
     */
    @Override
    protected void start() {
        log.debug( "Authentication: " + SecurityContextHolder.getContext().getAuthentication() );

        itbThread = new Thread( worker );
        itbThread.start();

        log.info( this.getClass().getSimpleName() + " started successfully." );

    }

    /**
     * Starts the command line interface.
     * 
     * @param args
     */
    public static void main( String[] args ) {
        log.info( "Starting spaces worker ... \n" );

        SecurityUtil.passAuthenticationToChildThreads();

        GenericSpacesWorkerCLI p = new GenericSpacesWorkerCLI();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#processOptions()
     */
    protected void processOptions() {
        super.processOptions();

        String d = super.getOptionValue( "delegate" );
        this.delegate = this.getBean( d );

        String i = super.getOptionValue( "interface" );
        Class intf;
        try {
            intf = Class.forName( i );
        } catch ( ClassNotFoundException e ) {
            throw new RuntimeException( e );
        }
        this.businessInterface = intf;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @Override
    protected void buildOptions() {

        Option delegateOpt = OptionBuilder.isRequired().hasArg( true ).withDescription(
                "The class to delegate the task to." ).create( "delegate" );
        super.addOption( delegateOpt );

        Option businessInterfaceOpt = OptionBuilder.isRequired().hasArg( true ).withDescription(
                "The name of the business/task interface." ).create( "interface" );
        super.addOption( businessInterfaceOpt );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractSpringAwareCLI#getShortDesc()
     */
    @Override
    public String getShortDesc() {
        return "A worker that can be used to process space-based tasks.";
    }

}
