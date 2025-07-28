Ext.BLANK_IMAGE_URL = Gemma.CONTEXT_PATH + '/images/default/s.gif';
Ext.namespace( 'Gemma.CharacteristicBrowser' );
Ext.onReady( function() {
   Ext.QuickTips.init();

   var browsergrid = new Gemma.AnnotationGrid( {

      viewConfig : {
         forceFit : true
      },

      readMethod : CharacteristicBrowserController.findCharacteristicsCustom,
      renderTo : "characteristicBrowser",
      readParams : [],
      editable : true,
      tbar : new Ext.Toolbar( [] ),
      useDefaultToolbar : false,
      showParent : true,

      width : 1200,
      height : 500,
      noInitialLoad : true
   } );

   Gemma.CharacteristicBrowser.handleError = function( msg, e ) {
      Ext.DomHelper.overwrite( "messages", {
         tag : 'img',
         src : Gemma.CONTEXT_PATH + '/images/icons/warning.png'
      } );
      Ext.DomHelper.append( "messages", {
         tag : 'span',
         html : "&nbsp;&nbsp;" + msg
      } );
      browsergrid.loadMask.hide();
      saveButton.enable();
   };

   var queryField = new Ext.form.TextField( {
      width : 240
   } );

   var doQuery = function() {
      Ext.DomHelper.overwrite( "messages", "" );
      var query = queryField.getValue();
      if ( !query ) {
         Ext.DomHelper.overwrite( "messages", "Please enter a query" );
         return;
      }
      browsergrid.loadMask.msg = "Updating ...";
      var searchEEs = eeCheckBox.getValue();
      var searchBMs = bmCheckBox.getValue();
      var searchFVs = fvCheckBox.getValue();
      var searchNos = noCheckBox.getValue();
      var searchCats = catsCheckBox.getValue();
      var searchFactorsValueValues = false; // fvvCheckBox.getValue();
      var categoryConstraint = categoryCombo.getValue();
      browsergrid.refresh( [ query, searchNos, searchEEs, searchBMs, searchFVs, searchFactorsValueValues,
         searchCats, constrainToCategoryCheck.getValue() ? categoryConstraint : '' ] );
   };

   var searchButton = new Ext.Toolbar.Button( {
      text : "search",
      tooltip : "Find matching characteristics in the database",
      handler : doQuery
   } );

   var saveButton = new Ext.Toolbar.Button( {
      text : "save",
      tooltip : "Saves your changes to the database",
      disabled : true,
      handler : function( but ) {
         but.disable();
         browsergrid.loadMask.msg = "Saving ...";
         browsergrid.loadMask.show();
         var chars = browsergrid.getEditedCharacteristics();

         var callback = browsergrid.refresh.createDelegate( browsergrid );
         var errorHandler = Gemma.CharacteristicBrowser.handleError.createDelegate( this );

         CharacteristicBrowserController.updateCharacteristics( chars, {
            callback : callback,
            errorHandler : errorHandler
         } );
      }
   } );

   browsergrid.on( "afteredit", function( e ) {
      saveButton.enable();
   } );

   var deleteButton = new Ext.Toolbar.Button( {
      text : "delete",
      tooltip : "Delete selected characteristics",
      disabled : true,
      handler : function() {
         Ext.DomHelper.overwrite( "messages", "" );
         browsergrid.loadMask.msg = "Deleting ...";
         browsergrid.loadMask.show();
         var chars = browsergrid.getSelectedCharacteristics();

         CharacteristicBrowserController.removeCharacteristics( chars, function() {

            /*
             * remove the records from the data store manually instead of just refreshing so that we don't lose any
             * edits that are in progress...
             */
            var selected = browsergrid.getSelectionModel().getSelections();
            for ( var i = 0; i < selected.length; ++i ) {
               browsergrid.getStore().remove( selected[i] );
            }
            browsergrid.getView().refresh();
            browsergrid.loadMask.hide();
         } );
      }
   } );
   browsergrid.getSelectionModel().on( "selectionchange", function( model ) {
      var selected = model.getSelections();
      Ext.DomHelper.overwrite( "messages", "" );
      if ( selected.length > 0 ) {
         deleteButton.enable();
      } else {
         deleteButton.disable();
      }
   } );

   var revertButton = new Ext.Toolbar.Button( {
      text : "revert",
      tooltip : "Undo changes to selected characteristics",
      disabled : true,
      handler : function() {
         var selected = browsergrid.getSelectionModel().getSelections();
         for ( var i = 0; i < selected.length; ++i ) {
            var record = selected[i];
            record.reject();
         }
         browsergrid.getView().refresh();
      }
   } );
   browsergrid.getSelectionModel().on( "selectionchange", function( model ) {
      var selected = model.getSelections();
      revertButton.disable();
      for ( var i = 0; i < selected.length; ++i ) {
         if ( selected[i].dirty ) {
            revertButton.enable();
            break;
         }
      }
   } );
   browsergrid.on( "afteredit", function( e ) {
      revertButton.enable();
   } );

   var savedCharacteristic = null;
   var copyHandler = function() {
      var selected = browsergrid.getSelectionModel().getSelections();
      for ( var i = 0; i < selected.length; ++i ) {
         var record = selected[i];
         savedCharacteristic = record.data;
         break;
      }
      pasteButton.enable();
      pasteCategoryButton.enable();
      pasteValueButton.enable();
   };

   var copyButton = new Ext.Toolbar.Button( {
      text : "copy",
      tooltip : "Copy values from the selected characteristic",
      disabled : true,
      handler : copyHandler
   } );

   browsergrid.getSelectionModel().on( "selectionchange", function( model ) {
      var selected = model.getSelections();
      if ( selected.length > 0 ) {
         copyButton.enable();
      } else {
         copyButton.disable();
      }
   } );

   var pasteHandler = function() {
      var selected = browsergrid.getSelectionModel().getSelections();
      for ( var i = 0; i < selected.length; ++i ) {
         var record = selected[i];
         record.set( "classUri", savedCharacteristic.classUri );
         record.set( "className", savedCharacteristic.className );
         record.set( "termUri", savedCharacteristic.termUri );
         record.set( "termName", savedCharacteristic.termName );
      }
      browsergrid.getView().refresh();
      saveButton.enable();
   };

   var pasteCategoryHandler = function() {
      var selected = browsergrid.getSelectionModel().getSelections();
      for ( var i = 0; i < selected.length; ++i ) {
         var record = selected[i];
         record.set( "classUri", savedCharacteristic.classUri );
         record.set( "className", savedCharacteristic.className );
      }
      browsergrid.getView().refresh();
      saveButton.enable();
   };

   var pasteValueHandler = function() {
      var selected = browsergrid.getSelectionModel().getSelections();
      for ( var i = 0; i < selected.length; ++i ) {
         var record = selected[i];
         record.set( "termUri", savedCharacteristic.termUri );
         record.set( "termName", savedCharacteristic.termName );
      }
      browsergrid.getView().refresh();
      saveButton.enable();
   };

   var pasteButton = new Ext.Toolbar.Button( {
      text : "paste",
      tooltip : "Paste copied values onto the selected characteristics; both Class and Term will be updated.",
      disabled : true,
      handler : pasteHandler
   } );

   var pasteCategoryButton = new Ext.Toolbar.Button( {
      text : "paste category",
      tooltip : "Paste copied Category onto the selected characteristics. Term will be left alone.",
      disabled : true,
      handler : pasteCategoryHandler
   } );

   var pasteValueButton = new Ext.Toolbar.Button( {
      text : "paste value",
      tooltip : "Paste copied value values onto the selected characteristics. Category will be left alone.",
      disabled : true,
      handler : pasteValueHandler
   } );

   var constrainToCategoryCheck = new Ext.form.Checkbox( {
      boxLabel : "Constrain to selected category",
      name : "constrainToCategoryCheck",
      width : 'auto',
      disabled : false
   });

   var categoryCombo = new Gemma.CategoryCombo( {} );

   browsergrid.on( "keypress", function( e ) {
      if ( e.ctrlKey ) {
         if ( e.getCharCode() == 99 ) { // 'c'
            copyHandler();
         } else if ( e.getCharCode() == 118 ) { // 'v'
            pasteHandler();
         }
      }
   } );

   var toolbar = browsergrid.getTopToolbar();

   toolbar.addField( queryField );
   toolbar.addSpacer();
   toolbar.addField( searchButton );
   toolbar.addSeparator();
   toolbar.addField( saveButton );
   toolbar.addSeparator();
   toolbar.addField( deleteButton );
   toolbar.addSeparator();
   toolbar.addField( revertButton );
   toolbar.addSeparator();
   toolbar.addField( copyButton );
   toolbar.addSeparator();
   toolbar.addField( pasteButton );
   toolbar.addSeparator();
   toolbar.addField( pasteCategoryButton );
   toolbar.addSeparator();
   toolbar.addField( pasteValueButton );
   toolbar.addSeparator();
   toolbar.addField( categoryCombo );
   toolbar.addSeparator();
   toolbar.addField(constrainToCategoryCheck);
   toolbar.addFill();

   /*
    * Second toolbar.
    */

   var eeCheckBox = new Ext.form.Checkbox( {
      boxLabel : 'Datasets',
      checked : true,
      name : 'searchEEs',
      width : 'auto'
   } );
   var bmCheckBox = new Ext.form.Checkbox( {
      boxLabel : 'Samples',
      checked : true,
      name : 'searchBMs',
      width : 'auto'
   } );
   var fvCheckBox = new Ext.form.Checkbox( {
      boxLabel : 'Factors',
      checked : true,
      name : 'searchFVs',
      width : 'auto'
   } );

   // var paCheckBox = new Ext.form.Checkbox( {
   //    boxLabel : 'Phenotype Associations',
   //    checked : true,
   //    name : 'searchPAs',
   //    width : 'auto'
   // } );

   // var fvvCheckBox = new Ext.form.Checkbox( {
   //    boxLabel : 'Factors (via their values)',
   //    checked : true,
   //    name : 'searchFVVs',
   //    width : 'auto'
   // } );

   var noCheckBox = new Ext.form.Checkbox( {
      boxLabel : 'No parent', // careful, these might just be hidden due to
      // security.
      checked : true,
      name : 'searchNos',
      width : 'auto'
   } );

   var catsCheckBox = new Ext.form.Checkbox( {
      boxLabel : 'Categories',
      checked : false,
      name : 'searchCats',
      width : 'auto'
   } );

   var secondToolbar = new Ext.Toolbar( {
      renderTo : browsergrid.tbar
   } );
   secondToolbar.addSpacer();
   secondToolbar.addText( "Find characteristics from:" );
   secondToolbar.addSpacer();
   secondToolbar.addField( eeCheckBox );
   secondToolbar.addSpacer();
   secondToolbar.addField( bmCheckBox );
   secondToolbar.addSpacer();
   secondToolbar.addField( fvCheckBox );
   secondToolbar.addSpacer();
   secondToolbar.addField( noCheckBox );
   // secondToolbar.addSpacer();
   // secondToolbar.addField( paCheckBox );
   // secondToolbar.addSpacer();
   // secondToolbar.addField( fvvCheckBox );
   secondToolbar.addSpacer();
   secondToolbar.addField( catsCheckBox );
   secondToolbar.doLayout();
   browsergrid.doLayout();

   queryField.el.on( "keyup", function( e ) {
      if ( e.getCharCode() == Ext.EventObject.ENTER ) {
         doQuery();
      }
   } );

} );