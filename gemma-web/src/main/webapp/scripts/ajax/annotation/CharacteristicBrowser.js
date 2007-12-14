Ext.namespace('Ext.Gemma.CharacteristicBrowser');

Ext.onReady( function() {
		
	Ext.Gemma.CharacteristicBrowser.grid = new Ext.Gemma.AnnotationGrid( "characteristicBrowser", {
		readMethod : CharacteristicBrowserController.findCharacteristics,
		readParams : [ ],
		editable : true,
		showParent : true,
		noInitialLoad : true,
		pageSize : 10
	} );
	Ext.Gemma.CharacteristicBrowser.grid.render();
	var footer = Ext.Gemma.CharacteristicBrowser.grid.getView().getFooterPanel( true );
	var paging = new Ext.Gemma.PagingToolbar( footer, Ext.Gemma.CharacteristicBrowser.grid.getDataSource(), {
		pageSize : 10
	} );
	
	var charCombo = new Ext.Gemma.CharacteristicCombo( { } );
	
	var searchButton = new Ext.Toolbar.Button( {
		text : "search",
		tooltip : "Find matching characteristics in the database",
		handler : function() {
			var query = charCombo.getCharacteristic().value;
			var searchEEs = eeCheckBox.getValue();
			var searchBMs = bmCheckBox.getValue();
			var searchFVs = fvCheckBox.getValue();
			var searchNos = noCheckBox.getValue();
			Ext.Gemma.CharacteristicBrowser.grid.refresh( [ query, searchNos, searchEEs, searchBMs, searchFVs ] );
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
			CharacteristicBrowserController.removeCharacteristics( chars );
			
			/* remove the records from the data store manually instead of just refreshing so that we don't
			 * lose any edits that are in progress...
			 */
			var selected = Ext.Gemma.CharacteristicBrowser.grid.getSelectionModel().getSelections();
			for ( var i=0; i<selected.length; ++i ) {
				Ext.Gemma.CharacteristicBrowser.grid.getDataSource().remove( selected[i] );
			}
			Ext.Gemma.CharacteristicBrowser.grid.getView().refresh();
			
			//var callback = Ext.Gemma.CharacteristicBrowser.grid.refresh.bind( Ext.Gemma.CharacteristicBrowser.grid );
			//CharacteristicBrowserController.removeCharacteristics( chars, callback );
		}
	} );
	Ext.Gemma.CharacteristicBrowser.grid.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		if ( selected.length > 0 )
			deleteButton.enable();
		else
			deleteButton.disable();
	} );
	
	var eeCheckBox = new Ext.form.Checkbox( {
		boxLabel : 'Expression Experiments',
		checked : true,
		name : 'searchEEs',
		width : 'auto'
	} );
	var bmCheckBox = new Ext.form.Checkbox( {
		boxLabel : 'BioMaterials',
		checked : true,
		name : 'searchBMs',
		width : 'auto'
	} );
	var fvCheckBox = new Ext.form.Checkbox( {
		boxLabel : 'Factor Values',
		checked : true,
		name : 'searchFVs',
		width : 'auto'
	} );
	var noCheckBox = new Ext.form.Checkbox( {
		boxLabel : 'No parent',
		checked : true,
		name : 'searchNos',
		width : 'auto'
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
	toolbar.addSeparator();
	toolbar.addText( "Find characteristics from" );
	toolbar.addSpacer();
	toolbar.addField( eeCheckBox );
	toolbar.addSpacer();
	toolbar.addField( bmCheckBox );
	toolbar.addSpacer();
	toolbar.addField( fvCheckBox );
	toolbar.addSpacer();
	toolbar.addField( noCheckBox );
	
} );