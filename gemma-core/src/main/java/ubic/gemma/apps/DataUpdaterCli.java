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

import ubic.gemma.loader.expression.geo.DataUpdater;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Add (or possibly replace) the data associated with an experiment. Cases include Affymetrix exon arrays, RNA-seq data.
 * 
 * @author paul
 * @version $Id$
 */
public class DataUpdaterCli extends ExpressionExperimentManipulatingCLI {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( "DataUpdater", args );

        DataUpdater serv = getBean( DataUpdater.class );

        for ( BioAssaySet ee : this.expressionExperiments ) {
            try {
                ExpressionExperiment thawedEe = this.eeService.thawLite( ( ExpressionExperiment ) ee );

                Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

                if ( arrayDesignsUsed.size() > 1 ) {
                    log.warn( "Cannot update data for experiment that uses multiple platforms" );
                    continue;
                }

                ArrayDesign ad = arrayDesignsUsed.iterator().next();

                if ( ad.getDescription().contains( "Exon" ) && ad.getTechnologyType().equals( TechnologyType.ONECOLOR ) ) {
                    log.info( thawedEe + " looks like affy exon array" );
                    serv.addAffyExonArrayData( thawedEe );
                    this.successObjects.add( thawedEe.toString() );
                } else {
                    log.warn( thawedEe + ": Don't know how to add data to this yet" );
                    this.errorObjects.add( thawedEe + ": Don't know how to add data to this yet" );
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
        DataUpdaterCli c = new DataUpdaterCli();
        c.doWork( args );
    }

}
