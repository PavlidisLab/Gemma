package ubic.gemma.model.association.phenotype;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.testing.BaseSpringContextTest;

public class PhenotypeAssociationTest extends BaseSpringContextTest {

    @Autowired
    private UrlEvidenceDao eviDao;

    @Autowired
    private LiteratureEvidenceDao litDao;

    @Test
    public void createUrlEvidence() {
        UrlEvidence entity = new UrlEvidenceImpl();
        entity.setDescription( "testDescription" );
        entity.setName( "testname" );
        entity.setUrl( "www.test.com" );
        eviDao.create( entity );
    }

    @Test
    public void createLiteratureEvidence() {
        LiteratureEvidence entity = new LiteratureEvidenceImpl();
        entity.setDescription( "testDescription2" );
        entity.setName( "testname2" );

        litDao.create( entity );

        entity.setName( "testDescription2Update" );

        litDao.update( entity );
    }

    @Test
    public void findAllLiteratureEvidence() {
        Collection<LiteratureEvidence> c = ( Collection<LiteratureEvidence> ) litDao.loadAll();

        // check all
        for ( LiteratureEvidence literatureEvidence : c ) {
            System.out.println( literatureEvidence.getDescription() );
        }
    }

}
