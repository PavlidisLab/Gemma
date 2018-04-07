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
import ubic.gemma.core.loader.expression.geo.DataUpdater;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * Add (or possibly replace) the data associated with an affymetrix data set, going back to the CEL files. Can handle
 * exon or 3' arrays.
 *
 * @author paul
 */
public class AffyDataFromCelCli extends ExpressionExperimentManipulatingCLI {

    private static final String APT_FILE_OPT = "aptFile";
    private static final String CDF_FILE_OPT = "cdfFile";
    private String aptFile = null;
    // /space/grp/databases/arrays/cdfs...
    private String cdfFile = null;

    public static void main( String[] args ) {
        AffyDataFromCelCli c = new AffyDataFromCelCli();
        Exception e = c.doWork( args );
        if ( e != null ) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public boolean checkForAlreadyDone( BioAssaySet ee ) {
        for ( QuantitationType qt : eeService.getQuantitationTypes( ( ExpressionExperiment ) ee ) ) {
            if ( qt.getIsMaskedPreferred() && qt.getIsRecomputedFromRawData() ) {
                return true;
            }
        }
        return super.auditEventService.hasEvent( ee, DataReplacedEvent.class );
    }

    @Override
    public String getCommandName() {
        return "affyFromCel";
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
            try {
                AbstractCLI.log.info( "Loading data from " + aptFile );
                if ( ( ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) && GeoPlatform
                        .isAffymetrixExonArray( ad.getShortName() ) ) || ad.getName().toLowerCase()
                                .contains( "exon" ) ) {
                    serv.addAffyExonArrayData( thawedEe, aptFile );
                } else if ( ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) && ad.getName().toLowerCase()
                        .contains( "affy" ) ) {
                    serv.addAffyData( thawedEe, aptFile );
                } else {
                    throw new IllegalArgumentException( "Option " + AffyDataFromCelCli.APT_FILE_OPT
                            + " only valid if you are using an exon array." );
                }
            } catch ( Exception exception ) {
                return exception;
            }
            return null;
        }

        if ( StringUtils.isNotBlank( cdfFile ) ) {
            if ( arrayDesignsUsed.size() > 1 ) {
                throw new IllegalArgumentException( "Cannot use " + AffyDataFromCelCli.CDF_FILE_OPT
                        + " for experiment that uses multiple platforms" );
            }
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
                if ( ( ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) && GeoPlatform
                        .isAffymetrixExonArray( ad.getShortName() ) ) || ad.getName().toLowerCase()
                                .contains( "exon" ) ) {
                    AbstractCLI.log.info( thawedEe + " looks like affy exon array" );

                    /*
                     * TODO: make this work with non-exon arrays that don't have CDFs (some platforms have 'unsupported'
                     * CDFs that we use, but these don't even have that)
                     * 
                     * # GPL11533=MoGene-1_1 - no cdf
                     * # GPL17962=HuGene-2_1 - no cdf
                     * # GPL16686=HuGene-2_0 - no cdf
                     * # GPL16570=MoGene-2_0 - no cdf
                     * # GPL11532=HuGene-1_1 - no cdf
                     * # GPL23159=Clariom_s_Human - no cdf
                     * # GPL17117=RaGene-2_0 - no cdf
                     * # GPL17400=MoGene-2_1 - no cdf
                     */

                    serv.addAffyExonArrayData( thawedEe );
                    this.successObjects.add( thawedEe.toString() );
                    AbstractCLI.log.info( "Successfully processed: " + thawedEe );
                } else if ( ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) && ad.getName().toLowerCase()
                        .contains( "affy" ) ) {
                    AbstractCLI.log.info( thawedEe + " looks like a affy 3-prime array" );
                    serv.reprocessAffyThreePrimeArrayData( thawedEe );
                    this.successObjects.add( thawedEe.toString() );
                    AbstractCLI.log.info( "Successfully processed: " + thawedEe );
                } else {
                    AbstractCLI.log.warn( ee
                            + ": This CLI can only deal with Affymetrix platforms (exon or 3' probe designs)" );
                    this.errorObjects.add( ee
                            + ": This CLI can only deal with Affymetrix platforms (exon or 3' probe designs)" );
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
    public String getShortDesc() {
        return "Reanalyze Affymetrix data from CEL files, if available; affy-power-tools must be configured.";
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addOption( AffyDataFromCelCli.APT_FILE_OPT, true,
                "File output from apt-probeset-summarize; use if you want to override usual GEO download behaviour; don't use with "
                        + AffyDataFromCelCli.CDF_FILE_OPT );
        super.addOption( AffyDataFromCelCli.CDF_FILE_OPT, true,
                "CDF file for Affy 3' arrays; otherwise will try to find automatically using the value of affy.power.tools.cdf.path; don't use with "
                        + AffyDataFromCelCli.APT_FILE_OPT );
        super.addForceOption();

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( this.hasOption( AffyDataFromCelCli.APT_FILE_OPT ) ) {
            this.aptFile = this.getOptionValue( AffyDataFromCelCli.APT_FILE_OPT );
        }
        if ( this.hasOption( AffyDataFromCelCli.CDF_FILE_OPT ) ) {
            this.cdfFile = this.getOptionValue( AffyDataFromCelCli.CDF_FILE_OPT );
        }

    }

}
