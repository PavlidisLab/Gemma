/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.apps;

import java.io.IOException;
import java.util.Collection;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.geo.DataUpdater;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Replace data in an existing data set. This is only to be used in special cases where AffyDataFromCelCli or
 * RNASeqDataAddCli are not appropriate (that is, for any other types of platforms) and the data imported from GEO (from
 * the SOFT) is not usable. For example, sometimes the submitted provides data that are filtered and normalized in some
 * weird way, and we can get better data from either the supplementary files in GEO or from an external source (e.g. the
 * authors). It is assumed that the quantitation type is the same (you can edit the QT details through the web
 * interface).
 * <p>
 * Again, don't use this this except as a last resort.
 * 
 * @author Paul
 */
public class ReplaceDataCli extends ExpressionExperimentManipulatingCLI {

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ReplaceDataCli c = new ReplaceDataCli();
        c.doWork( args );
    }

    private String file = null;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.core.util.AbstractCLI#getCommandName()
     */
    @Override
    public String getCommandName() {
        return "replaceData";
    }

    @Override
    public String getShortDesc() {
        return "Replace expression data for non-Affymetrix and non-RNA-seq data sets";
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addOption( "file", true,
                "Path to file with tab-delimited data, first column = probe ids, first row = sample IDs (e.g. GEO GSM#)" );
        super.addForceOption();
    }

    @Override
    protected Exception doWork( String[] args ) {
        super.processCommandLine( args );

        DataUpdater serv = getBean( DataUpdater.class );

        if ( this.expressionExperiments.size() > 1 ) {
            throw new IllegalArgumentException( "Sorry, can only process one experiment with this tool." );
        }

        if ( this.expressionExperiments.size() > 1 ) {
            log.warn( "This CLI can only deal with one experiment at a time; only the first one will be processed" );
        }

        ExpressionExperiment ee = ( ExpressionExperiment ) this.expressionExperiments.iterator().next();

        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

        if ( arrayDesignsUsed.size() > 1 ) {
            throw new IllegalArgumentException( "Sorry, can only process single-platform data sets with this tool." );
        }

        ArrayDesign targetArrayDesign = arrayDesignsUsed.iterator().next();

        Collection<QuantitationType> qts = eeService.getPreferredQuantitationType( ee );

        if ( qts.size() > 1 ) {
            throw new IllegalArgumentException( "Experiment must have just one preferred quantitation type to replace data for" );
        }

        QuantitationType qt = qts.iterator().next();
        if ( qt == null ) {
            throw new IllegalArgumentException( "Experiment must have a preferred quantitation type to replace data for" );
        }

        try {
            DoubleMatrixReader reader = new DoubleMatrixReader();

            DoubleMatrix<String, String> data = reader.read( file );

            serv.replaceData( ee, targetArrayDesign, qt, data );

        } catch ( IOException e ) {
            log.error( "Failed while processing " + ee, e );
            return e;
        }
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        this.file = getOptionValue( "file" );
        if ( file == null ) throw new IllegalArgumentException( "File is required" );
    }

}
