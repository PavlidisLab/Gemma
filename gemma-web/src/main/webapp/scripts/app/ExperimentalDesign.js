/**
 * Experimental design editor application.
 */

Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';

var serverFilePath = "";

var showDesignUploadForm = function() {

   var uploadForm = new Gemma.FileUploadForm( {
      title : 'Select the design file',
      id : 'upload-design-form',
      style : 'margin : 5px',
      allowBlank : false
   } );

   uploadForm.on( 'start', function( taskId ) {
      Ext.getCmp( 'submit-design-button' ).disable();
   }.createDelegate( this ) );

   uploadForm.on( 'finish', function( result ) {
      if ( result.success ) {
         Ext.getCmp( 'submit-design-button' ).enable();
         serverFilePath = result.localFile;
      }
   } );

   var w = new Ext.Window( {
      title : "Experimental design upload",
      closeAction : 'close',
      id : 'experimental-design-upload-form-window',
      width : 550,
      items : [ {
         xtype : 'panel',
         collapsible : true,
         title : 'Instructions',
         collapsed : false,
         frame : false,
         border : true,
         html : Gemma.HelpText.WidgetDefaults.ExperimentalDesignUpload.instructions
      }, uploadForm ],
      buttons : [ {
         id : 'submit-design-button',
         value : 'Submit',
         handler : submitDesign,
         text : "Submit dataset",
         disabled : true
      }, {
         value : 'Cancel',
         text : 'Cancel',
         enabled : true,
         handler : function( button, ev ) {
            w.close();
         }
      } ]
   } );

   w.show();

   w.loadMask = new Ext.LoadMask( 'experimental-design-upload-form-window', {
      msg : "Submitting..."
   } );
};

var submitDesign = function() {

   Ext.getCmp( 'experimental-design-upload-form-window' ).loadMask.show();

   ExperimentalDesignController.createDesignFromFile( dwr.util.getValue( "expressionExperimentID" ), serverFilePath, {
      callback : function() {
         Ext.getCmp( 'experimental-design-upload-form-window' ).loadMask.hide();
         Ext.Msg.alert( "Success", "Design imported." );
         Ext.getCmp( 'experimental-factor-grid' ).getStore().reload();

      },
      errorHandler : function( errorString, exception ) {

         Ext.Msg.alert( 'Error', errorString );
         Ext.getCmp( 'experimental-design-upload-form-window' ).loadMask.hide();
      }
   } );
};

Ext.onReady( function() {

   Ext.QuickTips.init();
   Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

   var eeId = Ext.get( "expressionExperimentID" ).getValue();
   var edId = Ext.get( "experimentalDesignID" ).getValue();
   var editable = Ext.get( 'currentUserCanEdit' ).getValue() === 'true';
   var taxonId = Ext.get( 'taxonId' ).getValue();

   /*
    * If we init before the tab is rendered, then the scroll bars don't show up.
    */
   var experimentalFactorGrid = new Gemma.ExperimentalFactorGrid( {
      id : 'experimental-factor-grid',
      title : "Experimental Factors",
      region : 'north',
      edId : edId,
      editable : editable,
      split : true,
      // north items must have height.
      height : 200
   } );

   var factorValueGrid = new Gemma.FactorValueGrid( {
      title : "Factor Values",
      region : 'center',
      id : 'factor-value-grid',
      form : 'factorValueForm', // hack
      split : true,
      edId : edId,
      height : 470,
      editable : editable,
      taxonId : taxonId
   } );

   var efPanel = new Ext.Panel( {
      layout : 'border',
      height : 670,
      renderTo : "experimentalFactorPanel",
      items : [ experimentalFactorGrid, factorValueGrid ]
   } );

   var bioMaterialEditor = new Gemma.BioMaterialEditor( {
      renderTo : "bioMaterialsPanel",
      id : 'biomaterial-grid-panel',
      height : 670,
      eeId : eeId,
      edId : edId,
      viewConfig : {
         forceFit : true
      },
      editable : editable
   } );

   var tabPanel = new Ext.TabPanel( {
      renderTo : "experimentalDesignPanel",
      layoutOnTabChange : false,
      activeTab : 0,
      height : 700,
      items : [ {
         contentEl : "experimentalFactorPanel",
         title : "Design setup"
      }, {
         contentEl : "bioMaterialsPanel",
         title : "Sample details"
      } ]
   } );

   /*
    * Only initialize once we are viewing the tab to help ensure the scroll bars are rendered right away.
    */
   var refreshNeeded = false;

   tabPanel.on( 'tabchange', function( panel, tab ) {
      if ( refreshNeeded || !bioMaterialEditor.firstInitDone && tab.contentEl == 'bioMaterialsPanel' ) {
         bioMaterialEditor.init();
         refreshNeeded = false;
      }
   } );

   experimentalFactorGrid.on( "experimentalfactorchange", function( efgrid, efs, factor ) {
      factorValueGrid.getEl().unmask();
      if ( factor && factor.get( "name" ) && factor.get( "id" ) ) {
         factorValueGrid.setTitle( "Factor values for : " + factor.get( "name" ) );
         factorValueGrid.setExperimentalFactor( factor.get( "id" ) );
      } else {
         // for example, if a delete was performed
         factorValueGrid.getStore().removeAll();
      }
      refreshNeeded = true;
   } );

   /**
    * takes a factor object, *not a record*
    * 
    * @param {Object}
    *           factor
    */
   experimentalFactorGrid.on( "experimentalfactorselected", function( factor ) {
      if ( (factor.type && factor.type == "continuous") ) {
         factorValueGrid.getStore().removeAll();
         factorValueGrid.setTitle( "Continuous values not displayed here, see the 'sample details' tab" );
         factorValueGrid.getEl().mask( "Continuous values not displayed here, see the 'sample details' tab" );

      } else {
         factorValueGrid.getEl().unmask();
         factorValueGrid.setTitle( "Factor values for : " + factor.name );
         factorValueGrid.setExperimentalFactor( factor.id );

      }
   } );

   factorValueGrid.on( "factorvaluecreate", function( fvgrid, fvs ) {
      refreshNeeded = true;
   } );

   factorValueGrid.on( "factorvaluechange", function( fvgrid, fvs ) {
      refreshNeeded = true;
   } );

   factorValueGrid.on( "factorvaluedelete", function( fvgrid, fvs ) {
      refreshNeeded = true;
   } );

} );