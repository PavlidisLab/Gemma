package ubic.gemma.persistence.util;

import org.junit.Test;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class EntityUrlBuilderTest {

    private final EntityUrlBuilder entityUrlBuilder = new EntityUrlBuilder( "" );

    @Test
    public void test() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 12L );
        ee.setShortName( "GSE001292" );
        assertEquals( "/expressionExperiment/showExpressionExperiment.html?id=12", entityUrlBuilder.fromHostUrl().entity( ee ).web().toUriString() );
        assertEquals( "/expressionExperiment/showExpressionExperiment.html?id=12", entityUrlBuilder.fromHostUrl().entity( ExpressionExperiment.class, 12L ).web().toUriString() );
        assertEquals( "/expressionExperiment/showExpressionExperiment.html?shortName=GSE001292", entityUrlBuilder.fromHostUrl().entity( ee ).web().byShortName().toUriString() );
        assertEquals( "/expressionExperiment/showAllExpressionExperiments.html?id=1,2,3", entityUrlBuilder.fromHostUrl().some( ExpressionExperiment.class, Arrays.asList( 1L, 2L, 3L ) ).web().toUriString() );
        assertEquals( "/expressionExperiment/showAllExpressionExperiments.html?id=12", entityUrlBuilder.fromHostUrl().some( Collections.singletonList( ee ) ).web().toUriString() );
        assertEquals( "/rest/v2/datasets/12", entityUrlBuilder.fromHostUrl().entity( ee ).rest().toUriString() );
        ArrayDesign ad = new ArrayDesign();
        ad.setId( 3L );
        ad.setShortName( "GPL10920" );
        assertEquals( "/arrays/showArrayDesign.html?id=3", entityUrlBuilder.fromHostUrl().entity( ad ).web().toUriString() );
        assertEquals( "/arrays/showArrayDesign.html?shortName=GPL10920", entityUrlBuilder.fromHostUrl().entity( ad ).web().byShortName().toUriString() );
        assertEquals( "/arrays/showAllArrayDesigns.html?id=1,2,3", entityUrlBuilder.fromHostUrl().some( ArrayDesign.class, Arrays.asList( 1L, 2L, 3L ) ).web().toUriString() );
        assertEquals( "/rest/v2/platforms/3", entityUrlBuilder.fromHostUrl().entity( ad ).rest().toUriString() );
        assertEquals( "/rest/v2/platforms", entityUrlBuilder.fromHostUrl().all( ArrayDesign.class ).rest().toUriString() );

        // no web/rest is set by default
        assertThrows( IllegalStateException.class, () -> entityUrlBuilder.fromHostUrl().entity( ee ).toUriString() );

        Gene gene = new Gene();
        gene.setId( 4L );
        assertEquals( "/rest/v2/genes/4", entityUrlBuilder.fromHostUrl().entity( gene ).rest().toUriString() );
    }

    @Test
    public void testWebByDefault() {
        EntityUrlBuilder eub = new EntityUrlBuilder( "" );
        eub.setWebByDefault();
        assertThrows( IllegalStateException.class, eub::setRestByDefault );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 12L );
        ee.setShortName( "GSE001292" );
        assertEquals( "/expressionExperiment/showExpressionExperiment.html?id=12", eub.fromHostUrl().entity( ee ).toUriString() );
    }

    @Test
    public void testRestByDefault() {
        EntityUrlBuilder eub = new EntityUrlBuilder( "" );
        eub.setRestByDefault();
        assertThrows( IllegalStateException.class, eub::setWebByDefault );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 12L );
        ee.setShortName( "GSE001292" );
        assertEquals( "/rest/v2/datasets/12", eub.fromHostUrl().entity( ee ).toUriString() );
    }

    @Test
    public void testFactorValueOntologyUrl() {
        FactorValue fv = new FactorValue();
        fv.setId( 2L );
        assertEquals( "/ont/TGFVO/2", entityUrlBuilder.fromHostUrl().entity( fv ).ont().toUriString() );
        Characteristic c = new Characteristic();
        c.setId( 3L );
        c.setValueUri( "http://gemma.msl.ubc.ca/ont/TGEMO_00166" );
        assertEquals( "/ont/TGEMO_00166", entityUrlBuilder.fromHostUrl().entity( c ).ont().toUriString() );
        Characteristic c2 = new Characteristic();
        c.setId( 4L );
        c.setValueUri( "http://purl.obolibrary.org/ont/NCBITaxon_9606" );
        assertThrows( UnsupportedOperationException.class, () -> entityUrlBuilder.fromHostUrl().entity( c ).ont().toUriString() );
    }
}