/**
 * This literature panel contains literature information. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.LiteraturePanel = Ext.extend(Ext.Panel, {
	border: false,
	layout: 'form',
	hidden: true,
    autoHeight:true,
	anchor: '96%', // Use anchor instead of width so that this panel can be resized as the main window is resized.
    defaultType: 'textfield',
    initComponent: function() {
		var pubMedIdField = new Ext.form.NumberField({
			// It is a validator that can decide if this field is valid.
			pudMedIdValidator: (this.pudMedIdValidator ?
									this.pudMedIdValidator :
									function() { return true; }),			
			name: "pubmedId",
			fieldLabel: 'PubMed Id',
			minValue: 1,			
			minLength: 1,
			maxLength: 9,
			allowBlank: false,
			allowDecimals: false,
			allowNegative: false,
			width: 100,
			autoCreate: {tag: 'input', type: 'text', size: '20', autocomplete: 'off', maxlength: '9'},
			enableKeyEvents: true,
		    initComponent: function() {
				// This is set to true initially because we will see it having invalid red border 
				// very briefly and then the border disappears after its initial value is set.		    	
				var keyPressed = true;
		    	
				Ext.apply(this, {
					validator: function() {
						// When a user press a key, the validator should return true. Otherwise, the
						// user cannot press the OK button to submit the form containing this field.  
						return keyPressed || this.pudMedIdValidator(); 
					},
					listeners: {
						keypress: function(numberField, event) {
							keyPressed = true;
						},
						blur: function(numberField) {
							keyPressed = false;
							updateBibliographicReferenceDetailsPanel();
						}
					}
				});

				this.superclass().initComponent.call(this);
	    	}
		});
		this.relayEvents(pubMedIdField, ['blur', 'keypress']);
		
		var statusDisplayField = new Ext.form.DisplayField({
			value: 'Searching for publication ...',
			style: 'color: grey; font-style: italic;',
			margins: '4 0 0 0'			
		});
		
		var bibliographicReferenceDetailsPanel = new Gemma.BibliographicReference.DetailsPanel({
			hidden: true,			
			border: false,
			header: false,
			padding: '0 0 10px 10px',			
			collapseByDefault: true       		
		});

		// loadMask needs to be set up on render because this.getEl() has not been defined yet before render.     	
		bibliographicReferenceDetailsPanel.on('render', function() {
			if (!bibliographicReferenceDetailsPanel.loadMask) {
				bibliographicReferenceDetailsPanel.loadMask = new Ext.LoadMask(bibliographicReferenceDetailsPanel.getEl(), {
					msg: "Loading ..."
				});
				
				// This status field cannot be set hidden in the config. Otherwise, it would be on top of the PubMed Id field.
				statusDisplayField.hide();
			}
		});    	
    	
		var pubMedStore = new Ext.data.Store({
			proxy: new Ext.data.DWRProxy({
				        apiActionToHandlerMap: {
			    	        read: {
			        	        dwrFunction: PhenotypeController.findBibliographicReference,
			            	    getDwrArgsFunction: function(request){
			            	    	return [request.params["pubMedId"]];
				                }
			    	        }
				        }
			    	}),
			reader: new Ext.data.JsonReader({
		        fields: Gemma.BibliographicReference.Record
			})
		});
		pubMedStore.on({
		    'load': {
		        fn: function(store, records, options){
					statusDisplayField.hide();
					bibliographicReferenceDetailsPanel.loadMask.hide();

		        	// Because it takes time to reload the store, update PudMed details
		        	// only when PudMed Id has not been changed (e.g. by clicking the Reset button).
					if (options.params.pubMedId === pubMedIdField.getValue()) {					
			        	if (pubMedStore.getTotalCount() > 0) {
							pubMedIdField.clearInvalid();
							
							bibliographicReferenceDetailsPanel.updateFields(pubMedStore.getAt(0));
							bibliographicReferenceDetailsPanel.show();
			        	} else {
							pubMedIdField.markInvalid("PubMed Id is not valid");
							
							bibliographicReferenceDetailsPanel.hide();
			        	}
		        	}		        	
		        },
		        scope:this
		    }
		});
		this.relayEvents(pubMedStore, ['load']);

		var updateBibliographicReferenceDetailsPanel = function() {
			var pubMedId = pubMedIdField.getValue();

			if (pubMedId === "") {
				bibliographicReferenceDetailsPanel.hide();
			} else if (pubMedId > 0) {
				if (bibliographicReferenceDetailsPanel.loadMask) {
					statusDisplayField.show();
					bibliographicReferenceDetailsPanel.loadMask.show();
				}

				pubMedStore.reload({
					params: {
						'pubMedId': pubMedId
					}
				});
			} else {
				bibliographicReferenceDetailsPanel.hide();
			}
		};
        
		Ext.apply(this, {
			getPubMedId: function() {
				return pubMedIdField.getValue();
			},
			setPubMedId: function(pubMedId) {
				pubMedIdField.setValue(pubMedId);
				
				if (pubMedId === '') {
					pubMedIdField.clearInvalid();
					this.hide();
				} else {
					this.show();
				}
				
				updateBibliographicReferenceDetailsPanel();
			},
			reset: function() {
				this.setPubMedId(pubMedIdField.originalValue);
			},
	        items :[
				{
					xtype: 'compositefield',
					border: false,
					layout: 'form',
					fieldLabel: 'PubMed Id',
				    items: [
						pubMedIdField,
						statusDisplayField
				    ]
				},
       			bibliographicReferenceDetailsPanel
	        ]
		});

		this.superclass().initComponent.call(this);
    }
});
