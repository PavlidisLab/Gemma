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
 * @see HumanGeneCoExpression
 */
public class HumanGeneCoExpression extends Gene2GeneCoexpression {

    public static final class Factory {

        @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
        public static HumanGeneCoExpression newInstance( Double effect, Long firstGene, Long secondGene ) {
            final HumanGeneCoExpression entity = new HumanGeneCoExpression();
            assert effect != null && firstGene != null && secondGene != null;
            Gene2GeneCoexpression.tryWriteFields( effect, firstGene, secondGene, entity );
            return entity;
        }
    }
}