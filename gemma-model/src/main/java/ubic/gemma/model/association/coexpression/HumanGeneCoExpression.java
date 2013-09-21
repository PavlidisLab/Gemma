/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.association.coexpression;

/**
 * 
 */
public abstract class HumanGeneCoExpression extends ubic.gemma.model.association.coexpression.Gene2GeneCoexpression {

    /**
     * Constructs new instances of {@link ubic.gemma.model.association.coexpression.HumanGeneCoExpression}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.coexpression.HumanGeneCoExpression}.
         */
        public static ubic.gemma.model.association.coexpression.HumanGeneCoExpression newInstance() {
            return new ubic.gemma.model.association.coexpression.HumanGeneCoExpressionImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.association.coexpression.HumanGeneCoExpression}, taking
         * all possible properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.association.coexpression.HumanGeneCoExpression newInstance(
                ubic.gemma.model.analysis.Analysis sourceAnalysis, ubic.gemma.model.genome.Gene secondGene,
                ubic.gemma.model.genome.Gene firstGene, Double pvalue, Double effect, Integer numDataSets,
                byte[] datasetsTestedVector, byte[] datasetsSupportingVector, byte[] specificityVector ) {
            final ubic.gemma.model.association.coexpression.HumanGeneCoExpression entity = new ubic.gemma.model.association.coexpression.HumanGeneCoExpressionImpl();
            entity.setSourceAnalysis( sourceAnalysis );
            entity.setSecondGene( secondGene );
            entity.setFirstGene( firstGene );
            entity.setPvalue( pvalue );
            entity.setEffect( effect );
            entity.setNumDataSets( numDataSets );
            entity.setDatasetsTestedVector( datasetsTestedVector );
            entity.setDatasetsSupportingVector( datasetsSupportingVector );
            entity.setSpecificityVector( specificityVector );
            return entity;
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1143581426457333084L;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public HumanGeneCoExpression() {
    }

}