/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.association;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.association.Gene2GeneProteinAssocation</code>.
 * 
 * @see ubic.gemma.model.association.Gene2GeneProteinAssocation
 */
public abstract class Gene2GeneProteinAssociationDaoBase extends HibernateDaoSupport implements
        Gene2GeneProteinAssociationDao {

}