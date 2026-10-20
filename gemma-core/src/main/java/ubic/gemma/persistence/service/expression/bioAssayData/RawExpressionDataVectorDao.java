/*
 * The Gemma project Copyright (c) 2009 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.persistence.service.expression.bioAssayData;

import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;

/**
 * @author paul
 */
public interface RawExpressionDataVectorDao extends DesignElementDataVectorDao<RawExpressionDataVector> {

    Collection<RawExpressionDataVector> find( ArrayDesign arrayDesign, QuantitationType quantitationType );

    Collection<RawExpressionDataVector> find( Collection<CompositeSequence> designElements,
            QuantitationType quantitationType );

    /**
     * Fetch up to {@code limit} raw vectors for the given quantitation type, chosen uniformly at random.
     * <p>
     * Mirrors {@code ProcessedExpressionDataVectorDao#getRandomProcessedVectors}: a cheap id-only scan is
     * shuffled in Java and only the picked vectors are fetched (with the design-element / array-design /
     * bioAssayDimension / quantitationType join fetches), avoiding an {@code ORDER BY RAND()} sort and a
     * whole-matrix load. Unlike the processed variant there is no {@code rankByMean} preference — raw
     * vectors carry no rank, so the sample is uniform.
     */
    Collection<RawExpressionDataVector> getRandomRawVectors( QuantitationType quantitationType, int limit );

}
