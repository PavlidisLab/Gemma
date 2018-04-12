/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.core.loader.expression.geo.DataUpdater;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Add (or possibly replace) the data associated with an affymetrix data set, going back to the CEL files. Can handle
 * exon or 3' arrays.
 *
 * @author paul
 */
public class AffyDataFromCelCli extends ExpressionExperimentManipulatingCLI {

    private static final String APT_FILE_OPT = "aptFile";

    public static void main( String[] args ) {
        AffyDataFromCelCli c = new AffyDataFromCelCli();
        Exception e = c.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    private String aptFile = null;

    private boolean checkForAlreadyDone( BioAssaySet ee ) {
        for ( QuantitationType qt : eeService.getQuantitationTypes( ( ExpressionExperiment ) ee ) ) {
            if ( qt.getIsMaskedPreferred() && qt.getIsRecomputedFromRawData() ) {
                return true;
            }
        }
        return super.auditEventService.hasEvent( ee, DataReplacedEvent.class );
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @Override
    public String getCommandName() {
        return "affyFromCel";
    }

    @Override
    public String getShortDesc() {
        return "Reanalyze Affymetrix data from CEL files, if available; affy-power-tools must be configured.";
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addOption( AffyDataFromCelCli.APT_FILE_OPT, true,
                "File output from apt-probeset-summarize; use if you want to override usual GEO download behaviour" );

        super.addForceOption();

    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception e = super.processCommandLine( args );
        if ( e != null )
            return e;

        DataUpdater serv = this.getBean( DataUpdater.class );

        // This can be done for multiple experiments under some conditions; we get this one just  to test for some multi-platform situations
        Collection<ArrayDesign> arrayDesignsUsed = this.eeService
                .getArrayDesignsUsed( this.expressionExperiments.iterator().next() );

        if ( StringUtils.isNotBlank( aptFile ) ) {
            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException(
                        "Can't use " + AffyDataFromCelCli.APT_FILE_OPT + " unless you are doing just one experiment" );
            }
            ExpressionExperiment thawedEe = ( ExpressionExperiment ) this.expressionExperiments.iterator().next();
            thawedEe = this.eeService.thawLite( thawedEe );

            if ( arrayDesignsUsed.size() > 1 ) {
                throw new IllegalArgumentException( "Cannot use " + AffyDataFromCelCli.APT_FILE_OPT
                        + " for experiment that uses multiple platforms" );
            }

            ArrayDesign ad = arrayDesignsUsed.iterator().next();

            if ( !GeoPlatform.isAffyPlatform( ad.getShortName() ) ) {
                throw new IllegalArgumentException( "Not an Affymetrix array so far as we can tell: " + ad );
            }

            try {
                AbstractCLI.log.info( "Loading data from " + aptFile );
                serv.addAffyData( thawedEe, aptFile );

            } catch ( Exception exception ) {
                return exception;
            }
            return null;
        }

        for ( BioAssaySet ee : this.expressionExperiments ) {
            try {

                Collection<ArrayDesign> adsUsed = this.eeService.getArrayDesignsUsed( ee );

                /*
                 * if the audit trail already has a DataReplacedEvent, skip it, unless --force. Ignore this for
                 * multiplatform studies (at our peril)
                 */
                if ( adsUsed.size() == 1 && !force && this.checkForAlreadyDone( ee ) ) {
                    AbstractCLI.log.warn( ee
                            + ": Already has been recomputed from raw data, skipping (use 'force' to override')" );
                    this.errorObjects.add( ee + ": Already has been computed from raw data" );
                    continue;
                }

                ExpressionExperiment thawedEe = ( ExpressionExperiment ) ee;
                thawedEe = this.eeService.thawLite( thawedEe );

                ArrayDesign ad = adsUsed.iterator().next();
                /*
                 * Even if there are multiple platforms, we assume they are all the same type. If not, that's your
                 * problem :) (seriously, we could check...)
                 */
                if ( ( GeoPlatform.isAffyPlatform( ad.getShortName() ) ) ) {
                    AbstractCLI.log.info( thawedEe + " looks like affy array" );

                    serv.reprocessAffyDataFromCel( thawedEe );
                    this.successObjects.add( thawedEe );
                    AbstractCLI.log.info( "Successfully processed: " + thawedEe );
                } else {
                    AbstractCLI.log.warn( ee
                            + ": This CLI can only deal with Affymetrix platforms" );
                    this.errorObjects.add( ee
                            + ": This CLI can only deal with Affymetrix platforms" );
                }

            } catch ( Exception exception ) {
                AbstractCLI.log.error( exception, exception );
                this.errorObjects.add( ee + " " + exception.getLocalizedMessage() );
            }

        }

        super.summarizeProcessing();

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( AffyDataFromCelCli.APT_FILE_OPT ) ) {
            this.aptFile = this.getOptionValue( AffyDataFromCelCli.APT_FILE_OPT );
        }

    }

}
