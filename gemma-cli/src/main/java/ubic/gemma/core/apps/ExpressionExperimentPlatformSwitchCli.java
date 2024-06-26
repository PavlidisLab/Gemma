/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.ExpressionExperimentPlatformSwitchService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.Collection;

/**
 * Switch the array design used to the merged one.
 *
 * @author pavlidis
 */
public class ExpressionExperimentPlatformSwitchCli extends ExpressionExperimentManipulatingCLI {

    private String arrayDesignName = null;

    @Autowired
    private ExpressionExperimentPlatformSwitchService serv;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private AuditTrailService ats;

    @Override
    public String getCommandName() {
        return "switchExperimentPlatform";
    }

    @Override
    protected void doWork() throws Exception {
        for ( BioAssaySet ee : expressionExperiments ) {
            if ( ee instanceof ExpressionExperiment ) {
                this.processExperiment( ( ExpressionExperiment ) ee );
            } else {
                throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
            }
        }
    }

    @Override
    public String getShortDesc() {
        return "Switch an experiment to a different array design (usually a merged one)";
    }

    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        Option arrayDesignOption = Option.builder( "a" ).hasArg().argName( "Array design" ).desc(
                        "Array design short name to be switched to - no need to specify if the platforms used by the EE are merged" )
                .longOpt( "array" ).build();
        options.addOption( arrayDesignOption );
        this.addForceOption( options );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( 'a' ) ) {
            this.arrayDesignName = commandLine.getOptionValue( 'a' );
        }
    }

    private void processExperiment( ExpressionExperiment ee ) {
        try {
            ee = this.eeService.thawLite( ee );
            ArrayDesign ad;
            if ( this.arrayDesignName != null ) {
                ad = this.locateArrayDesign( this.arrayDesignName );
                if ( ad == null ) {
                    throw new RuntimeException( "Unknown array design" );
                }
                serv.switchExperimentToArrayDesign( ee, ad );
                ats.addUpdateEvent( ee, ExpressionExperimentPlatformSwitchEvent.class, "Switched to use " + ad );
            } else {
                // Identify merged platform automatically; not really recommended as it might not make the optimal choice.
                ad = serv.switchExperimentToMergedPlatform( ee );
                ats.addUpdateEvent( ee, ExpressionExperimentPlatformSwitchEvent.class, "Switched to use merged platform " + ad );
            }
            addSuccessObject( ee );
        } catch ( Exception e ) {
            addErrorObject( ee, e );
        }
    }

    /**
     * @param  name               of the array design to find.
     * @return an array design, if found. Bails otherwise with an error exit code
     */
    private ArrayDesign locateArrayDesign( String name ) {

        ArrayDesign arrayDesign = null;

        Collection<ArrayDesign> byname = arrayDesignService.findByName( name.trim().toUpperCase() );
        if ( byname.size() > 1 ) {
            throw new IllegalArgumentException( "Ambiguous name: " + name );
        } else if ( byname.size() == 1 ) {
            arrayDesign = byname.iterator().next();
        }

        if ( arrayDesign == null ) {
            arrayDesign = arrayDesignService.findByShortName( name );
        }

        if ( arrayDesign == null ) {
            AbstractCLI.log.error( "No arrayDesign " + name + " found" );
        }
        return arrayDesign;
    }
}
