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

import junit.framework.Test;
import junit.framework.TestSuite;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionServiceTest;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoImplTest;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailDaoTest;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailServiceImplTest;
import ubic.gemma.model.common.auditAndSecurity.UserDaoImplTest;
import ubic.gemma.model.common.description.BibliographicReferenceDaoImplTest;
import ubic.gemma.model.common.description.DatabaseEntryDaoImplTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoImplTest;
import ubic.gemma.model.expression.bioAssay.BioAssayDaoImplTest;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionDaoImplTest;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorDaoImplTest;
import ubic.gemma.model.expression.biomaterial.BioMaterialDaoImplTest;
import ubic.gemma.model.expression.designElement.CompositeSequenceDaoImplTest;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDaoImplTest;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDeleteTest;
import ubic.gemma.model.genome.BioSequencePersistTest;
import ubic.gemma.model.genome.QtlDaoImplTest;
import ubic.gemma.model.genome.gene.CandidateGeneListDaoImplTest;
import ubic.gemma.model.genome.gene.GeneDaoTest;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDaoImplTest;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoImplTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AllModelTests {

    public static Test suite() {
        TestSuite suite = new TestSuite( "Model-related tests for gemma-core" );

        suite.addTestSuite( AuditTrailDaoTest.class );
        suite.addTestSuite( AuditTrailServiceImplTest.class );
        suite.addTestSuite( UserDaoImplTest.class );

        suite.addTestSuite( BibliographicReferenceDaoImplTest.class );
        suite.addTestSuite( DatabaseEntryDaoImplTest.class );

        suite.addTestSuite( ArrayDesignDaoImplTest.class );
        suite.addTestSuite( BioAssayDaoImplTest.class );
        suite.addTestSuite( BioAssayDimensionDaoImplTest.class );
        suite.addTestSuite( BioMaterialDaoImplTest.class );
        suite.addTestSuite( BioSequencePersistTest.class );
        suite.addTestSuite( CandidateGeneListDaoImplTest.class );
        suite.addTestSuite( GeneDaoTest.class );

        suite.addTestSuite( BlatAssociationDaoImplTest.class );
        suite.addTestSuite( BlatResultDaoImplTest.class );

        suite.addTestSuite( ExpressionExperimentDaoImplTest.class );
        suite.addTestSuite( ExpressionExperimentDeleteTest.class );

        suite.addTestSuite( QtlDaoImplTest.class );
        suite.addTestSuite( DesignElementDataVectorDaoImplTest.class );
        suite.addTestSuite( Probe2ProbeCoexpressionDaoImplTest.class );
        suite.addTestSuite( CompositeSequenceDaoImplTest.class );
        suite.addTestSuite(Gene2GeneCoexpressionServiceTest.class);

        return suite;
    }
}
