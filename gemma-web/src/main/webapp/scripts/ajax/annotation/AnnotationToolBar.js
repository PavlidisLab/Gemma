Ext.namespace('Ext.Gemma');

/* Ext.Gemma.AnnotationToolBar constructor...
 * 	div is the name of the div in which to render the tool bar.
 * 	annotationGrid is the grid that contains the annotations.
 * 	saveHandler is a function with arguments ( characteristic, callback )
 * 		where characteristic is the new characteristic to add
 * 		  and callback is the function to be called when the characteristic has been added
 * 	deleteHandler is a function with arguments ( ids, callback )
 * 		where ids is an array of characteristic ids to remove
 * 		  and callback is the function to be called when the characteristics have been removed
 * 
 *  addDescription if defined will create a description field that the user must fill in
 */
 
Ext.Gemma.AnnotationToolBar = function ( div, annotationGrid, saveHandler, deleteHandler, addDescription, opts ) {

	Ext.Gemma.AnnotationToolBar.superclass.constructor.call( this, div );
	
	var charComboOpts = { };
	if ( opts && opts.charComboWidth ) {
		charComboOpts.width = opts.charComboWidth
	}
	var charCombo = new Ext.Gemma.CharacteristicCombo( charComboOpts );
	
	var mgedComboOpts = {
		emptyText : "Select a class",
		selectOnFocus : true
	};
	if ( opts && opts.mgedComboWidth ) {
		mgedComboOpts.width = opts.mgedComboWidth;
	}
	var mgedCombo = new Ext.Gemma.MGEDCombo( mgedComboOpts );
	mgedCombo.on( "select", function ( combo, record, index ) {
		charCombo.setCategory( record.data.term, record.data.uri );
		saveButton.enable();
	} );
	
	
	var descriptionField = new Ext.form.TextField( {
		allowBlank : true,
		invalidText : "Enter a description",
		blankText : "Add a simple description",
		emptyText : "Description",
		width : 75
	} );
	
	
	var saveButton = new Ext.Toolbar.Button( {
		text : "save",
		tooltip : "Adds the new annotation",
		disabled : true,
		handler : function() {
			var characteristic = charCombo.getCharacteristic();
			
				if (addDescription) {		
//Don't want description to be mandatory anymore							
//					var description = descriptionField.getValue();
//					if ((description === undefined) || (description.length === 0) || (description === "Description")){
//						alert("Please add a description");
//						return;
//					}
					characteristic.description = descriptionField.getValue();
				}			
			
			saveHandler( characteristic, annotationGrid.refresh.bind( annotationGrid ) );
			mgedCombo.reset();
			charCombo.reset();
			descriptionField.reset();			
			saveButton.disable();
		}
	} );
	
	var deleteButton = new Ext.Toolbar.Button( {
		text : "delete",
		tooltip : "Removes the selected annotation",
		disabled : true,
		handler : function() {
			deleteHandler( annotationGrid.getSelectedIds(), annotationGrid.refresh.bind( annotationGrid ) );
			deleteButton.disable();
		}
	} );
	annotationGrid.getSelectionModel().on( "selectionchange", function( model ) {
		var selected = model.getSelections();
		if ( selected.length > 0 )
			deleteButton.enable();
		else
			deleteButton.disable();
	} );
	
	this.addField( mgedCombo );
	this.addSpacer();
	this.addField( charCombo );
	this.addSpacer();
	
	if (addDescription) {
		this.addField( descriptionField );
		this.addSpacer();
	}
	
	if ( saveHandler ) { this.addField( saveButton ); }
	if ( saveHandler && deleteHandler ) { this.addSeparator(); }
	if ( deleteHandler ) { this.addField( deleteButton ); }
	
}

/* other public methods...
 */
Ext.extend( Ext.Gemma.AnnotationToolBar, Ext.Toolbar, {
	
} );