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
package ubic.gemma.core.analysis.preprocess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;

import java.util.Collection;

/**
 * Transactional methods for updating merged vectors.
 *
 * @author Paul
 */
@Service
public class VectorMergingHelperServiceImpl implements VectorMergingHelperService {

    private static final Log log = LogFactory.getLog( VectorMergingHelperServiceImpl.class );

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Override
    @Transactional
    public ExpressionExperiment persist( ExpressionExperiment ee,
            Collection<RawExpressionDataVector> newVectors ) {
        VectorMergingHelperServiceImpl.log
                .info( "Creating " + newVectors.size() + " merged raw data vectors; removing " + ee.getRawExpressionDataVectors().size()
                        + " old ones" );

        ee.getProcessedExpressionDataVectors().clear();
        ee.getRawExpressionDataVectors().clear();
        ee.getRawExpressionDataVectors().addAll( newVectors );
        return expressionExperimentDao.save( ee );

    }

}
