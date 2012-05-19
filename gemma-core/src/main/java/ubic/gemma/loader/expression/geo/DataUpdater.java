/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.loader.expression.geo;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.AffyPowerToolsProbesetSummarize;
import ubic.gemma.loader.expression.geo.fetcher.RawDataFetcher;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Update the data associated with an experiment. Primary designed for filling in data that we can't or don't want to
 * get from GEO. For loading experiments from flat files, see SimpleExpressionDataLoaderService
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class DataUpdater {

    @Autowired
    ArrayDesignService arrayDesignService;

    @Autowired
    ExpressionExperimentService experimentService;

    @Autowired
    ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    public void addAffyExonArrayData( ExpressionExperiment ee ) {
        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException();
        }
        addAffyExonArrayData( ee, ads.iterator().next() );
    }

    /**
     * @param ee
     * @param ad
     */
    public void addAffyExonArrayData( ExpressionExperiment ee, ArrayDesign ad ) {

        RawDataFetcher f = new RawDataFetcher();
        Collection<LocalFile> files = f.fetch( ee.getAccession().getAccession() );

        if ( files.isEmpty() ) {
            throw new RuntimeException( "Data was apparently not available" );
        }

        AffyPowerToolsProbesetSummarize apt = new AffyPowerToolsProbesetSummarize();
        Collection<RawExpressionDataVector> vectors = apt.processExonArrayData( ee, ad, files );

        experimentService.replaceVectors( ee, ad, vectors );

    }

    /**
     * @param ee
     * @param data
     */
    public void replaceData( ExpressionExperiment ee, ExpressionDataDoubleMatrix data ) {
        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException();
        }
        addData( ee, ads.iterator().next(), data );
    }

    /**
     * @param ee
     * @param ad
     * @param data
     */
    public void addData( ExpressionExperiment ee, ArrayDesign ad, ExpressionDataDoubleMatrix data ) {
        throw new UnsupportedOperationException( "not implemented yet" );
    }

}
