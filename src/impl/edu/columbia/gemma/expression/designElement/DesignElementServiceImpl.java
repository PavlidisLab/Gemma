/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package edu.columbia.gemma.expression.designElement;

/**
 * <hr>
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.expression.designElement.DesignElementService
 */
public class DesignElementServiceImpl extends edu.columbia.gemma.expression.designElement.DesignElementServiceBase {

    /**
     * @see edu.columbia.gemma.expression.designElement.DesignElementService#find(edu.columbia.gemma.expression.designElement.DesignElement)
     */
    protected edu.columbia.gemma.expression.designElement.DesignElement handleFind(
            edu.columbia.gemma.expression.designElement.DesignElement designElement ) throws java.lang.Exception {
        return this.getDesignElementDao().find( designElement );
    }

    /**
     * @see edu.columbia.gemma.expression.designElement.DesignElementService#remove(edu.columbia.gemma.expression.designElement.DesignElement)
     */
    protected void handleRemove( edu.columbia.gemma.expression.designElement.DesignElement designElement )
            throws java.lang.Exception {
        this.getDesignElementDao().remove( designElement );
    }

    /**
     * @see edu.columbia.gemma.expression.designElement.DesignElementService#update(edu.columbia.gemma.expression.designElement.DesignElement)
     */
    protected void handleUpdate( edu.columbia.gemma.expression.designElement.DesignElement designElement )
            throws java.lang.Exception {
        this.getDesignElementDao().update( designElement );
    }

    @Override
    protected DesignElement handleFindOrCreate( DesignElement designElement ) throws Exception {
        return this.getDesignElementDao().findOrCreate( designElement );
    }

}