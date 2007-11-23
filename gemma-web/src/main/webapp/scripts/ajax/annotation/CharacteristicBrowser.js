Ext.namespace('Ext.Gemma.CharacteristicBrowser');

Ext.onReady( function() {

	var edCharCombo = new Ext.Gemma.CharacteristicCombo( { lazyRender : true } );
	var valueEditor = new Ext.grid.GridEditor( edCharCombo );
	edCharCombo.on( "select", function ( combo, record, index ) { valueEditor.completeEdit(); } );
	var edMgedCombo = new Ext.Gemma.MGEDCombo( { lazyRender : true } );
	var categoryEditor = new Ext.grid.GridEditor( edMgedCombo );
	edMgedCombo.on( "select", function ( combo, record, index ) { categoryEditor.completeEdit(); } );
	var editedIds = {};
	
	Ext.Gemma.CharacteristicBrowser.grid = new Ext.Gemma.AnnotationGrid( "characteristicBrowser", {
		readMethod : CharacteristicBrowserController.findCharacteristics,
		readParams : [ "xyzzy" ],
		cm : new Ext.grid.ColumnModel( [
			{ header: "Category", id: "category", dataIndex: "className", editor: categoryEditor },
			{ header: "Value", id: "value", dataIndex: "termName", renderer: Ext.Gemma.AnnotationGrid.getStyler(), editor: valueEditor },
			{ header: "Parent", dataIndex: "parentLink" }
		] ),
		autoExpandColumn : 2
	} );
	Ext.Gemma.CharacteristicBrowser.grid.render();
	Ext.Gemma.CharacteristicBrowser.grid.getDataSource().on( "load", function() {
		Ext.Gemma.CharacteristicBrowser.grid.getView().autoSizeColumns();
	} );
	Ext.Gemma.CharacteristicBrowser.grid.on( "beforeedit", function( e ) {
		var row = e.record.data;
		var col = Ext.Gemma.CharacteristicBrowser.grid.getColumnModel().getColumnId( e.column );
		if ( col == "value" ) {
			var f = edCharCombo.setCategory.bind( edCharCombo );
			f( row.className, row.classUri );
		}
	} );
	Ext.Gemma.CharacteristicBrowser.grid.on( "afteredit", function( e ) {
		var row = e.record.data;
		var col = Ext.Gemma.CharacteristicBrowser.grid.getColumnModel().getColumnId( e.column );
		if ( col == "category" ) {
			var f = edMgedCombo.getTerm.bind( edMgedCombo );
			var term = f();
			row.className = term.term;
			row.classUri = term.uri;
		} else if ( col == "value" ) {
			var f = edCharCombo.getCharacteristic.bind( edCharCombo );
			var c = f();
			row.termName = c.value;
			row.termUri = c.valueUri;
		}
		editedIds[ row.id ] = true;
		saveButton.enable();
		Ext.Gemma.CharacteristicBrowser.grid.getView().refresh();
	} );
	
	var charCombo = new Ext.Gemma.CharacteristicCombo( { } );
	
	var searchButton = new Ext.Toolbar.Button( {
		text : "search",
		tooltip : "Find matching characteristics in the database",
		handler : function() {
			Ext.Gemma.CharacteristicBrowser.grid.refresh( [ charCombo.getCharacteristic().value ] );
		}
	} );
	
	var saveButton = new Ext.Toolbar.Button( {
		text : "save",
		tooltip : "Saves your changes to the database",
		disabled : true,
		handler : function() {
			var chars = [];
			Ext.Gemma.CharacteristicBrowser.grid.getDataSource().each( function( record ) {
				var row = record.data;
				if ( editedIds[ row.id ] ) {
					chars.push( Ext.Gemma.AnnotationGrid.convertToCharacteristic( row ) );
				}
			} );
			editedIds = {};
			saveButton.disable();
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
	
	var gridPanel = Ext.Gemma.CharacteristicBrowser.grid.getView().getHeaderPanel( true );
	var toolbar = new Ext.Toolbar( gridPanel );
	toolbar.addField( charCombo );
	toolbar.addSpacer();
	toolbar.addField( searchButton );
	toolbar.addSeparator();
	toolbar.addField( saveButton );
	toolbar.addSeparator();
	toolbar.addField( deleteButton );
} );