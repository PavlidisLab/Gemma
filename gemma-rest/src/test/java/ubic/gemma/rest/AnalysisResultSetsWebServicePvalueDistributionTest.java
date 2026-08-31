/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.core.analysis.service.ExpressionAnalysisResultSetFileService;
import ubic.gemma.core.util.math.distribution.Histogram;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.DatabaseEntryArgService;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.ExpressionAnalysisResultSetArg;
import ubic.gemma.rest.util.args.ExpressionAnalysisResultSetArgService;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AnalysisResultSetsWebService#getPvalueDistribution}, which serves the
 * stored {@code PVALUE_DISTRIBUTION} histogram rather than aggregating
 * {@code DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT} (120.5 GB / ~1.57e9 rows on production) per
 * request. Pure Mockito — what is pinned is the routing, the rebinning arithmetic and the four
 * refusals, not the DAO.
 */
@ExtendWith(MockitoExtension.class)
public class AnalysisResultSetsWebServicePvalueDistributionTest {

    /**
     * What {@code addPvalueDistribution} writes: 100 fixed-width bins over [0, 1].
     */
    private static final int STORED_BINS = 100;

    @Mock
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;
    @Mock
    @SuppressWarnings("unused") // injected by Mockito to satisfy @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Mock
    @SuppressWarnings("unused")
    private ExpressionAnalysisResultSetFileService expressionAnalysisResultSetFileService;
    @Mock
    private ExpressionAnalysisResultSetArgService expressionAnalysisResultSetArgService;
    @Mock
    @SuppressWarnings("unused")
    private DatasetArgService datasetArgService;
    @Mock
    @SuppressWarnings("unused")
    private DatabaseEntryArgService databaseEntryArgService;

    @InjectMocks
    private AnalysisResultSetsWebService webService;

    private ExpressionAnalysisResultSet ears;
    private ExpressionAnalysisResultSetArg arg;

    @BeforeEach
    public void setUp() {
        ears = new ExpressionAnalysisResultSet();
        ears.setId( 42L );
        arg = ExpressionAnalysisResultSetArg.valueOf( "42" );
    }

    /**
     * A stored histogram whose bin i holds i + 1 counts, so every merge is checkable by hand.
     */
    private Histogram storedHistogram() {
        Histogram h = new Histogram( "42", STORED_BINS, 0.0, 1.0 );
        for ( int i = 0; i < STORED_BINS; i++ ) {
            h.fill( i, i + 1 );
        }
        return h;
    }

    private void stubStored( Histogram h ) {
        when( expressionAnalysisResultSetArgService.getEntity( any( ExpressionAnalysisResultSetArg.class ) ) )
                .thenReturn( ears );
        when( expressionAnalysisResultSetService.loadPvalueDistribution( ears ) ).thenReturn( h );
    }

    private static PvalueDistributionValueObject payloadOf( Response response ) {
        assertThat( response.getStatus() ).isEqualTo( 200 );
        Object entity = response.getEntity();
        assertThat( entity ).isInstanceOf( ResponseDataObject.class );
        Object data = ( ( ResponseDataObject<?> ) entity ).getData();
        assertThat( data ).isInstanceOf( PvalueDistributionValueObject.class );
        return ( PvalueDistributionValueObject ) data;
    }

    // -----------------------------------------------------------------------
    // it serves the STORED histogram, and nothing else
    // -----------------------------------------------------------------------

    @Test
    public void servesTheStoredHistogramVerbatimAt100Bins() {
        stubStored( storedHistogram() );

        PvalueDistributionValueObject vo = payloadOf( webService.getPvalueDistribution( arg, 100, "raw" ) );

        assertThat( vo.getResultSetId() ).isEqualTo( 42L );
        assertThat( vo.getColumn() ).isEqualTo( "raw" );
        assertThat( vo.getBins() ).hasSize( 100 );
        // bin i of the stored histogram held i + 1; served unchanged
        for ( int i = 0; i < 100; i++ ) {
            assertThat( vo.getBins().get( i ).getCount() )
                    .describedAs( "bin %d", i )
                    .isEqualTo( i + 1 );
        }
        // 1 + 2 + ... + 100
        assertThat( vo.getN() ).isEqualTo( 5050L );
        verify( expressionAnalysisResultSetService ).loadPvalueDistribution( ears );
    }

    @Test
    public void downBinningMergesWholeStoredBinsAndPreservesTheTotal() {
        stubStored( storedHistogram() );

        // 20 is the default and what gemma-ui sends: 100 / 20 = 5 stored bins per output bin
        PvalueDistributionValueObject vo = payloadOf( webService.getPvalueDistribution( arg, 20, "raw" ) );

        assertThat( vo.getBins() ).hasSize( 20 );
        for ( int j = 0; j < 20; j++ ) {
            // stored bins 5j..5j+4 held 5j+1 .. 5j+5
            long expected = 0;
            for ( int i = 5 * j; i < 5 * j + 5; i++ ) {
                expected += i + 1;
            }
            assertThat( vo.getBins().get( j ).getCount() )
                    .describedAs( "output bin %d", j )
                    .isEqualTo( expected );
        }
        // merging never loses or invents counts
        assertThat( vo.getN() ).isEqualTo( 5050L );
        // and the edges still tile [0, 1]
        assertThat( vo.getBins().get( 0 ).getLo() ).isEqualTo( 0.0 );
        assertThat( vo.getBins().get( 19 ).getHi() ).isEqualTo( 1.0 );
    }

