package ubic.gemma.rest.util.args;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Test;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CompositeSequenceArgTest {

    @Test
    public void testValueOfNumericDispatchesToIdArg() {
        CompositeSequenceArg<?> arg = CompositeSequenceArg.valueOf( "555" );
        assertThat( arg ).isInstanceOf( CompositeSequenceIdArg.class );
        assertThat( arg.getValue() ).isEqualTo( 555L );
    }

    @Test
    public void testValueOfStringDispatchesToNameArg() {
        CompositeSequenceArg<?> arg = CompositeSequenceArg.valueOf( "201234_at" );
        assertThat( arg ).isInstanceOf( CompositeSequenceNameArg.class );
        assertThat( arg.getValue() ).isEqualTo( "201234_at" );
    }

    @Test
    public void testValueOfEmptyRaises() {
        assertThatThrownBy( () -> CompositeSequenceArg.valueOf( "" ) )
                .isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testValueOfBlankRaises() {
        assertThatThrownBy( () -> CompositeSequenceArg.valueOf( "   " ) )
                .isInstanceOf( MalformedArgException.class );
    }

    private static ArrayDesign platform( long id ) {
        ArrayDesign ad = new ArrayDesign();
        ad.setId( id );
        ad.setShortName( "GPL890" );
        return ad;
    }

    private static CompositeSequence probe( long id, String name, ArrayDesign platform ) {
        CompositeSequence cs = new CompositeSequence();
        cs.setId( id );
        cs.setName( name );
        cs.setArrayDesign( platform );
        return cs;
    }

    /**
     * A probe NAME can be entirely numeric -- Agilent feature-number arrays name every probe with a
     * bare integer -- and the numeric branch of {@link CompositeSequenceArg#valueOf} reads it as an
     * id. Verified on gemma2: the probe named "22575" on GPL890 has id 209787, so the id lookup
     * lands on an unrelated probe on another platform. The name lookup has to take over.
     */
    @Test
    public void numericNameIsFoundWhenTheIdBelongsToAnotherPlatform() {
        CompositeSequenceService service = mock( CompositeSequenceService.class );
        ArrayDesign gpl890 = platform( 9L );
        ArrayDesign other = platform( 77L );
        CompositeSequence wrong = probe( 22575L, "A_23_P55421", other );
        CompositeSequence right = probe( 209787L, "22575", gpl890 );

        when( service.load( 22575L ) ).thenReturn( wrong );
        when( service.findByName( gpl890, "22575" ) ).thenReturn( right );

        CompositeSequenceArg<?> arg = CompositeSequenceArg.valueOf( "22575" );
        assertThat( arg.getEntityWithPlatform( service, gpl890 ) ).isSameAs( right );
    }

    @Test
    public void numericNameIsFoundWhenNoSuchIdExistsAtAll() {
        CompositeSequenceService service = mock( CompositeSequenceService.class );
        ArrayDesign gpl890 = platform( 9L );
        CompositeSequence right = probe( 209787L, "22575", gpl890 );

        when( service.load( 22575L ) ).thenReturn( null );
        when( service.findByName( gpl890, "22575" ) ).thenReturn( right );

        assertThat( CompositeSequenceArg.valueOf( "22575" ).getEntityWithPlatform( service, gpl890 ) )
                .isSameAs( right );
    }

    /**
     * The id keeps precedence, and costs no extra query: a numeric argument that resolves to a probe
     * on the requested platform must not also trigger a name lookup.
     */
    @Test
    public void idOnTheRequestedPlatformWinsWithoutANameLookup() {
        CompositeSequenceService service = mock( CompositeSequenceService.class );
        ArrayDesign gpl890 = platform( 9L );
        CompositeSequence byId = probe( 22575L, "A_23_P55421", gpl890 );

        when( service.load( 22575L ) ).thenReturn( byId );

        assertThat( CompositeSequenceArg.valueOf( "22575" ).getEntityWithPlatform( service, gpl890 ) )
                .isSameAs( byId );
        verify( service, never() ).findByName( gpl890, "22575" );
    }

    /**
     * The pre-existing 400 is preserved where it was actually right: the id names a real probe on
     * another platform and nothing on this platform carries that name.
     */
    @Test
    public void idOnAnotherPlatformWithNoNameMatchStillRaises() {
        CompositeSequenceService service = mock( CompositeSequenceService.class );
        ArrayDesign gpl890 = platform( 9L );
        ArrayDesign other = platform( 77L );

        when( service.load( 22575L ) ).thenReturn( probe( 22575L, "A_23_P55421", other ) );
        when( service.findByName( gpl890, "22575" ) ).thenReturn( null );

        assertThatThrownBy( () -> CompositeSequenceArg.valueOf( "22575" ).getEntityWithPlatform( service, gpl890 ) )
                .isInstanceOf( BadRequestException.class );
    }

    /**
     * Neither an id nor a name match yields null, which the arg service turns into a 404.
     */
    @Test
    public void neitherIdNorNameYieldsNull() {
        CompositeSequenceService service = mock( CompositeSequenceService.class );
        ArrayDesign gpl890 = platform( 9L );

        when( service.load( 22575L ) ).thenReturn( null );
        when( service.findByName( gpl890, "22575" ) ).thenReturn( null );

        assertThat( CompositeSequenceArg.valueOf( "22575" ).getEntityWithPlatform( service, gpl890 ) ).isNull();
    }

    /**
     * The fallback looks up the spelling as written, not a re-rendering of the parsed long -- a probe
     * named "007" is not a probe named "7".
     */
    @Test
    public void nameFallbackPreservesLeadingZeroes() {
        CompositeSequenceService service = mock( CompositeSequenceService.class );
        ArrayDesign gpl890 = platform( 9L );
        CompositeSequence right = probe( 500L, "007", gpl890 );

        when( service.load( 7L ) ).thenReturn( null );
        when( service.findByName( gpl890, "007" ) ).thenReturn( right );

        assertThat( CompositeSequenceArg.valueOf( "007" ).getEntityWithPlatform( service, gpl890 ) )
                .isSameAs( right );
        verify( service, never() ).findByName( gpl890, "7" );
    }
}
