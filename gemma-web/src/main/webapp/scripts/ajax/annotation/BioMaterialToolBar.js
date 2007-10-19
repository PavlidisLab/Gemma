Ext.onReady( function() {

	var bmid = dwr.util.getValue("bmId");

	var mgedCombo = new Ext.Gemma.MGEDCombo( {
		emptyText : "Select a class",
		selectOnFocus : true
	} );
	mgedCombo.on( "select", function ( combo, record, index ) {
		charCombo.setCategory( record.data.term, record.data.uri );
		saveButton.enable();
	} );

	var charCombo = new Ext.Gemma.CharacteristicCombo( {
	} );

	/* the save button calls the refreshAnnotations function defined
	 * by bmAnnotations.js
	 */
	var saveButton = new Ext.Toolbar.Button( {
		text : "save",
		tooltip : "Adds the new annotation",
		disabled : true,
		handler : function() {
			OntologyService.saveBioMaterialStatement(
				charCombo.getCharacteristic(), [bmid], refreshAnnotations );
			mgedCombo.reset();
			charCombo.reset();
			saveButton.disable();
		}
	} );
	
	/* the delete button reads from the grid defined by bmAnnotations.js
	 */
	var deleteButton = new Ext.Toolbar.Button( {
		text : "delete",
		tooltip : "Removes the selected annotation",
		disabled : true,
		handler : function() {
			var selected = annotationsGrid.getSelectionModel().getSelections();
			var ids = [];
			for ( var i=0; i<selected.length; ++i ) {
				ids.push( selected[i].id );
			}
			OntologyService.removeBioMaterialStatement(
				ids, [bmid], refreshAnnotations );
			deleteButton.disable();
		}
	} );
	annotationsGrid.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		if ( selected.length > 0 )
			deleteButton.enable();
		else
			deleteButton.disable();
	} );
	
	
	var toolbar = new Ext.Toolbar("bmAnnotator");
	toolbar.addField( mgedCombo );
	toolbar.addSpacer();
	toolbar.addField( charCombo );
	toolbar.addSpacer();
	toolbar.addField( saveButton );
	toolbar.addSeparator();
	toolbar.addField( deleteButton );

} );