    // -----------------------------------------------------------------------
    // bins: only divisors of the stored bin count
    // -----------------------------------------------------------------------

    @Test
    public void everyDivisorOf100IsAcceptedIncludingTheDefault20() {
        stubStored( storedHistogram() );
        for ( int bins : new int[] { 1, 2, 4, 5, 10, 20, 25, 50, 100 } ) {
            PvalueDistributionValueObject vo = payloadOf( webService.getPvalueDistribution( arg, bins, "raw" ) );
            assertThat( vo.getBins() )
                    .describedAs( "bins=%d", bins )
                    .hasSize( bins );
            assertThat( vo.getN() )
                    .describedAs( "bins=%d", bins )
                    .isEqualTo( 5050L );
        }
    }

    @Test
    public void aNonDivisorBinCountIsRejectedWithTheAcceptedValues() {
        stubStored( storedHistogram() );

        // 3 does not divide 100; rebinning would split stored counts across output bins
        assertThatThrownBy( () -> webService.getPvalueDistribution( arg, 3, "raw" ) )
                .isInstanceOf( BadRequestException.class )
                .hasMessageContaining( "1, 2, 4, 5, 10, 20, 25, 50, 100" );

        // the old contract advertised 1..1000; 7 and 1000 were both legal then
        assertThatThrownBy( () -> webService.getPvalueDistribution( arg, 7, "raw" ) )
                .isInstanceOf( BadRequestException.class );
        assertThatThrownBy( () -> webService.getPvalueDistribution( arg, 1000, "raw" ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void aBinCountBelowOneIsRejectedBeforeAnythingIsLoaded() {
        assertThatThrownBy( () -> webService.getPvalueDistribution( arg, 0, "raw" ) )
                .isInstanceOf( BadRequestException.class );
        verify( expressionAnalysisResultSetService, never() ).loadPvalueDistribution( any() );
    }

    // -----------------------------------------------------------------------
    // column: 'corrected' is refused, never silently answered with raw
    // -----------------------------------------------------------------------

    @Test
    public void correctedIsRejectedRatherThanSilentlyServedTheRawDistribution() {
        assertThatThrownBy( () -> webService.getPvalueDistribution( arg, 20, "corrected" ) )
                .isInstanceOf( BadRequestException.class )
                .hasMessageContaining( "raw" );
        // nothing was loaded, so nothing could have been mislabelled as corrected
        verify( expressionAnalysisResultSetService, never() ).loadPvalueDistribution( any() );
    }

    @Test
    public void anUnknownColumnIsRejected() {
        assertThatThrownBy( () -> webService.getPvalueDistribution( arg, 20, "bogus" ) )
                .isInstanceOf( BadRequestException.class );
        verify( expressionAnalysisResultSetService, never() ).loadPvalueDistribution( any() );
    }

    /**
     * The wire default has to be raw. A direct method call bypasses JAX-RS parameter binding, so
     * this reads the {@code @DefaultValue} off the production method instead.
     */
    @Test
    public void theWireDefaultsAreRawAnd20Bins() {
        assertThat( defaultValueOfQueryParam( "column" ) ).isEqualTo( "raw" );
        assertThat( defaultValueOfQueryParam( "bins" ) ).isEqualTo( "20" );
    }

    private static String defaultValueOfQueryParam( String name ) {
        Method m;
        try {
            m = AnalysisResultSetsWebService.class.getMethod( "getPvalueDistribution",
                    ExpressionAnalysisResultSetArg.class, int.class, String.class );
        } catch ( NoSuchMethodException e ) {
            throw new AssertionError( e );
        }
        for ( Annotation[] annotations : m.getParameterAnnotations() ) {
            String queryParam = null;
            String defaultValue = null;
            for ( Annotation a : annotations ) {
                if ( a instanceof QueryParam ) {
                    queryParam = ( ( QueryParam ) a ).value();
                } else if ( a instanceof DefaultValue ) {
                    defaultValue = ( ( DefaultValue ) a ).value();
                }
            }
            if ( name.equals( queryParam ) ) {
                return defaultValue;
            }
        }
        throw new AssertionError( "no @QueryParam(\"" + name + "\") on getPvalueDistribution" );
    }

    // -----------------------------------------------------------------------
    // the two empty conditions are kept apart
    // -----------------------------------------------------------------------

    @Test
    public void noStoredHistogramIs404NotAnEmpty200Or204() {
        stubStored( null );

        assertThatThrownBy( () -> webService.getPvalueDistribution( arg, 20, "raw" ) )
                .isInstanceOf( NotFoundException.class )
                .hasMessageContaining( "no stored p-value distribution" );
    }

    @Test
    public void anAllZeroStoredHistogramIs204() {
        // the row exists and has its 100 bins; the analysis just produced no non-null p-values
        stubStored( new Histogram( "42", STORED_BINS, 0.0, 1.0 ) );

        Response response = webService.getPvalueDistribution( arg, 20, "raw" );

        assertThat( response.getStatus() ).isEqualTo( 204 );
        assertThat( response.getEntity() ).isNull();
    }

    @Test
    public void aStoredHistogramWithNoBinsIs204() {
        stubStored( new Histogram( "42", 0, 0.0, 1.0 ) );

        Response response = webService.getPvalueDistribution( arg, 20, "raw" );

        assertThat( response.getStatus() ).isEqualTo( 204 );
    }
}
