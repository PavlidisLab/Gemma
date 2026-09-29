/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest.providers;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.rest.annotations.Costly;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the half of the mechanism that hands permits back, and for the startup check that every
 * {@link Costly} annotation names a pool that exists.
 */
class CostlyEndpointApplicationEventListenerTest {

    /**
     * Stands in for an annotated resource class. Deliberately carries no JAX-RS annotations: anything
     * under {@code ubic.gemma.rest} with an {@code @Path} is picked up by the package scan in
     * {@code BaseJerseyTest5}, and a fixture would then show up as a real endpoint that
     * {@code OpenApiTest} holds to the spec contract. The resource model below is built by hand
     * instead.
     */
    @SuppressWarnings("unused")
    public static class FakeResource {

        @Costly("search")
        public String wellNamed() {
            return "";
        }

        @Costly("serch")
        public String misspelledPool() {
            return "";
        }
    }

    private CostlyEndpointBudget budget;
    private CostlyEndpointApplicationEventListener listener;

    @BeforeEach
    void setUp() {
        budget = mock( CostlyEndpointBudget.class );
        listener = new CostlyEndpointApplicationEventListener();
        ReflectionTestUtils.setField( listener, "budget", budget );
    }

    private static ApplicationEvent initializationFinished( ResourceModel model ) {
        ApplicationEvent event = mock( ApplicationEvent.class );
        when( event.getType() ).thenReturn( ApplicationEvent.Type.INITIALIZATION_FINISHED );
        when( event.getResourceModel() ).thenReturn( model );
        return event;
    }

    /**
     * A one-route resource model whose GET is handled by the named {@link FakeResource} method, which
     * is what {@code checkPoolsExist} reads the annotation off.
     */
    private static ResourceModel modelFor( String methodName ) throws NoSuchMethodException {
        Resource.Builder resource = Resource.builder( "/fake" );
        resource.addMethod( "GET" )
                .handledBy( FakeResource.class, FakeResource.class.getDeclaredMethod( methodName ) );
        return new ResourceModel.Builder( false ).addResource( resource.build() ).build();
    }

    private static ResourceModel modelOf( Class<?>... resources ) {
        ResourceModel.Builder model = new ResourceModel.Builder( false );
        for ( Class<?> r : resources ) {
            model.addResource( Resource.from( r ) );
        }
        return model.build();
    }

    private static RequestEvent finished( ContainerRequest request ) {
        RequestEvent event = mock( RequestEvent.class );
        when( event.getType() ).thenReturn( RequestEvent.Type.FINISHED );
        when( event.getContainerRequest() ).thenReturn( request );
        return event;
    }

    @Test
    void aPoolNameThatExistsPassesTheStartupCheck() throws Exception {
        ResourceModel model = modelFor( "wellNamed" );
        assertThatCode( () -> listener.onEvent( initializationFinished( model ) ) )
                .doesNotThrowAnyException();
    }

    @Test
    void aMisspelledPoolNameFailsStartupRatherThanGoingUnbounded() throws Exception {
        ResourceModel model = modelFor( "misspelledPool" );
        assertThatThrownBy( () -> listener.onEvent( initializationFinished( model ) ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "serch" )
                .hasMessageContaining( "search" );
    }

    @Test
    void everyCostlyRouteInTheApplicationNamesAKnownPool() {
        // The real annotations, checked the same way the application checks them at startup.
        ResourceModel model = modelOf(
                ubic.gemma.rest.DatasetsWebService.class,
                ubic.gemma.rest.AnnotationsWebService.class,
                ubic.gemma.rest.GeneWebService.class,
                ubic.gemma.rest.GoTermsWebService.class,
                ubic.gemma.rest.SearchWebService.class );
        assertThatCode( () -> listener.onEvent( initializationFinished( model ) ) )
                .doesNotThrowAnyException();
    }

    @Test
    void aFinishedRequestHandsItsPermitBack() {
        ContainerRequest request = mock( ContainerRequest.class );
        when( request.getProperty( CostlyEndpointFilter.POOL_PROPERTY ) ).thenReturn( "vectors" );

        RequestEventListener release = listener.onRequest( mock( RequestEvent.class ) );
        release.onEvent( finished( request ) );

        verify( budget ).release( "vectors" );
    }

    @Test
    void aRequestThatNeverTookAPermitReleasesNothing() {
        ContainerRequest request = mock( ContainerRequest.class );
        when( request.getProperty( CostlyEndpointFilter.POOL_PROPERTY ) ).thenReturn( null );

        RequestEventListener release = listener.onRequest( mock( RequestEvent.class ) );
        release.onEvent( finished( request ) );

        verify( budget, never() ).release( anyString() );
    }

    @Test
    void anEventOtherThanFinishedDoesNotReleaseEarly() {
        ContainerRequest request = mock( ContainerRequest.class );
        when( request.getProperty( CostlyEndpointFilter.POOL_PROPERTY ) ).thenReturn( "vectors" );
        RequestEvent responseFiltered = mock( RequestEvent.class );
        when( responseFiltered.getType() ).thenReturn( RequestEvent.Type.RESP_FILTERS_FINISHED );

        RequestEventListener release = listener.onRequest( mock( RequestEvent.class ) );
        release.onEvent( responseFiltered );

        // RESP_FILTERS_FINISHED lands before a streamed entity is written; releasing there would
        // uncap the pool for the length of every download.
        verify( budget, never() ).release( anyString() );
    }

    @Test
    void aPermitIsHandedBackOnlyOnce() {
        ContainerRequest request = mock( ContainerRequest.class );
        // Mirrors ContainerRequest: the property is gone once removeProperty has been called.
        when( request.getProperty( CostlyEndpointFilter.POOL_PROPERTY ) )
                .thenReturn( "vectors" )
                .thenReturn( null );

        RequestEventListener release = listener.onRequest( mock( RequestEvent.class ) );
        release.onEvent( finished( request ) );
        release.onEvent( finished( request ) );

        verify( request ).removeProperty( CostlyEndpointFilter.POOL_PROPERTY );
        verify( budget, times( 1 ) ).release( anyString() );
    }
}
