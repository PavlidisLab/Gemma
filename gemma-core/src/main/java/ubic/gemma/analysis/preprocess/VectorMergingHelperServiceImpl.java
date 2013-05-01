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

import ubic.gemma.model.common.quantitationtype.QuantitationType;
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

}
