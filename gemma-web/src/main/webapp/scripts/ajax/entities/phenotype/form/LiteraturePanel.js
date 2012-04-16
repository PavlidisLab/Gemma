/**
 * This literature panel contains literature information. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.LiteraturePanel = Ext.extend(Ext.Panel, {
	pubMedIdFieldAllowBlank: false,
	pubMedIdFieldLabel: 'PubMed Id',
	evidenceId: null,
	border: false,
	layout: 'form',
	hidden: true,
    autoHeight:true,
    defaultType: 'textfield',
    initComponent: function() {
    	var previousPubMedId = '';
    	
		var pubMedIdField = new Ext.form.NumberField({
			minValue: 1,			
			minLength: 1,
			maxLength: 9,
			// It is set to true initially because it is hidden and will 
			// be set to this.pubMedIdFieldAllowBlank when the panel is shown. 
			allowBlank: true,
			allowDecimals: false,
			allowNegative: false,
			width: 100,
			autoCreate: {tag: 'input', type: 'text', size: '20', autocomplete: 'off', maxlength: '9'},
			enableKeyEvents: true,
		    initComponent: function() {

				Ext.apply(this, {
					listeners: {
						blur: function(numberField) {
							updateBibliographicReferenceDetailsPanel(true);
						}
					}
				});

				this.superclass().initComponent.call(this);
	    	}
		});

		pubMedIdField.on({
			blur: function(numberField) {
				this.fireEvent('pubMedIdFieldBlur', this);
			},
			// Don't use keypress because when I do pubMedIdField.getValue(), I
			// just get the PubMed Id before keypress event occurs. 
			keyup: function(numberField, event) {
				var currentPubMedId = pubMedIdField.getValue();
				
				// Fire event only when PubMed Id has been changed.
				if (previousPubMedId !== currentPubMedId) {
					previousPubMedId = currentPubMedId;
	
					this.fireEvent('pubMedIdFieldKeyUp', this, event);
				}
			},
			scope: this
		})
		
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
		bibliographicReferenceDetailsPanel.on('render', function(thisPanel) {
			if (!thisPanel.loadMask) {
				thisPanel.loadMask = new Ext.LoadMask(thisPanel.getEl(), {
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
			            	    	return [request.params["pubMedId"], this.evidenceId];
				                }.createDelegate(this)
			    	        }
				        }
			    	}),
			reader: new Ext.data.JsonReader({
		        fields: Gemma.BibliographicReference.Record
			})
		});
		pubMedStore.on({
		    'load': {
		        fn: function(store, records, options) {
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

					if (options.params.shouldFireEvent) {
						this.fireEvent('pubMedIdStoreLoad', this, store, records, options);
					}					
		        },
		        scope:this
		    }
		});

		var updateBibliographicReferenceDetailsPanel = function(shouldFireEvent) {
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
						'pubMedId': pubMedId,
						'shouldFireEvent': shouldFireEvent						
					}
				});
			} else {
				bibliographicReferenceDetailsPanel.hide();
			}
		};
        
		Ext.apply(this, {
			listeners: {
				show: function(thisComponent) {
					pubMedIdField.allowBlank = this.pubMedIdFieldAllowBlank;		
				},
				hide: function(thisComponent) {
					pubMedIdField.allowBlank = true;
				},
				scope:this
			},
			isValid: function() {
				var pubMedId = pubMedIdField.getValue(); 
				var isPubMedIdValid = false;
				if (pubMedId === '') {
					isPubMedIdValid = this.pubMedIdFieldAllowBlank;
				} else if (pubMedId > 0) {
					isPubMedIdValid = pubMedStore.getTotalCount() > 0;
				}
				
				return isPubMedIdValid;
			},
			getPubMedId: function() {
				return pubMedIdField.getValue();
			},
			getCitationValueObject: function() {
				var pubMedId = this.getPubMedId();
				if (pubMedId === '') {
					return null;
				} else {
					var citationValueObject = new CitationValueObject();
					citationValueObject.pubmedAccession = pubMedId;
	
					return citationValueObject;
				}
			},
			setPubMedId: function(pubMedId) {
				// Don't use !== because pubMedId is a string while pubMedIdField.getValue() is a number.
				if (pubMedIdField.getValue() != pubMedId) {
					pubMedIdField.setValue(pubMedId);
					pubMedIdField.originalValue = pubMedId;
					
					updateBibliographicReferenceDetailsPanel(false);
				}
				
				if (pubMedId === '') {
					pubMedIdField.clearInvalid();
					this.hide();
				} else {
					this.show();
				}
			},
			setEvidenceId: function(evidenceId) {
				this.evidenceId = evidenceId;
			},
			reset: function() {
				this.setPubMedId(pubMedIdField.originalValue);
			},
	        items :[
				{
					xtype: 'compositefield',
					border: false,
					layout: 'form',
					fieldLabel: this.pubMedIdFieldLabel,
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
