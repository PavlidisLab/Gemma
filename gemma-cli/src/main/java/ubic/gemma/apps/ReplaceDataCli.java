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
package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.loader.expression.DataUpdater;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
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

    @Autowired
    private DataUpdater dataUpdater;

    private String file = null;

    public ReplaceDataCli() {
        super();
        setSingleExperimentMode();
    }

    @Override
    public String getCommandName() {
        return "replaceData";
    }

    @Override
    public String getShortDesc() {
        return "Replace expression data for non-Affymetrix and non-RNA-seq data sets";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( Option.builder( "file" ).longOpt( null ).desc( "Path to file with tab-delimited data, first column = probe ids, first row = sample IDs (e.g. GEO GSM#)" ).argName( "file path" ).hasArg().required().build() );
        super.addForceOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        this.file = commandLine.getOptionValue( "file" );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

        if ( arrayDesignsUsed.size() > 1 ) {
            throw new IllegalArgumentException( "Sorry, can only process single-platform data sets with this tool." );
        }

        ArrayDesign targetArrayDesign = arrayDesignsUsed.iterator().next();

        QuantitationType qt = eeService.getPreferredQuantitationType( ee )
                .orElseThrow( () -> new IllegalArgumentException(
                        "Experiment must have a preferred quantitation type to replace data for" ) );

        DoubleMatrixReader reader = new DoubleMatrixReader();

        DoubleMatrix<String, String> data;
        try {
            data = reader.read( file );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        dataUpdater.replaceData( ee, targetArrayDesign, qt, data );
    }
}
