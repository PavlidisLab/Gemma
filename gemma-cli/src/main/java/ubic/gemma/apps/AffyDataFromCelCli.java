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

package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.core.loader.expression.geo.model.GeoPlatform;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedDataReplacedEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.io.IOException;
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

    @Autowired
    private DataUpdater serv;
    @Autowired
    private ArrayDesignService asd;

    private String aptFile = null;
    private String celchip = null;

    @Override
    public String getCommandName() {
        return "affyFromCel";
    }

    @Override
    public String getShortDesc() {
        return "Reanalyze Affymetrix data from CEL files, if available; affy-power-tools must be configured.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        addSingleExperimentOption( options, Option.builder( AffyDataFromCelCli.APT_FILE_OPT ).longOpt( null )
                .desc( "File output from apt-probeset-summarize; use if you want to override usual GEO download behaviour; "
                + "ensure you used the right official CDF/MPS configuration" ).argName( "path" ).hasArg().build() );
        addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( AffyDataFromCelCli.APT_FILE_OPT ) ) {
            this.aptFile = commandLine.getOptionValue( AffyDataFromCelCli.APT_FILE_OPT );
        }
        if ( commandLine.hasOption( "celchip" ) ) {
            this.celchip = commandLine.getOptionValue( "celchip" );
        }
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        // This can be done for multiple experiments under some conditions; we get this one just  to test for some multi-platform situations
        if ( StringUtils.isNotBlank( aptFile ) ) {
            processFromAptFile( ee );
        } else {
            reprocessFromCel( ee );
        }
    }

    private void processFromAptFile( ExpressionExperiment ee ) {
        if ( this.celchip != null ) {
            throw new UnsupportedOperationException( "celchip not supported with aptFile yet" );
        }

        ee = this.eeService.thawLite( ee );

        Collection<ArrayDesign> arrayDesignsUsed = this.eeService
                .getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            throw new IllegalArgumentException( "Cannot use " + AffyDataFromCelCli.APT_FILE_OPT
                    + " for experiment that uses multiple platforms" );
        }

        ArrayDesign ad = arrayDesignsUsed.iterator().next();

        if ( !GeoPlatform.isAffyPlatform( ad.getShortName() ) ) {
            throw new IllegalArgumentException( "Not an Affymetrix array so far as we can tell: " + ad );
        }

        log.info( "Loading data from " + aptFile );
        try {
            serv.addAffyDataFromAPTOutput( ee, aptFile );
            addSuccessObject( ee, "Loaded Affy data from " + aptFile + "." );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private void reprocessFromCel( ExpressionExperiment ee ) {
        ee = this.eeService.thawLite( ee );

        /*
         * if the audit trail already has a DataReplacedEvent, skip it, unless --force.
         */
        if ( this.checkForAlreadyDone( ee ) && !isForce() ) {
            addWarningObject( ee, "Was already run before, use -force to reload Affy data." );
            return;
        }

        /*
         * Avoid repeated attempts that won't work e.g. no data available.
         */
        if ( super.auditEventService.hasEvent( ee, FailedDataReplacedEvent.class ) && !isForce() ) {
            addWarningObject( ee, "Loading Affy data failed before, use -force to re-attempt." );
            return;
        }

        if ( ee.getAccession() == null || ee.getAccession().getAccession() == null ) {
            addWarningObject( ee, "Can only process from CEL for data sets with an external accession." );
            return;
        }

        Collection<ArrayDesign> adsUsed = this.eeService.getArrayDesignsUsed( ee );
        ArrayDesign ad = adsUsed.iterator().next();

        /*
         * Even if there are multiple platforms, we assume they are all Affy, or all not. If not, that's your
         * problem :) (seriously, we could check...)
         */
        if ( GeoPlatform.isAffyPlatform( ad.getShortName() ) ) {
            log.info( ad + " looks like Affy array" );
            serv.reprocessAffyDataFromCel( ee );
            addSuccessObject( ee, "Reprocessed Affy data from CEL." );
        } else if ( asd.isMerged( Collections.singleton( ad.getId() ) ).get( ad.getId() ) ) {
            ad = asd.thawLite( ad );
            ArrayDesign mergee = ad.getMergees().iterator().next();

            // handle one level of sub-mergees....
            if ( asd.isMerged( Collections.singleton( mergee.getId() ) ).get( mergee.getId() ) ) {
                mergee = asd.thawLite( mergee );
                mergee = asd.thawLite( mergee.getMergees().iterator().next() );
            }

            if ( GeoPlatform.isAffyPlatform( mergee.getShortName() ) ) {
                log.info( ad + " looks like Affy array made from merger of other platforms" );
                serv.reprocessAffyDataFromCel( ee );
                addSuccessObject( ee, "Reprocessed Affy data from CEL from a merged platform." );
            } else {
                throw new RuntimeException( ad + " is not recognized as an Affymetrix platform. If this is a mistake, the Gemma configuration needs to be updated." );
            }
        } else {
            throw new RuntimeException( ad + " is not recognized as an Affymetrix platform. If this is a mistake, the Gemma configuration needs to be updated." );
        }
    }

    private boolean checkForAlreadyDone( ExpressionExperiment ee ) {
        for ( QuantitationType qt : eeService.getQuantitationTypes( ee ) ) {
            if ( qt.getIsRecomputedFromRawData() ) {
                return true;
            }
        }
        return super.auditEventService.hasEvent( ee, DataReplacedEvent.class );
    }
}
