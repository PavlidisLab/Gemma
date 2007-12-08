Ext.namespace('Ext.Gemma.CharacteristicBrowser');

Ext.onReady( function() {
		
	Ext.Gemma.CharacteristicBrowser.grid = new Ext.Gemma.AnnotationGrid( "characteristicBrowser", {
		readMethod : CharacteristicBrowserController.findCharacteristics,
		readParams : [ ],
		editable : true,
		showParent : true,
		noInitialLoad : true
	} );
	Ext.Gemma.CharacteristicBrowser.grid.render();
	
	var charCombo = new Ext.Gemma.CharacteristicCombo( { } );
	
	var searchButton = new Ext.Toolbar.Button( {
		text : "search",
		tooltip : "Find matching characteristics in the database",
		handler : function() {
			var value = charCombo.getCharacteristic().value;
			Ext.Gemma.CharacteristicBrowser.grid.refresh( [ value ] );
		}
	} );
	Ext.Gemma.CharacteristicBrowser.grid.on( "afteredit", function( e ) {
		saveButton.enable();
		Ext.Gemma.CharacteristicBrowser.grid.getView().refresh();
	} );

	var saveButton = new Ext.Toolbar.Button( {
		text : "save",
		tooltip : "Saves your changes to the database",
		disabled : true,
		handler : function() {
			saveButton.disable();
			var chars = Ext.Gemma.CharacteristicBrowser.grid.getEditedCharacteristics();
			var callback = Ext.Gemma.CharacteristicBrowser.grid.refresh.bind( Ext.Gemma.CharacteristicBrowser.grid );
			CharacteristicBrowserController.updateCharacteristics( chars, callback );
		}
	} );
	
	var deleteButton = new Ext.Toolbar.Button( {
		text : "delete",
		tooltip : "Delete selected characteristics",
		disabled : true,
		handler : function() {
			var chars = Ext.Gemma.CharacteristicBrowser.grid.getSelectedCharacteristics();
			var callback = Ext.Gemma.CharacteristicBrowser.grid.refresh.bind( Ext.Gemma.CharacteristicBrowser.grid );
			CharacteristicBrowserController.removeCharacteristics( chars , callback );
		}
	} );
	Ext.Gemma.CharacteristicBrowser.grid.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		if ( selected.length > 0 )
			deleteButton.enable();
		else
			deleteButton.disable();
	} );
	
	var gridHeader = Ext.Gemma.CharacteristicBrowser.grid.getView().getHeaderPanel( true );
	var toolbar = new Ext.Toolbar( gridHeader );
	toolbar.addField( charCombo );
	toolbar.addSpacer();
	toolbar.addField( searchButton );
	toolbar.addSeparator();
	toolbar.addField( saveButton );
	toolbar.addSeparator();
	toolbar.addField( deleteButton );
	
} );