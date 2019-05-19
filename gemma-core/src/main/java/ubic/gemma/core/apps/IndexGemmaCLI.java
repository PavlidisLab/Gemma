/*
 /*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.lang3.time.StopWatch;
import org.compass.gps.spi.CompassGpsInterfaceDevice;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.persistence.util.CompassUtils;

/**
 * Simple command line to index the gemma db. Can index gene's, Expression experiments or array Designs
 *
 * @author klc
 */
public class IndexGemmaCLI extends AbstractCLIContextCLI {

    private boolean indexAD = false;
    private boolean indexB = false;
    private boolean indexEE = false;
    private boolean indexG = false;
    private boolean indexP = false;
    private boolean indexQ = false;
    private boolean indexX = false;
    private boolean indexY = false;

    public static void main( String[] args ) {
        IndexGemmaCLI p = new IndexGemmaCLI();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            AbstractCLI.log.info( "Total indexing time: " + watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.SYSTEM;
    }

    @Override
    public String getCommandName() {
        return "searchIndex";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option geneOption = Option.builder( "g" ).desc( "Index genes" ).longOpt( "genes" ).build();
        this.addOption( geneOption );

        Option eeOption = Option.builder( "g" ).desc( "Index Expression Experiments" )
                .longOpt( "ExpressionExperiments" ).build();
        this.addOption( eeOption );

        Option adOption = Option.builder( "a" ).desc( "Index Array Designs" ).longOpt( "ArrayDesigns" )
                .build();
        this.addOption( adOption );

        Option bibliographicOption = Option.builder( "b" ).desc( "Index Bibliographic References" )
                .longOpt( "Bibliographic" ).build();
        this.addOption( bibliographicOption );

        Option probeOption = Option.builder( "s" ).desc( "Index probes" ).longOpt( "probes" ).build();
        this.addOption( probeOption );

        Option sequenceOption = Option.builder( "q" ).desc( "Index sequences" ).longOpt( "sequences" )
                .build();
        this.addOption( sequenceOption );

        this.addOption( Option.builder( "x" ).desc( "Index EE sets" ).longOpt( "eesets" ).build() );

        this.addOption( Option.builder( "y" ).desc( "Index gene sets" ).longOpt( "genesets" ).build() );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = this.processCommandLine( args );
        if ( err != null ) {
            return err;
        }
        try {
            /*
             * These beans are defined in Spring XML.
             */
            if ( this.indexG ) {
                this.rebuildIndex( this.getBean( "geneGps", CompassGpsInterfaceDevice.class ), "Gene index" );
            }
            if ( this.indexEE ) {
                this.rebuildIndex( this.getBean( "expressionGps", CompassGpsInterfaceDevice.class ),
                        "Expression Experiment index" );
            }
            if ( this.indexAD ) {
                this.rebuildIndex( this.getBean( "arrayGps", CompassGpsInterfaceDevice.class ), "Array Design index" );
            }
            if ( this.indexB ) {
                this.rebuildIndex( this.getBean( "bibliographicGps", CompassGpsInterfaceDevice.class ),
                        "Bibliographic Reference Index" );
            }
            if ( this.indexP ) {
                this.rebuildIndex( this.getBean( "probeGps", CompassGpsInterfaceDevice.class ),
                        "Probe Reference Index" );
            }
            if ( this.indexQ ) {
                this.rebuildIndex( this.getBean( "biosequenceGps", CompassGpsInterfaceDevice.class ),
                        "BioSequence Index" );
            }

            if ( this.indexY ) {
                this.rebuildIndex( this.getBean( "experimentSetGps", CompassGpsInterfaceDevice.class ),
                        "Experiment set Index" );
            }

            if ( this.indexX ) {
                this.rebuildIndex( this.getBean( "geneSetGps", CompassGpsInterfaceDevice.class ), "Gene set Index" );
            }
        } catch ( Exception e ) {
            AbstractCLI.log.error( e );
            return e;
        }
        return null;
    }

    @Override
    public String getShortDesc() {
        return "Create or update the searchable indexes for a Gemma production system";
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( 'e' ) )
            indexEE = true;

        if ( this.hasOption( 'a' ) )
            indexAD = true;

        if ( this.hasOption( 'g' ) )
            indexG = true;

        if ( this.hasOption( 'b' ) )
            indexB = true;

        if ( this.hasOption( 's' ) )
            indexP = true;

        if ( this.hasOption( 'q' ) )
            indexQ = true;
        if ( this.hasOption( 'x' ) )
            indexX = true;
        if ( this.hasOption( 'y' ) )
            indexY = true;

    }

    private void rebuildIndex( CompassGpsInterfaceDevice device, String whatIndexingMsg ) {

        long time = System.currentTimeMillis();

        AbstractCLI.log.info( "Rebuilding " + whatIndexingMsg );
        CompassUtils.rebuildCompassIndex( device );
        time = System.currentTimeMillis() - time;

        AbstractCLI.log.info( "Finished rebuilding " + whatIndexingMsg + ".  Took (ms): " + time );
        AbstractCLI.log.info( " \n " );

    }
}
