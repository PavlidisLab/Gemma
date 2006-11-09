/*
 * The Gemma project
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
package ubic.gemma.model.expression.bioAssayData;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author joseph
 * @version $Id$
 */
public class DesignElementDataVectorDaoImplTest extends BaseSpringContextTest {
    DesignElementDataVectorDao designElementDataVectorDao;
    
    DesignElementDataVector dedv;
    /**
     * @param designElementDataVectorDao the designElementDataVectorDao to set
     */
    public void setDesignElementDataVectorDao( DesignElementDataVectorDao designElementDataVectorDao ) {
        this.designElementDataVectorDao = designElementDataVectorDao;
    }
    

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        dedv = DesignElementDataVector.Factory.newInstance();
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
    }

   public void testGetGenes() {
       Collection<DesignElementDataVector> dataVectors = new ArrayList<DesignElementDataVector>();
       dedv = DesignElementDataVector.Factory.newInstance();
       dedv.setId( (long) 1 );
       dataVectors.add( dedv );
       
       dedv = DesignElementDataVector.Factory.newInstance();
       dedv.setId( (long) 2 );
       dataVectors.add( dedv );
       
       dedv = DesignElementDataVector.Factory.newInstance();
       dedv.setId( (long) 3 );
       dataVectors.add( dedv );
       
       Map m = designElementDataVectorDao.getGenes( dataVectors );
       assertNotNull(m);
   }
}