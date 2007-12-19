Ext.namespace('Ext.Gemma.CharacteristicBrowser');

Ext.onReady( function() {
		
	Ext.Gemma.CharacteristicBrowser.grid = new Ext.Gemma.AnnotationGrid( "characteristicBrowser", {
		tbar : new Ext.Toolbar( { } ),
		readMethod : CharacteristicBrowserController.findCharacteristics,
		readParams : [ ],
		editable : true,
		showParent : true,
		noInitialLoad : true,
		pageSize : 10
	} );
	Ext.Gemma.CharacteristicBrowser.grid.render();
	
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
	Ext.Gemma.CharacteristicBrowser.grid.on( "afteredit", function( e ) {
		saveButton.enable();
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
	
	var revertButton = new Ext.Toolbar.Button( {
		text : "revert",
		tooltip : "Undo changes to selected characteristics",
		disabled : true,
		handler : function() {
			var selected = Ext.Gemma.CharacteristicBrowser.grid.getSelectionModel().getSelections();
			for ( var i=0; i<selected.length; ++i ) {
				var record = selected[i]
				//record.reject();
			}
			Ext.Gemma.CharacteristicBrowser.grid.getView().refresh();
		}
	} );
	Ext.Gemma.CharacteristicBrowser.grid.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		revertButton.disable();
		for ( var i=0; i<selected.length; ++i ) {
			if ( selected[i].dirty ) {
				revertButton.enable();
				break;
			}
		}
	} );
	Ext.Gemma.CharacteristicBrowser.grid.on( "afteredit", function( e ) {
		revertButton.enable();
	} );

/*
	var testButton = new Ext.Toolbar.Button( {
		text : "test",
		handler : function() {
			var selected = Ext.Gemma.CharacteristicBrowser.grid.getSelectionModel().getSelections();
			for ( var i=0; i<selected.length; ++i ) {
				var record = selected[i]
				record.data.parentLink = record.data.parentLink.concat( String.format( "<div style='white-space: normal; margin-left: 1em;'>{0}</div>", record.data.parentDescription ) );
			}
			Ext.Gemma.CharacteristicBrowser.grid.getView().refresh();
		}
	} );
*/

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
	
	var toolbar = Ext.Gemma.CharacteristicBrowser.grid.getTopToolbar();
	toolbar.addField( charCombo );
	toolbar.addSpacer();
	toolbar.addField( searchButton );
	toolbar.addSeparator();
	toolbar.addField( saveButton );
	toolbar.addSeparator();
	toolbar.addField( deleteButton );
	toolbar.addSeparator();
	toolbar.addField( revertButton );
/*
	toolbar.addSeparator();
	toolbar.addField( testButton );
*/

	var secondToolbar = new Ext.Toolbar( toolbar.getEl().createChild() );
	secondToolbar.addSpacer();
	secondToolbar.addText( "Find characteristics from" );
	secondToolbar.addSpacer();
	secondToolbar.addField( eeCheckBox );
	secondToolbar.addSpacer();
	secondToolbar.addField( bmCheckBox );
	secondToolbar.addSpacer();
	secondToolbar.addField( fvCheckBox );
	secondToolbar.addSpacer();
	secondToolbar.addField( noCheckBox );
	
} );