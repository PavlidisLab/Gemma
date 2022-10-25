/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;

/**
 * Provides methods that can be applied to both RawExpressionDataVector and ProcessedExpressionDataVector
 */
@Service
public class RawExpressionDataVectorServiceImpl extends AbstractDesignElementDataVectorService<RawExpressionDataVector>
        implements RawExpressionDataVectorService {

    @Autowired
    protected RawExpressionDataVectorServiceImpl( RawExpressionDataVectorDao mainDao ) {
        super( mainDao );
    }
}