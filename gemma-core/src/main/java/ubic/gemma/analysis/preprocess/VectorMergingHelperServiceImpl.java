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
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Transactional methods for updating merged vectors.
 * 
 * @author Paul
 * @version $Id$
 */
@Service
public class VectorMergingHelperServiceImpl implements VectorMergingHelperService {

    private static Log log = LogFactory.getLog( VectorMergingHelperServiceImpl.class );

    @Autowired
    protected DesignElementDataVectorService designElementDataVectorService;

    @Autowired
    protected ProcessedExpressionDataVectorService processedExpressionDataVectorService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    @Autowired
    private TwoChannelMissingValues twoChannelMissingValueService;

    /**
     * @param expExp
     * @param type
     * @param newVectors
     */
    @Override
    public void persist( ExpressionExperiment expExp, QuantitationType type,
            Collection<DesignElementDataVector> newVectors ) {
        if ( newVectors.size() > 0 ) {

            this.processedExpressionDataVectorService.removeProcessedDataVectors( expExp );

            log.info( "Creating " + newVectors.size() + " new vectors for " + type );
            designElementDataVectorService.create( newVectors );
        } else {
            throw new IllegalStateException( "Unexpectedly, no new vectors for " + type );
        }
    }

    /**
     * Do missing value and processed vector creation steps.
     * 
     * @param ees
     */
    @Override
    public ExpressionExperiment postProcess( ExpressionExperiment ee ) {

        log.info( "Postprocessing ..." );

        Collection<ArrayDesign> arrayDesignsUsed = expressionExperimentService.getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            log.warn( "Skipping postprocessing because experiment uses "
                    + "multiple platform types. Please check valid entry and run postprocessing separately." );
        }

        ArrayDesign arrayDesignUsed = arrayDesignsUsed.iterator().next();
        processForMissingValues( ee, arrayDesignUsed );
        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
        return ee;

    }

    /**
     * @param ee
     * @return
     */
    private boolean processForMissingValues( ExpressionExperiment ee, ArrayDesign design ) {

        boolean wasProcessed = false;

        TechnologyType tt = design.getTechnologyType();
        if ( tt == TechnologyType.TWOCOLOR || tt == TechnologyType.DUALMODE ) {
            log.info( ee + " uses a two-color array design, processing for missing values ..." );
            // ee = expressionExperimentService.thawLite( ee );
            twoChannelMissingValueService.computeMissingValues( ee );
            wasProcessed = true;
        }

        return wasProcessed;
    }

}
