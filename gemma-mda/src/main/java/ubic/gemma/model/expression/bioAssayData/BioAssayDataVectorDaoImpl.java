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
package ubic.gemma.model.expression.bioAssayData;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.bioAssayData.BioAssayDataVector
 */
@Repository
public class BioAssayDataVectorDaoImpl extends ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDaoBase {

    @Autowired
    public BioAssayDataVectorDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDaoBase#find(ubic.gemma.model.expression.bioAssayData
     * .BioAssayDataVector)
     */
    @Override
    public BioAssayDataVector find( BioAssayDataVector bioAssayDataVector ) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * @seeubic.gemma.model.expression.bioAssayData.BioAssayDataVectorDaoBase#findOrCreate(ubic.gemma.model.expression.
     * bioAssayData.BioAssayDataVector)
     */
    @Override
    public BioAssayDataVector findOrCreate( BioAssayDataVector bioAssayDataVector ) {
        throw new UnsupportedOperationException();
    }
}