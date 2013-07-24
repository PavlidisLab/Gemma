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

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import ubic.gemma.loader.expression.geo.DataUpdater;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Add (or possibly replace) the data associated with an experiment. Cases include Affymetrix exon arrays, RNA-seq
 * data.+
 * 
 * @author paul
 * @version $Id$
 */
public class AffyExonArrayDataAddCli extends ExpressionExperimentManipulatingCLI {

    private static final String APT_FILE_OPT = "aptFile";
    private String aptFile = null;

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addOption( APT_FILE_OPT, true,
                "File output from apt-probeset-summarize; use if you want to override usual GEO download behaviour" );

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( APT_FILE_OPT ) ) {
            this.aptFile = getOptionValue( APT_FILE_OPT );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "AffyExonArrayDataAdd", args );

        DataUpdater serv = getBean( DataUpdater.class );

        if ( StringUtils.isNotBlank( aptFile ) ) {
            if ( this.expressionExperiments.size() > 1 ) {
                throw new IllegalArgumentException( "Can't use -aptfile unless you are doing just one experiment" );
            }
            BioAssaySet ee = this.expressionExperiments.iterator().next();
            ExpressionExperiment thawedEe = this.eeService.thawLite( ( ExpressionExperiment ) ee );

            Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

            if ( arrayDesignsUsed.size() > 1 ) {
                throw new IllegalArgumentException( "Cannot update data for experiment that uses multiple platforms" );
            }

            ArrayDesign ad = arrayDesignsUsed.iterator().next();
            try {
                if ( ad.getName().toLowerCase().contains( "exon" ) ) {
                    log.info( "Loading data from " + aptFile );
                    serv.addAffyExonArrayData( thawedEe, aptFile );
                    this.successObjects.add( thawedEe.toString() );
                } else {
                    throw new IllegalArgumentException( "Option -aptfile only valid if you are using an exon array." );
                }
            } catch ( Exception e ) {
                log.error( e, e );
                return e;
            }
            return null;
        }

        for ( BioAssaySet ee : this.expressionExperiments ) {
            try {
                ExpressionExperiment thawedEe = this.eeService.thawLite( ( ExpressionExperiment ) ee );

                Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

                if ( arrayDesignsUsed.size() > 1 ) {
                    log.warn( "Cannot update data for experiment that uses multiple platforms" );
                    continue;
                }

                ArrayDesign ad = arrayDesignsUsed.iterator().next();

                if ( ad.getName().toLowerCase().contains( "exon" )
                        && ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) ) {
                    log.info( thawedEe + " looks like affy exon array" );
                    serv.addAffyExonArrayData( thawedEe );
                    this.successObjects.add( thawedEe.toString() );
                } else {
                    throw new IllegalStateException( "This CLI can only deal with exon arrays" );

                }
            } catch ( Exception e ) {
                this.errorObjects.add( ee + " " + e.getLocalizedMessage() );
            }
        }

        super.summarizeProcessing();

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        AffyExonArrayDataAddCli c = new AffyExonArrayDataAddCli();
        c.doWork( args );
    }

}
