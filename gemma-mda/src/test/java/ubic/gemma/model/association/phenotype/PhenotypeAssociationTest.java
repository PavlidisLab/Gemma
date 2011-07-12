package ubic.gemma.model.association.phenotype;

import static org.junit.Assert.*;

import java.util.Collection;

import junit.tests.framework.AssertTest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.testing.BaseSpringContextTest;

public class PhenotypeAssociationTest extends BaseSpringContextTest {

    @Autowired
    private UrlEvidenceDao eviDao;

    @Test
    public void testUrlEvidence() {

        // create
        UrlEvidence urlEvidence = new UrlEvidenceImpl();
        urlEvidence.setDescription( "testDescription" );
        urlEvidence.setName( "testname" );
        urlEvidence.setUrl( "www.test.com" );
        UrlEvidence entityReturn = eviDao.create( urlEvidence );
        assertNotNull( entityReturn.getId() );

        // update
        urlEvidence.setUrl( "www.testupdate.com" );
        eviDao.update( urlEvidence );

        // load
        UrlEvidence urlEvidenceLoad = eviDao.load( entityReturn.getId() );
        assertNotNull( urlEvidenceLoad );
        assertTrue( urlEvidenceLoad.getUrl().equals( "www.testupdate.com" ) );

        // remove
        eviDao.remove( entityReturn.getId() );
        assertNull( eviDao.load( entityReturn.getId() ) );
    }

}
