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

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * Replace data in an existing data set. This is only to be used in special cases where AffyDataFromCelCli or
 * RNASeqDataAddCli are not appropriate (that is, for any other types of platforms) and the data imported from GEO (from
 * the SOFT) is not usable. For example, sometimes the submitted provides data that are filtered and normalized in some
 * weird way, and we can get better data from either the supplementary files in GEO or from an external source (e.g. the
 * authors). It is assumed that the quantitation type is the same (you can edit the QT details through the web
 * interface).
 * Again, don't use this this except as a last resort.
 *
 * @author Paul
 */
public class ReplaceDataCli extends ExpressionExperimentManipulatingCLI {

    private String file = null;

    public static void main( String[] args ) {
        ReplaceDataCli c = new ReplaceDataCli();
        executeCommand( c, args );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions() {
        super.buildOptions();
        super.addOption( "file", null,
                "Path to file with tab-delimited data, first column = probe ids, first row = sample IDs (e.g. GEO GSM#)", "file path" );
        super.addForceOption();
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        this.file = this.getOptionValue( "file" );
        if ( file == null )
            throw new IllegalArgumentException( "File is required" );
    }

    @Override
    public String getCommandName() {
        return "replaceData";
    }

    @Override
    protected void doWork() throws Exception {
        DataUpdater dataUpdater = this.getBean( DataUpdater.class );

        if ( this.expressionExperiments.size() > 1 ) {
            throw new IllegalArgumentException( "Sorry, This CLI can only deal with one experiment at a time." );
        }

        ExpressionExperiment ee = ( ExpressionExperiment ) this.expressionExperiments.iterator().next();

        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

        if ( arrayDesignsUsed.size() > 1 ) {
            throw new IllegalArgumentException( "Sorry, can only process single-platform data sets with this tool." );
        }

        ArrayDesign targetArrayDesign = arrayDesignsUsed.iterator().next();

        Collection<QuantitationType> qts = eeService.getPreferredQuantitationType( ee );

        if ( qts == null || qts.size() < 1 || qts.iterator().next() == null ) {
            throw new IllegalArgumentException(
                    "Experiment must have a preferred quantitation type to replace data for" );
        }

        if ( qts.size() > 1 ) {
            throw new IllegalArgumentException(
                    "Experiment must have just one preferred quantitation type to replace data for" );
        }

        QuantitationType qt = qts.iterator().next();

        DoubleMatrixReader reader = new DoubleMatrixReader();

        DoubleMatrix<String, String> data = reader.read( file );

        dataUpdater.replaceData( ee, targetArrayDesign, qt, data );
    }

    @Override
    public String getShortDesc() {
        return "Replace expression data for non-Affymetrix and non-RNA-seq data sets";
    }

}
