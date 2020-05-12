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

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedDataReplacedEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.Collection;
import java.util.Collections;

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
        executeCommand( c, args );
    }

    private String aptFile = null;
    private String celchip = null;

    private boolean checkForAlreadyDone( BioAssaySet ee ) {
        for ( QuantitationType qt : eeService.getQuantitationTypes( ( ExpressionExperiment ) ee ) ) {
            if ( qt.getIsRecomputedFromRawData() ) {
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
        super.addOption( AffyDataFromCelCli.APT_FILE_OPT, null,
                "File output from apt-probeset-summarize; use if you want to override usual GEO download behaviour; "
                        + "ensure you used the right official CDF/MPS configuration",
                "path" );
        super.addForceOption();

    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception e = super.processCommandLine( args );
        if ( e != null )
            return e;

        DataUpdater serv = this.getBean( DataUpdater.class );

        if ( this.expressionExperiments.isEmpty() )
            return null;

        // This can be done for multiple experiments under some conditions; we get this one just  to test for some multi-platform situations
        Collection<ArrayDesign> arrayDesignsUsed = this.eeService
                .getArrayDesignsUsed( this.expressionExperiments.iterator().next() );

        if ( StringUtils.isNotBlank( aptFile ) ) {
            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException(
                        "Can't use " + AffyDataFromCelCli.APT_FILE_OPT + " unless you are doing just one experiment" );
            }

            if ( this.celchip != null ) {
                throw new UnsupportedOperationException( "celchip not supported with aptFile yet" );
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
                serv.addAffyDataFromAPTOutput( thawedEe, aptFile );

            } catch ( Exception exception ) {
                return exception;
            }
            return null;
        }

        for ( BioAssaySet ee : this.expressionExperiments ) {
            try {

                Collection<ArrayDesign> adsUsed = this.eeService.getArrayDesignsUsed( ee );
                ExpressionExperiment thawedEe = ( ExpressionExperiment ) ee;
                thawedEe = this.eeService.thawLite( thawedEe );

                /*
                 * if the audit trail already has a DataReplacedEvent, skip it, unless --force.
                 */
                if ( this.checkForAlreadyDone( ee ) && !this.force ) {

                    this.errorObjects.add( ee
                            + ": Was already run before, use -force" );
                    continue;
                }

                /*
                 * Avoid repeated attempts that won't work e.g. no data available.
                 */
                if ( super.auditEventService.hasEvent( ee, FailedDataReplacedEvent.class ) && !this.force ) {
                    this.errorObjects.add( ee
                            + ": Failed before, use -force to re-attempt" );
                    continue;
                }

                if ( thawedEe.getAccession() == null || thawedEe.getAccession().getAccession() == null ) {
                    throw new UnsupportedOperationException( "Can only process from CEL for data sets with an external accession" );
                }

                ArrayDesign ad = adsUsed.iterator().next();
                ArrayDesignService asd = this.getBean( ArrayDesignService.class );

                /*
                 * Even if there are multiple platforms, we assume they are all Affy, or all not. If not, that's your
                 * problem :) (seriously, we could check...)
                 */
                if ( ( GeoPlatform.isAffyPlatform( ad.getShortName() ) ) ) {
                    AbstractCLI.log.info( ad + " looks like Affy array" );
                    serv.reprocessAffyDataFromCel( thawedEe );

                    this.successObjects.add( thawedEe );
                    AbstractCLI.log.info( "Successfully processed: " + thawedEe );
                } else if ( asd.isMerged( Collections.singleton( ad.getId() ) ).get( ad.getId() ) ) {
                    ad = asd.thawLite( ad );
                    if ( GeoPlatform.isAffyPlatform( ad.getMergees().iterator().next().getShortName() ) ) {
                        AbstractCLI.log.info( ad + " looks like Affy array made from merger of other platforms" );
                        serv.reprocessAffyDataFromCel( thawedEe );
                        this.successObjects.add( thawedEe );
                        AbstractCLI.log.info( "Successfully processed: " + thawedEe );
                    }
                } else {

                    this.errorObjects.add( ee + ": " +
                            ad
                            + " is not recognized as an Affymetrix platform. If this is a mistake, the Gemma configuration needs to be updated." );
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
        if ( this.hasOption( "celchip" ) ) {
            this.celchip = this.getOptionValue( "celchip" );
        }
    }

}
