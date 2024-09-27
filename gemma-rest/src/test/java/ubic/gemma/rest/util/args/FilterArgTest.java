package ubic.gemma.rest.util.args;

import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Subquery;
import ubic.gemma.persistence.util.SubqueryMode;
import ubic.gemma.rest.util.MalformedArgException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

public class FilterArgTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private FilteringService<Identifiable> mockVoService;

    private void setUpMockVoService() {
        Set<String> universe = mock( Set.class );
        when( universe.contains( any( String.class ) ) ).thenReturn( true );
        when( mockVoService.getFilterableProperties() ).thenReturn( universe );
        when( mockVoService.getFilter( any(), any(), any( String.class ) ) )
                .thenAnswer( arg -> Filter.parse( "alias",
                        arg.getArgument( 0, String.class ),
                        String.class,
                        arg.getArgument( 1, Filter.Operator.class ),
                        arg.getArgument( 2, String.class ) ) );
    }

    @Test
    public void testEmptyFilter() {
        Filters filters = FilterArg.valueOf( "" ).getFilters( mockVoService );
        assertThat( filters ).isEmpty();
        verifyNoInteractions( mockVoService );
    }

    @Test
    public void testSimpleEquality() {
        setUpMockVoService();
        Filters filters = FilterArg.valueOf( "a = b" ).getFilters( mockVoService );
        assertThat( filters )
                .extracting( of -> of.get( 0 ) )
                .first()
                .hasFieldOrPropertyWithValue( "propertyName", "a" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "b" );
    }

    @Test
    public void testStringCannotContainSpace() {
        assertThatThrownBy( () -> FilterArg.valueOf( "a = bcd d" ) )
                .isInstanceOf( MalformedArgException.class )
                .cause()
                .isInstanceOf( InputMismatchException.class )
                .hasFieldOrPropertyWithValue( "offendingToken.startIndex", 8 );
    }

    @Test
    public void testParseInvalidOperator() {
        assertThatThrownBy( () -> FilterArg.valueOf( "a ~= bcd" ) )
                .isInstanceOf( MalformedArgException.class )
                .cause()
                .isInstanceOf( NoViableAltException.class )
                .hasFieldOrPropertyWithValue( "startToken.startIndex", 0 );
    }

    @Test
    public void testInvalidFilter() {
        assertThatThrownBy( () -> FilterArg.valueOf( "a =b" ).getFilters( mockVoService ) )
                .isInstanceOf( MalformedArgException.class )
                .cause()
                .isInstanceOf( NoViableAltException.class )
                .hasFieldOrPropertyWithValue( "startToken.startIndex", 0 );
    }

    @Test
    public void testConjunction() {
        setUpMockVoService();
        Filters filters = FilterArg.valueOf( "a = b and c = d" ).getFilters( mockVoService );
        assertThat( filters ).hasSize( 2 );
        assertThat( filters )
                .extracting( of -> of.get( 0 ) )
                .first()
                .hasFieldOrPropertyWithValue( "propertyName", "a" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "b" );
        assertThat( filters )
                .extracting( of -> of.get( 0 ) )
                .last()
                .hasFieldOrPropertyWithValue( "propertyName", "c" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "d" );
    }

    @Test
    public void testDisjunction() {
        setUpMockVoService();
        Filters filters = FilterArg.valueOf( "a = b, c = d" ).getFilters( mockVoService );
        assertThat( filters ).hasSize( 1 );
        assertThat( filters.iterator().next() )
                .hasSize( 2 );

        assertThat( filters.iterator().next().get( 0 ) )
                .hasFieldOrPropertyWithValue( "propertyName", "a" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "b" );

        assertThat( filters.iterator().next().get( 1 ) )
                .hasFieldOrPropertyWithValue( "propertyName", "c" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "d" );

        FilterArg.valueOf( "a = b or c = d" ).getFilters( mockVoService );

        assertThat( filters.iterator().next().get( 0 ) )
                .hasFieldOrPropertyWithValue( "propertyName", "a" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "b" );

        assertThat( filters.iterator().next().get( 1 ) )
                .hasFieldOrPropertyWithValue( "propertyName", "c" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "d" );
    }

    @Test
    public void testConjunctionOfDisjunctions() {
        setUpMockVoService();
        Filters filters = FilterArg.valueOf( "a = b or g = h and c = d or e = f" ).getFilters( mockVoService );

        assertThat( filters ).hasSize( 2 );

        Iterator<List<Filter>> iterator = filters.iterator();
        List<Filter> of;

        of = iterator.next();
        assertThat( of )
                .hasSize( 2 );
        assertThat( of.get( 0 ) )
                .hasFieldOrPropertyWithValue( "propertyName", "a" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "b" );

        assertThat( of.get( 1 ) )
                .hasFieldOrPropertyWithValue( "propertyName", "g" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "h" );

        of = iterator.next();
        assertThat( of.get( 0 ) )
                .hasFieldOrPropertyWithValue( "propertyName", "c" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "d" );

        assertThat( of.get( 1 ) )
                .hasFieldOrPropertyWithValue( "propertyName", "e" )
                .hasFieldOrPropertyWithValue( "operator", Filter.Operator.eq )
                .hasFieldOrPropertyWithValue( "requiredValue", "f" );
    }

    @Test
    public void testParseDate() {
        Set<String> universe = mock( Set.class );
        when( universe.contains( any( String.class ) ) ).thenReturn( true );
        when( mockVoService.getFilterableProperties() ).thenReturn( universe );
        when( mockVoService.getFilter( any(), any(), any( String.class ) ) )
                .thenAnswer( a -> Filter.parse( "alias", a.getArgument( 0 ), Date.class, a.getArgument( 1 ), a.getArgument( 2, String.class ) ) );
        FilterArg<Identifiable> fa = FilterArg.valueOf( "lastUpdated >= 2022-01-01" );
        Filters f = fa.getFilters( mockVoService );
        assertThat( f ).isNotNull();
        Filter subClause = f.iterator().next().get( 0 );
        assertThat( subClause )
                .hasFieldOrPropertyWithValue( "objectAlias", "alias" )
                .hasFieldOrPropertyWithValue( "propertyName", "lastUpdated" );
        assertThat( ( Date ) subClause.getRequiredValue() )
                .isEqualTo( OffsetDateTime.of( LocalDateTime.of( 2022, 1, 1, 0, 0, 0 ), ZoneOffset.UTC ).toInstant() );

        // let's reparse the toString() representation to ensure it's still a valid filter string
        // the only caveat is that the objectAlias will be prefixed again
        FilterArg<Identifiable> fa2 = FilterArg.valueOf( subClause.toString() );
        Filters f2 = fa2.getFilters( mockVoService );
        assertThat( f2 ).isNotNull();
        Filter subClause2 = f2.iterator().next().get( 0 );
        assertThat( subClause2 )
                .hasFieldOrPropertyWithValue( "objectAlias", "alias" )
                .hasFieldOrPropertyWithValue( "propertyName", "alias.lastUpdated" );
        assertThat( ( Date ) subClause2.getRequiredValue() )
                .isEqualTo( OffsetDateTime.of( LocalDateTime.of( 2022, 1, 1, 0, 0, 0 ), ZoneOffset.UTC ).toInstant() );
    }

    @Test
    public void testParseString() {
        setUpMockVoService();
        checkValidString( "a" );
        checkValidString( "https://john@example.com:8080/a;c=d?e=f&d=%20D#fragment" );
        checkValidString( "a+b" );
        checkValidString( "a_b-c" );
        checkValidString( "() ", "\"() \"" );
        checkValidString( "\"", "\"\\\"\"" );
    }

    private void checkValidString( String s ) {
        checkValidString( s, s );
    }

    private void checkValidString( String expected, String s ) {
        FilterArg<Identifiable> fa = FilterArg.valueOf( "foo >= " + s );
        Filters f = fa.getFilters( mockVoService );
        assertThat( f ).isNotNull();
        Filter subClause = f.iterator().next().get( 0 );
        assertThat( subClause )
                .hasFieldOrPropertyWithValue( "objectAlias", "alias" )
                .hasFieldOrPropertyWithValue( "propertyName", "foo" )
                .hasFieldOrPropertyWithValue( "requiredValue", expected );
    }

    @Test
    public void testParseStringUnescapeQuote() {
        setUpMockVoService();
        FilterArg<Identifiable> fa = FilterArg.valueOf( "foo >= \"\\\"\"" );
        Filters f = fa.getFilters( mockVoService );
        assertThat( f ).isNotNull();
        Filter subClause = f.iterator().next().get( 0 );
        assertThat( subClause )
                .hasFieldOrPropertyWithValue( "objectAlias", "alias" )
                .hasFieldOrPropertyWithValue( "propertyName", "foo" )
                .hasFieldOrPropertyWithValue( "requiredValue", "\"" );
    }

    @Test
    public void testParseCollection() {
        Set<String> universe = mock( Set.class );
        when( universe.contains( any( String.class ) ) ).thenReturn( true );
        when( mockVoService.getFilterableProperties() ).thenReturn( universe );
        //noinspection unchecked
        when( mockVoService.getFilter( any(), any(), anyCollection() ) )
                .thenAnswer( a -> Filter.parse( "alias", a.getArgument( 0 ), String.class,
                        a.getArgument( 1 ), a.getArgument( 2, Collection.class ) ) );
        checkValidCollection( Arrays.asList( "1", "2", "true", "foo" ), "(1, 2, true, foo)" );
    }

    @Test
    public void testParseEmptyCollectionRaisesMalformedArgException() {
        setUpMockVoService();
        //noinspection unchecked
        when( mockVoService.getFilter( any(), any(), anyCollection() ) )
                .thenAnswer( a -> Filter.parse( "alias", a.getArgument( 0 ), String.class,
                        a.getArgument( 1 ), a.getArgument( 2, Collection.class ) ) );
        assertThatThrownBy( () -> checkValidCollection( Collections.emptyList(), "()" ) )
                .isInstanceOf( MalformedArgException.class )
                .hasMessageContaining( "'alias.foo in ()' must be non-empty" );
        assertThatThrownBy( () -> checkValidCollection( Collections.emptyList(), "( )" ) )
                .isInstanceOf( MalformedArgException.class )
                .hasMessageContaining( "'alias.foo in ()' must be non-empty" );
    }

    @Test
    public void testParseSubqueryWithEmptyCollectionRaisesMalformedArgException() {
        setUpMockVoService();
        //noinspection unchecked
        when( mockVoService.getFilter( any(), any(), anyCollection() ) )
                .thenAnswer( a -> Filter.by( "alias", "id", Long.class, Filter.Operator.inSubquery,
                        new Subquery( "ExpressionExperiment", "id", Collections.emptyList(),
                                Filter.parse( null, a.getArgument( 0 ), String.class, a.getArgument( 1 ), ( Collection<String> ) a.getArgument( 2, Collection.class ) ) ) ) );
        assertThatThrownBy( () -> checkValidCollection( Collections.emptyList(), "()" ) )
                .isInstanceOf( MalformedArgException.class )
                .hasMessageContaining( "'foo in ()' must be non-empty" );
    }

    @Test
    public void testBase64EncodedFilter() {
        setUpMockVoService();
        FilterArg<Identifiable> arg = FilterArg.valueOf( "H4sIAAAAAAAAA8tMUbBVMAQA2dNQugYAAAA=" );
        Filters f = arg.getFilters( mockVoService );
        Filter subClause = f.iterator().next().get( 0 );
        assertThat( subClause )
                .hasFieldOrPropertyWithValue( "objectAlias", "alias" )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", "1" );
    }

    @Test
    public void testPropertyStartingWithGzipMagicNumber() {
        setUpMockVoService();
        FilterArg<Identifiable> arg = FilterArg.valueOf( "H4s = 1" );
        Filters f = arg.getFilters( mockVoService );
        Filter subClause = f.iterator().next().get( 0 );
        assertThat( subClause )
                .hasFieldOrPropertyWithValue( "objectAlias", "alias" )
                .hasFieldOrPropertyWithValue( "propertyName", "H4s" )
                .hasFieldOrPropertyWithValue( "requiredValue", "1" );
    }

    @Test
    public void testNone() {
        setUpMockVoService();
        FilterArg<Identifiable> arg = FilterArg.valueOf( "none(characteristics.category = disease)" );
        arg.getFilters( mockVoService );
        verify( mockVoService ).getFilter( "characteristics.category", Filter.Operator.eq, "disease", SubqueryMode.NONE );
    }

    @Test
    public void testAny() {
        setUpMockVoService();
        FilterArg<Identifiable> arg = FilterArg.valueOf( "any(characteristics.category = disease)" );
        arg.getFilters( mockVoService );
        verify( mockVoService ).getFilter( "characteristics.category", Filter.Operator.eq, "disease", SubqueryMode.ANY );
    }

    @Test
    public void testAll() {
        setUpMockVoService();
        FilterArg<Identifiable> arg = FilterArg.valueOf( "all(characteristics.category = disease)" );
        arg.getFilters( mockVoService );
        verify( mockVoService ).getFilter( "characteristics.category", Filter.Operator.eq, "disease", SubqueryMode.ALL );
    }

    private void checkValidCollection( Collection<String> expected, String input ) {
        FilterArg<Identifiable> fa = FilterArg.valueOf( "foo in " + input );
        Filters f = fa.getFilters( mockVoService );
        Filter subClause = f.iterator().next().get( 0 );
        assertThat( subClause )
                .hasFieldOrPropertyWithValue( "objectAlias", "alias" )
                .hasFieldOrPropertyWithValue( "propertyName", "foo" )
                .hasFieldOrPropertyWithValue( "requiredValue", expected );
    }
}