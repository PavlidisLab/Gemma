Ext.namespace('Ext.Gemma');

/* Ext.Gemma.AnnotationToolBar constructor...
 * 	annotationGrid is the grid that contains the annotations.
 * 	config is a hash with the following options:
 * 		createHandler : a function with arguments ( characteristic, callback )
 * 			where characteristic is the new characteristic to add
 * 			  and callback is the function to be called when the characteristic has been added
 * 			if this argument is not present, there will be no create button in the toolbar
 * 		deleteHandler : a function with arguments ( ids, callback )
 * 			where ids is an array of characteristic ids to remove
 * 			  and callback is the function to be called when the characteristics have been removed
 * 			if this argument is not present, there will be no delete button in the toolbar
 * 		saveHandler : a function with arguments ( characteristics, callback )
 * 			where characteristics is an array of characteristics to update
 * 			  and callback is the function to be called when the characteristics have been updated
 * 			if this argument is not present, there will be no save button in the toolbar
 */
 
Ext.Gemma.AnnotationToolBar = function ( annotationGrid, config ) {
	
	var createHandler = config.createHandler; delete config.createHandler;
	var deleteHandler = config.deleteHandler; delete config.deleteHandler;
	var saveHandler = config.saveHandler; delete config.saveHandler;
	if ( annotationGrid.editable && !saveHandler ) {
		saveHandler = CharacteristicBrowserController.updateCharacteristics;
	}

	var charComboOpts = { };
	if ( config.charComboWidth ) {
		charComboOpts.width = config.charComboWidth; delete config.charComboWidth;
	}
	var mgedComboOpts = {
		emptyText : "Select a class"
	};
	if ( config.mgedComboWidth ) {
		mgedComboOpts.width = config.mgedComboWidth; delete config.mgedComboWidth;
	}
	if ( config.mgedTermKey ) {
		mgedComboOpts.termKey = config.mgedTermKey; delete config.mgedTermKey;
	}
	
	/* according to the docs, we shouldn't have to call this method, but
	 * if we don't, we're left with the empty placeholder toolbar above
	 * our new toolbar...
	 */
	Ext.Gemma.AnnotationToolBar.superclass.constructor.call( this, {
		autoHeight : true,
		renderTo : annotationGrid.tbar
	} );
	
	var charCombo = new Ext.Gemma.CharacteristicCombo( charComboOpts );
	
	var mgedCombo = new Ext.Gemma.MGEDCombo( mgedComboOpts );
	mgedCombo.on( "select", function ( combo, record, index ) {
		charCombo.setCategory( record.data.term, record.data.uri );
		createButton.enable();
	} );
	
	var descriptionField = new Ext.form.TextField( {
		allowBlank : true,
		invalidText : "Enter a description",
		blankText : "Add a simple description",
		emptyText : "Description",
		width : 75
	} );
	
	if ( createHandler ) {
		var createButton = new Ext.Toolbar.Button( {
			text : "create",
			tooltip : "Adds the new annotation",
			disabled : true,
			handler : function() {
				var characteristic = charCombo.getCharacteristic();
				if ( config.addDescription ) {
					characteristic.description = descriptionField.getValue();
				}
				createHandler( characteristic, annotationGrid.refresh.bind( annotationGrid ) );
				mgedCombo.reset();
				charCombo.reset();
				descriptionField.reset();			
				createButton.disable();
			}
		} );
	}
	
	if ( deleteHandler ) {
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
	}
	
	if ( saveHandler ) {
		var saveButton = new Ext.Toolbar.Button( {
			text : "save",
			tooltip : "Saves the updated annotations",
			disabled : true,
			handler : function() {
				saveHandler( annotationGrid.getEditedCharacteristics(), annotationGrid.refresh.bind( annotationGrid ) );
				saveButton.disable();
	}
		} );
		annotationGrid.on( "afteredit", function( model ) {
			saveButton.enable();
		} );
	}
	
	this.addField( mgedCombo );
	this.addSpacer();
	this.addField( charCombo );
	this.addSpacer();
	
	if ( config.addDescription ) {
		this.addField( descriptionField );
		this.addSpacer();
	}
	
	if ( createHandler ) { this.addField( createButton ); }
	if ( createHandler && ( deleteHandler || saveHandler ) ) { this.addSeparator(); }
	if ( deleteHandler ) { this.addField( deleteButton ); }
	if ( ( createHandler || deleteHandler ) && saveHandler ) { this.addSeparator(); }
	if ( saveHandler ) { this.addField( saveButton ); }
	
};

/* other public methods...
 */
Ext.extend( Ext.Gemma.AnnotationToolBar, Ext.Toolbar, {
	
} );