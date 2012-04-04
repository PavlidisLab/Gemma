/**
 * This experimental panel allows users to enter data for experimental evidence. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.ExperimentalPanel = Ext.extend(Ext.Panel, {
	border: false,
	layout: 'form',
	hidden: true,
    autoHeight:true,
    defaultType: 'textfield',
    initComponent: function() {
    	var hasDuplicate = false;
    	var isOnlyPrimaryEmpty = false;

    	var firePubMedIdInvalid = function(literaturePanel, shouldShow) {
    		if (hasDuplicate || isOnlyPrimaryEmpty || literaturePanel.isErrorShown !== shouldShow) {
    			literaturePanel.isErrorShown = shouldShow;

    			var invalidPubMedIdLabels = [];
    			if (primaryLiteraturePanel.isErrorShown) {
    				invalidPubMedIdLabels.push(primaryLiteraturePanel.pubMedIdFieldLabel);
    			}
    			if (secondaryLiteraturePanel.isErrorShown) {
    				invalidPubMedIdLabels.push(secondaryLiteraturePanel.pubMedIdFieldLabel);
    			}
    			this.fireEvent('pubMedIdsInvalid', invalidPubMedIdLabels);
    		}
    		hasDuplicate = false;
			isOnlyPrimaryEmpty = false;    		
    	}.createDelegate(this);

		var pubMedIdFieldBlurHandler = function(literaturePanel) {
			var pubMedId = literaturePanel.getPubMedId();
			
			if (pubMedId === "") {
				if (isOnlyPrimaryEmpty) {
					if (literaturePanel === secondaryLiteraturePanel) {
						firePubMedIdInvalid(literaturePanel, false);
					}
				} else {
					if (literaturePanel === primaryLiteraturePanel && secondaryLiteraturePanel.getPubMedId() !== '') {
						this.fireEvent('pubMedIdOnlyPrimaryEmpty',
							primaryLiteraturePanel.pubMedIdFieldLabel, secondaryLiteraturePanel.pubMedIdFieldLabel);
						isOnlyPrimaryEmpty = true;
					} else {
						firePubMedIdInvalid(literaturePanel, false);
					}
				}
			} else if (pubMedId <= 0) {
				if (isOnlyPrimaryEmpty) {
					if (literaturePanel === primaryLiteraturePanel) {
				    	firePubMedIdInvalid(literaturePanel, true);
					}
				} else {
					firePubMedIdInvalid(literaturePanel, true);
				}
			}
//			if (pubMedId === "") {
//				firePubMedIdInvalid(literaturePanel, false);
//			} else if (pubMedId <= 0) {
//				firePubMedIdInvalid(literaturePanel, true);
//			}
		}.createDelegate(this);
			
		var pubMedIdFieldKeyPressHandler = function(literaturePanel, event) {
			if (isOnlyPrimaryEmpty) {
				if (literaturePanel === primaryLiteraturePanel) {
					firePubMedIdInvalid(literaturePanel, false);
				}
			} else {
				firePubMedIdInvalid(literaturePanel, false);
			}
			
//			firePubMedIdInvalid(literaturePanel, false);
		};

		var pubMedIdStoreLoadHandler = function(literaturePanel, store, records, options) {
        	// Because it takes time to reload the store, show errors
        	// only when PudMed Id has not been changed (e.g. by clicking the Reset button).
			if (options.params.pubMedId === literaturePanel.getPubMedId()) {
				if (literaturePanel === secondaryLiteraturePanel && primaryLiteraturePanel.getPubMedId() === '') {
					this.fireEvent('pubMedIdOnlyPrimaryEmpty',
						primaryLiteraturePanel.pubMedIdFieldLabel, secondaryLiteraturePanel.pubMedIdFieldLabel);
					isOnlyPrimaryEmpty = true;
				} else if (store.getTotalCount() > 0) {
					if (primaryLiteraturePanel.getPubMedId() === secondaryLiteraturePanel.getPubMedId()) {
						this.fireEvent('pubMedIdsDuplicate',
							primaryLiteraturePanel.pubMedIdFieldLabel, secondaryLiteraturePanel.pubMedIdFieldLabel);
						hasDuplicate = true;
					} else {
//			    		firePubMedIdInvalid(literaturePanel, false);
						this.fireEvent('pubMedIdStoreLoad');
							    		
					}
		    	} else {
		    		firePubMedIdInvalid(literaturePanel, true);
		    	}
			}			    	
		}.createDelegate(this);

		var primaryLiteraturePanel = new Gemma.PhenotypeAssociationForm.LiteraturePanel({
			isErrorShown: false,
			pubMedIdFieldAllowBlank: true,
			pubMedIdFieldLabel: 'PubMed Id',
			listeners: {
				pubMedIdFieldBlur: pubMedIdFieldBlurHandler,  
				pubMedIdFieldKeyPress: pubMedIdFieldKeyPressHandler,
				pubMedIdStoreLoad: pubMedIdStoreLoadHandler
			}
    	});

		var secondaryLiteraturePanel = new Gemma.PhenotypeAssociationForm.LiteraturePanel({
			isErrorShown: false,
			pubMedIdFieldAllowBlank: true,
			pubMedIdFieldLabel: 'Secondary PubMed Id',
			listeners: {
				pubMedIdFieldBlur: pubMedIdFieldBlurHandler,  
				pubMedIdFieldKeyPress: pubMedIdFieldKeyPressHandler,
				pubMedIdStoreLoad: pubMedIdStoreLoadHandler
			}
		});
    	
    	var experimentTagsPanel = new Gemma.PhenotypeAssociationForm.ExperimentTagsPanel();

		this.relayEvents(experimentTagsPanel, ['keyup', 'select',  
			'experimentTagFieldAdded', 'experimentTagFieldCleared', 'experimentTagFieldRemoved']);

    	var setAllItemsVisible = function(container, isVisible) {
    		for (var i = 0; i < container.items.length; i++) {
    			container.items.items[i].setVisible(isVisible);
    		}
    	}
    	
		Ext.apply(this, {
			setEvidenceId: function(evidenceId) {
				primaryLiteraturePanel.setEvidenceId(evidenceId);
			},
			selectExperimentalData: function(primaryPubMedId,
				secondaryPubMedId, experimentTagSelections, geneSelection) {
				primaryLiteraturePanel.setPubMedId(primaryPubMedId);
				secondaryLiteraturePanel.setPubMedId(secondaryPubMedId);
				experimentTagsPanel.selectExperimentTags(experimentTagSelections, geneSelection);
			},
			listeners: {
				show: function(thisComponent) { 
					setAllItemsVisible(thisComponent, true);
				},
				hide: function(thisComponent) { 
					setAllItemsVisible(thisComponent, false);
				},
				scope: this
			},
			isValid: function() {
				var primaryPubMedId = primaryLiteraturePanel.getPubMedId();
				var secondaryPubMedId = secondaryLiteraturePanel.getPubMedId();

				return primaryLiteraturePanel.isValid() &&
					   secondaryLiteraturePanel.isValid() &&
					   ((primaryPubMedId !== secondaryPubMedId) ||
					    (primaryPubMedId === '' && secondaryPubMedId === '' )) &&
					   (primaryPubMedId !== '' || secondaryPubMedId === '') &&
					   experimentTagsPanel.isValid();
			},
			getValues: function() {
				var secondaryCitationValueObject = secondaryLiteraturePanel.getCitationValueObject();
			
				return { 
					primaryPublicationCitationValueObject: primaryLiteraturePanel.getCitationValueObject(),
					relevantPublicationsCitationValueObjects: secondaryCitationValueObject == null ?
						[] :
						[ secondaryCitationValueObject ],
					experimentCharacteristics: experimentTagsPanel.getSelectedExperimentTags()
				};
			},
			reset: function() {
				primaryLiteraturePanel.reset();
				secondaryLiteraturePanel.reset();
				experimentTagsPanel.reset();
			},
			setCurrentGeneTaxonId: function(newCurrentGeneTaxonId) {
				experimentTagsPanel.setCurrentGeneTaxonId(newCurrentGeneTaxonId);
			},
	        items :[
	        	primaryLiteraturePanel,
	        	secondaryLiteraturePanel,
	        	experimentTagsPanel
	        ]
		});
		this.superclass().initComponent.call(this);
    }
});