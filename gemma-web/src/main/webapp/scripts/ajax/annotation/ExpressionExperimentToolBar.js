Ext.onReady( function() {

	var eeid = dwr.util.getValue("eeId");

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

	/* the save button calls the refreshEEAnnotations function defined
	 * by eeAnnotations.js
	 */
	var saveButton = new Ext.Toolbar.Button( {
		text : "save",
		tooltip : "Adds the new annotation",
		disabled : true,
		handler : function() {
			OntologyService.saveExpressionExperimentStatement(
				charCombo.getCharacteristic(), [eeid], refreshEEAnnotations );
			mgedCombo.reset();
			charCombo.reset();
			saveButton.disable();
		}
	} );
	
	/* the delete button reads from the grid defined by eeAnnotations.js
	 */
	var deleteButton = new Ext.Toolbar.Button( {
		text : "delete",
		tooltip : "Removes the selected annotation",
		disabled : true,
		handler : function() {
			var selected = eeGrid.getSelectionModel().getSelections();
			var ids = [];
			for ( var i=0; i<selected.length; ++i ) {
				ids.push( selected[i].id );
			}
			OntologyService.removeExpressionExperimentStatement(
				ids, [eeid], refreshEEAnnotations );
			deleteButton.disable();
		}
	} );
	eeGrid.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		if ( selected.length > 0 )
			deleteButton.enable();
		else
			deleteButton.disable();
	} );
	
	
	var toolbar = new Ext.Toolbar("eeAnnotator");
	toolbar.addField( mgedCombo );
	toolbar.addSpacer();
	toolbar.addField( charCombo );
	toolbar.addSpacer();
	toolbar.addField( saveButton );
	toolbar.addSeparator();
	toolbar.addField( deleteButton );

} );