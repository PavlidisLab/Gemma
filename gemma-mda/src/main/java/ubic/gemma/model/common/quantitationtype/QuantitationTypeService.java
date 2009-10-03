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
package ubic.gemma.model.common.quantitationtype;

/**
 * 
 */
public interface QuantitationTypeService {

    /**
     * 
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType create(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * 
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType find(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * 
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType findOrCreate(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * 
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType load( java.lang.Long id );

    /**
     * 
     */
    public java.util.Collection<QuantitationType> loadAll();

    /**
     * 
     */
    public void remove( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * 
     */
    public void update( ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

}
