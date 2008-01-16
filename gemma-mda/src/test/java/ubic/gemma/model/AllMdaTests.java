/*
 * The gemma.model project
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

import junit.framework.Test;
import junit.framework.TestSuite;
import ubic.gemma.model.common.auditAndSecurity.UserServiceImplTest;
import ubic.gemma.model.common.description.BibliographicReferenceServiceImplTest;
import ubic.gemma.model.common.description.ExternalDatabaseServiceImplTest;
import ubic.gemma.model.common.description.LocalFileServiceImplTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignServiceImplTest;
import ubic.gemma.model.expression.experiment.ExpressionExperimentServiceImplTest;
import ubic.gemma.model.genome.GeneImplTest;
import ubic.gemma.model.genome.biosequence.BioSequenceImplTest;
import ubic.gemma.model.genome.gene.CandidateGeneImplTest;
import ubic.gemma.model.genome.gene.CandidateGeneListImplTest;
import ubic.gemma.model.genome.gene.CandidateGeneListServiceImplTest;
import ubic.gemma.model.genome.gene.GeneServiceImplTest;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultImplTest;

/**
 * Tests for gemma.model-mda
 * 
 * @author pavlidis
 * @version $Id$
 */
public class AllMdaTests extends TestSuite {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Tests for gemma-mda" );

        suite.addTestSuite( UserServiceImplTest.class );

        suite.addTestSuite( BibliographicReferenceServiceImplTest.class );
        suite.addTestSuite( ExternalDatabaseServiceImplTest.class );
        suite.addTestSuite( LocalFileServiceImplTest.class );

        suite.addTestSuite( ArrayDesignServiceImplTest.class );
        suite.addTestSuite( ExpressionExperimentServiceImplTest.class );

        suite.addTestSuite( BioSequenceImplTest.class );

        suite.addTestSuite( CandidateGeneImplTest.class );
        suite.addTestSuite( CandidateGeneListImplTest.class );
        suite.addTestSuite( CandidateGeneListServiceImplTest.class );
        suite.addTestSuite( GeneServiceImplTest.class );

        suite.addTestSuite( BlatResultImplTest.class );
        suite.addTestSuite( GeneImplTest.class );
        return suite;
    }

}
