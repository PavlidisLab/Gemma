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
package ubic.gemma.model.association.phenotype;

import java.util.Collection;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.BaseDao;

/**
 * @author nicolas
 * @version $Id$
 */
public interface PhenotypeAssociationDao extends BaseDao<PhenotypeAssociation> {

    /** find Genes link to a phenotype */
    public Collection<PhenotypeAssociation> findByPhenotype( String phenotypeValue );

    /** find all phenotypes */
    public Collection<CharacteristicValueObject> loadAllPhenotypes();

    /**
     * count the number of Genes with a phenotype
     */
    public Long countGenesWithPhenotype( String phenotypeValue );
}
