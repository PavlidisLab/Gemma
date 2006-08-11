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
package ubic.gemma.model;

import ubic.gemma.externalDb.ExternalDatabaseTest;
import ubic.gemma.model.common.description.BibliographicReferenceDaoImplTest;
import ubic.gemma.model.common.description.DatabaseEntryDaoImplTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoImplTest;
import ubic.gemma.model.expression.bioAssay.BioAssayDaoImplTest;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDaoImplTest;
import ubic.gemma.model.expression.biomaterial.BioMaterialDaoImplTest;
import ubic.gemma.model.genome.QtlDaoImplTest;
import ubic.gemma.model.genome.gene.CandidateGeneListDaoImplTest;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDaoImplTest;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoImplTest;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AllModelTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Model-related tests for gemma-core" );

        suite.addTestSuite( ArrayDesignDaoImplTest.class );
        suite.addTestSuite( BioMaterialDaoImplTest.class );
        suite.addTestSuite( ExternalDatabaseTest.class );
        suite.addTestSuite( CandidateGeneListDaoImplTest.class );
        suite.addTestSuite( QtlDaoImplTest.class );

        suite.addTestSuite( BibliographicReferenceDaoImplTest.class );
        suite.addTestSuite( DatabaseEntryDaoImplTest.class );

        suite.addTestSuite( BioAssayDaoImplTest.class );
        suite.addTestSuite( BioAssayDimensionDaoImplTest.class );
        suite.addTestSuite( BlatAssociationDaoImplTest.class );
        suite.addTestSuite( BlatResultDaoImplTest.class );

        return suite;
    }
}
