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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.compass.gps.spi.CompassGpsInterfaceDevice;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.persistence.util.CompassUtils;

/**
 * Simple command line to index the gemma db. Can index gene's, Expression experiments or array Designs
 *
 * @author klc
 */
@Component
public class IndexGemmaCLI extends AbstractCLI {

    @Autowired
    private BeanFactory beanFactory;

    private boolean indexAD = false;
    private boolean indexB = false;
    private boolean indexEE = false;
    private boolean indexG = false;
    private boolean indexP = false;
    private boolean indexQ = false;
    private boolean indexX = false;
    private boolean indexY = false;

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
    protected void buildOptions( Options options ) {
        Option geneOption = Option.builder( "g" ).desc( "Index genes" ).longOpt( "genes" ).build();
        options.addOption( geneOption );

        Option eeOption = Option.builder( "g" ).desc( "Index Expression Experiments" )
                .longOpt( "ExpressionExperiments" ).build();
        options.addOption( eeOption );

        Option adOption = Option.builder( "a" ).desc( "Index Array Designs" ).longOpt( "ArrayDesigns" )
                .build();
        options.addOption( adOption );

        Option bibliographicOption = Option.builder( "b" ).desc( "Index Bibliographic References" )
                .longOpt( "Bibliographic" ).build();
        options.addOption( bibliographicOption );

        Option probeOption = Option.builder( "s" ).desc( "Index probes" ).longOpt( "probes" ).build();
        options.addOption( probeOption );

        Option sequenceOption = Option.builder( "q" ).desc( "Index sequences" ).longOpt( "sequences" )
                .build();
        options.addOption( sequenceOption );

        options.addOption( Option.builder( "x" ).desc( "Index EE sets" ).longOpt( "eesets" ).build() );

        options.addOption( Option.builder( "y" ).desc( "Index gene sets" ).longOpt( "genesets" ).build() );
    }

    @Override
    protected void doWork() throws Exception {
        /*
         * These beans are defined in Spring XML.
         */
        if ( this.indexG ) {
            this.rebuildIndex( beanFactory.getBean( "geneGps", CompassGpsInterfaceDevice.class ), "Gene index" );
        }
        if ( this.indexEE ) {
            this.rebuildIndex( beanFactory.getBean( "expressionGps", CompassGpsInterfaceDevice.class ),
                    "Expression Experiment index" );
        }
        if ( this.indexAD ) {
            this.rebuildIndex( beanFactory.getBean( "arrayGps", CompassGpsInterfaceDevice.class ), "Array Design index" );
        }
        if ( this.indexB ) {
            this.rebuildIndex( beanFactory.getBean( "bibliographicGps", CompassGpsInterfaceDevice.class ),
                    "Bibliographic Reference Index" );
        }
        if ( this.indexP ) {
            this.rebuildIndex( beanFactory.getBean( "probeGps", CompassGpsInterfaceDevice.class ),
                    "Probe Reference Index" );
        }
        if ( this.indexQ ) {
            this.rebuildIndex( beanFactory.getBean( "biosequenceGps", CompassGpsInterfaceDevice.class ),
                    "BioSequence Index" );
        }

        if ( this.indexY ) {
            this.rebuildIndex( beanFactory.getBean( "experimentSetGps", CompassGpsInterfaceDevice.class ),
                    "Experiment set Index" );
        }

        if ( this.indexX ) {
            this.rebuildIndex( beanFactory.getBean( "geneSetGps", CompassGpsInterfaceDevice.class ), "Gene set Index" );
        }
    }

    @Override
    public String getShortDesc() {
        return "Create or update the searchable indexes for a Gemma production system";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'e' ) )
            indexEE = true;

        if ( commandLine.hasOption( 'a' ) )
            indexAD = true;

        if ( commandLine.hasOption( 'g' ) )
            indexG = true;

        if ( commandLine.hasOption( 'b' ) )
            indexB = true;

        if ( commandLine.hasOption( 's' ) )
            indexP = true;

        if ( commandLine.hasOption( 'q' ) )
            indexQ = true;
        if ( commandLine.hasOption( 'x' ) )
            indexX = true;
        if ( commandLine.hasOption( 'y' ) )
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
