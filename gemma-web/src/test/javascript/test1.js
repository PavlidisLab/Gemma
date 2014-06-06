describe( "Basic Assumptions", function() {
   it( "has ExtJS loaded", function() {
      expect( Ext ).toBeDefined();
      expect( Ext.version ).toBeTruthy();
      expect( Ext.versionDetail.major ).toEqual( 3 );
   } );
} );

describe( "CoexVOUtil", function() {
   it( "getEntityIds", function() {

      expect( Gemma.CoexVOUtil.getEntityIds( [ {
         id : 1
      }, {
         id : 2
      }, {
         id : 3
      } ] ) ).toEqual( [ 1, 2, 3 ] );

   } );

   it( "getAllGeneIds", function() {

      expect( Gemma.CoexVOUtil.getAllGeneIds( [ {
         id : 1,
         queryGene : {
            id : 10
         },
         foundGene : {
            id : 11
         }
      }, {
         id : 2,
         queryGene : {
            id : 10
         },
         foundGene : {
            id : 12
         }
      }, {
         id : 3,
         queryGene : {
            id : 14
         },
         foundGene : {
            id : 15
         }
      } ] ).length ).toEqual( 5 );

   } );
} );

/**
 * A more complex integration test that includes a delay, mocking of a server-side DWR-based call, and testing
 * triggering of an event.
 */
describe( "search For Genes with empty query", function() {

   // jasmine-jquery setup for fixtures. We should make this a global call somewhere...
   jasmine.getJSONFixtures().fixturesPath = 'spec/fixtures/json/';

   /*
    * This style of beforeEach, with the 'done' parameter, allows you to run test setup and then wait a while - the
    * specs will not run until you call done().
    */
   beforeEach( function( done ) {

      // scope 'this' is shared by all the specs
      this.mock = new FakeDWR( GenePickerController );

      // have to call some setup stuff first, before the form is built.

      /*
       * Specify that the searchGenesAndGeneGroups will return data. GenePickerControllerResultOfEmptyQuery.json is a
       * file containing the json-version of what the server returns and is passed to the DWR callback. To generate the
       * data, I used a live server and captured the javascript, pasted it into the javascript console in Chrome, and
       * then
       */
      this.data = getJSONFixture( 'GenePickerControllerResultOfEmptyQuery.json' );
      this.mock.mock_method( "searchGenesAndGeneGroups", this.data );

      /*
       * Now we're spying on the method that is already wrapped by FakeDWR. Adding and.callThrough() is important;
       * without it the method would not actually get executed. That would be okay if we just want to know if the method
       * was called but I want to see if the method does what it is supposed to do. This is basically essential for
       * these DWR methods.
       */
      spyOn( GenePickerController, "searchGenesAndGeneGroups" ).and.callThrough();

      /*
       * Set up a search form (AnalysisResultsSearchForm)
       */
      var coexpressionSearchData = new Gemma.CoexpressionSearchData();
      this.searchForm = new Gemma.AnalysisResultsSearchForm( {
         width : Gemma.SEARCH_FORM_WIDTH,
         observableSearchResults : coexpressionSearchData
      } );

      /*
       * Spy on a an event so we can test it was fired
       */
      this.se = spyOnEventExt( this.searchForm.geneSearchAndPreview.geneCombo.store, 'load' );

      /*
       * Spy on another method. 'and.callThrough()' is important to ensure that the method actually gets called, as
       * opposed to simplying checking if it was called. If we don't add that, then the records will not be loaded. That
       * would be okay for a unit test, but this is more of an integration test.
       */
      spyOn( this.searchForm.geneSearchAndPreview.geneCombo.store.proxy, 'request' ).and.callThrough();

      /*
       * Give the genecombo focus with no input. Should show the 'default' gene sets. This starts an asynchronous call
       * to the server in real life. In our test, we have the DWR call mocked.
       */
      this.searchForm.geneSearchAndPreview.geneCombo.setValue( '' );
      this.searchForm.geneSearchAndPreview.geneCombo.fireEvent( 'focus' );
      // note that calling focus() doesn't trigger the event, so we trigger it manually.

      /*
       * wait for the call to be triggered; there is a delay of 1200ms before we do an empty search (see
       * GeneAndGeneGroupCombo.js). Otherwise would not need this much time. Another way to do this would be to add a
       * listener for an event that happens when the call is done (e.g., store.load) but in this case I want to test
       * some of those events.
       */
      setTimeout( function() {
         done();
      }, 1300 );

   } );

   /*
    * The actual test.
    */
   it( 'checks GenePickerController.searchGenesAndGeneGroups was called and records loaded', function() {

      /*
       * jasmine.any lets us specify a parameter must be present, without specifying what it is exactly. So here we are
       * saying that the callback and errorHandler should be Functions.
       */
      expect( GenePickerController.searchGenesAndGeneGroups ).toHaveBeenCalledWith( '', null, {
         callback : jasmine.any( Function ),
         errorHandler : jasmine.any( Function )
      } );

      expect( this.searchForm.geneSearchAndPreview.geneCombo.store.proxy.request ).toHaveBeenCalled();

      expect( this.se ).toHaveBeenTriggered();

      expect( this.searchForm.geneSearchAndPreview.geneCombo.store.getCount() ).toEqual( 4 );

   } );

   /*
    * Teardown
    */
   afterEach( function() {
      this.searchForm.destroy();
      this.mock.unmock_method( "searchGenesAndGeneGroups" );
   } );

} );

describe("check user groups manager is okay", function() {
   
   /*
    * Log in.
    */
   
});


