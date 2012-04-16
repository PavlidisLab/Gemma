/**
 * This panel contains one or more instances of a panel containing 
 * ExperimentTagCategoryComboBox and ExperimentTagValueComboBox for
 * users to specify experiment tags.  
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

// Make categories global so that they are available all the time and we don't retrieve them from server as needed.  
Gemma.PhenotypeAssociationForm.ExperimentTagCategories = null;

Gemma.PhenotypeAssociationForm.ExperimentTagsPanel = Ext.extend(Ext.Panel, {
	fieldLabel: 'Experiment Tag',
	border: false,
	layout: 'form',
    initComponent: function() {
		var originalExperimentTagSelections = null;
		var originalGeneSelection = null;

		var comboBoxCount = 0;
		var currentGeneTaxonId = null;

		var createRowPanel = function(experimentTagSelection) {
			var experimentTagCategoryComboBox = new Gemma.PhenotypeAssociationForm.ExperimentTagCategoryComboBox({
				storeData: Gemma.PhenotypeAssociationForm.ExperimentTagCategories,
				listeners: {
					blur: function(combo) {
						var currentCategoryUri = combo.getValue();
						experimentTagValueComboBox.currentCategoryUri = (currentCategoryUri == '') ?
							null :
							currentCategoryUri;				
					},
					scope: this
				}			
			});
			var experimentTagValueComboBox = new Gemma.PhenotypeAssociationForm.ExperimentTagValueComboBox({
				currentCategoryUri: experimentTagSelection == null ?
										null :
										experimentTagSelection.categoryUri,
				currentGeneTaxonId: currentGeneTaxonId,
				margins: '0 0 0 3' // Add some space after the category.
			});
			
			experimentTagCategoryComboBox.selectExperimentTagCategory(experimentTagSelection);	
			experimentTagValueComboBox.selectExperimentTagValue(experimentTagSelection);
			
			this.relayEvents(experimentTagCategoryComboBox, ['select']);			
			this.relayEvents(experimentTagValueComboBox, ['keyup', 'select']);
			
			var rowPanel = new Ext.Panel({
				border: false,
				layout: 'hbox',
				getCategoryComboBox: function() {
					return experimentTagCategoryComboBox;
				},
				getValueComboBox: function() {
					return experimentTagValueComboBox;
				},
		    	items: [ 
					experimentTagCategoryComboBox,
					experimentTagValueComboBox,
					{
						xtype: 'button', // Remove experiment tag button
						icon:'/Gemma/images/icons/subtract.png',
						margins: '0 0 3 3',			
						handler: function() {
							if (comboBoxCount > 1) {
								rowsPanel.remove(rowPanel);
								rowsPanel.doLayout();
								comboBoxCount--;
			    				this.fireEvent('experimentTagFieldRemoved');
							} else {
								experimentTagCategoryComboBox.clearValue();
								experimentTagValueComboBox.clearValue();
			    				this.fireEvent('experimentTagFieldCleared');
							}
			    		},
						scope: this				    		
			    	}					
		    	] 
			}); 
			
			comboBoxCount++;
			
			return rowPanel;
		}.createDelegate(this);
    	
		var rowsPanel = new Ext.Panel({
			border: false,
			layout: 'form',
	    	items: [
	    		createRowPanel()
	    	] 
		}); 
    	
		var addExperimentTagRowButton = new Ext.Button({
			icon:'/Gemma/images/icons/add.png',
			fieldLabel: "&nbsp;",
			labelSeparator : '',
			handler: function() {
				rowsPanel.add(createRowPanel());
				rowsPanel.doLayout();
				this.fireEvent('experimentTagFieldAdded');
			},
			scope: this			
		});

		var visiblityHandler = function(isVisible) {
			if (isVisible && Gemma.PhenotypeAssociationForm.ExperimentTagCategories == null) {
				PhenotypeController.findExperimentMgedCategory(function(characteristicValueObjects) {
					Gemma.PhenotypeAssociationForm.ExperimentTagCategories = [];
					for (var i = 0; i < characteristicValueObjects.length; i++) {
						Gemma.PhenotypeAssociationForm.ExperimentTagCategories.push({
							categoryUri: characteristicValueObjects[i].categoryUri,
							category: characteristicValueObjects[i].category
						})
					}
					
					for (var i = 0; i < rowsPanel.items.length; i++) {
						var currRowPanel = rowsPanel.items.itemAt(i);
					
						var currentValue = currRowPanel.getCategoryComboBox().getValue();
						
						// TODO: Make sure if there is a better way to do the following.
						// Because store is shared among all the combo boxes, just load data for the first one.
						if (i === 0) {
							currRowPanel.getCategoryComboBox().getStore().loadData(Gemma.PhenotypeAssociationForm.ExperimentTagCategories);
						}
						if (currentValue !== '') {
							currRowPanel.getCategoryComboBox().setValue(currentValue);
						}
					}
				}.createDelegate(this));
			} 
			
			for (var i = 0; i < rowsPanel.items.length; i++) {
				var currRowPanel = rowsPanel.items.itemAt(i);
			
				// Make sure that if this panel is not visible, all fields' allowBlank properties are true and vice versa.
				currRowPanel.getCategoryComboBox().allowBlank = !isVisible;
				currRowPanel.getValueComboBox().allowBlank = !isVisible;			    
			}
		}

		Ext.apply(this, {
			selectExperimentTags: function(experimentTagSelections, geneSelection) {
				originalExperimentTagSelections = experimentTagSelections;
				originalGeneSelection = geneSelection;
				
				if (geneSelection == null) {
					currentGeneTaxonId = null;
				} else {
					currentGeneTaxonId = geneSelection.taxonId;
				}

				// Keep only the first combo box and remove the rest.
				for (comboBoxCount -= 1; comboBoxCount > 0; comboBoxCount--) {
					rowsPanel.remove(rowsPanel.getComponent(comboBoxCount));
				}
				comboBoxCount++;

				var firstExperimentTagCategoryComboBox = rowsPanel.items.itemAt(comboBoxCount - 1).getCategoryComboBox();
				var firstExperimentTagValueComboBox = rowsPanel.items.itemAt(comboBoxCount - 1).getValueComboBox();
				firstExperimentTagValueComboBox.currentGeneTaxonId = currentGeneTaxonId;
				
				if (experimentTagSelections == null || experimentTagSelections.length <= 0) {
					firstExperimentTagCategoryComboBox.setValue('');
					firstExperimentTagCategoryComboBox.reset();
					firstExperimentTagCategoryComboBox.clearInvalid();

					firstExperimentTagValueComboBox.setValue('');
					firstExperimentTagValueComboBox.reset();
					firstExperimentTagValueComboBox.clearInvalid();
					
					firstExperimentTagValueComboBox.currentCategoryUri = null;
				} else {
					firstExperimentTagCategoryComboBox.selectExperimentTagCategory(experimentTagSelections[0]);
					firstExperimentTagValueComboBox.selectExperimentTagValue(experimentTagSelections[0]);
					
					firstExperimentTagValueComboBox.currentCategoryUri = firstExperimentTagCategoryComboBox.getValue();
					
					for (var i = 1; i < experimentTagSelections.length; i++) {
						rowsPanel.add(createRowPanel(experimentTagSelections[i]));
					}
					rowsPanel.doLayout();					
				}
			},
			getSelectedExperimentTags: function() {
				var selectedExperimentTags = [];
				for (var i = 0; selectedExperimentTags != null && i < rowsPanel.items.length; i++) {
					var currRowPanel = rowsPanel.items.itemAt(i);
				
					var currCategoryRecord = currRowPanel.getCategoryComboBox().getSelectedRecord();
					var currValueRecord = currRowPanel.getValueComboBox().getSelectedRecord();					
				    
				    if (currCategoryRecord == null || currValueRecord == null) {
			        	selectedExperimentTags = null;
				    } else {
						var characteristicValueObject = new CharacteristicValueObject();
						characteristicValueObject.id = currCategoryRecord.id;
						characteristicValueObject.category = currCategoryRecord.category;
						characteristicValueObject.categoryUri = currCategoryRecord.categoryUri;
						characteristicValueObject.value = currValueRecord.value;
						characteristicValueObject.valueUri = currValueRecord.valueUri;
						
						selectedExperimentTags.push(characteristicValueObject);				    	
					}
				}
				return selectedExperimentTags;
			},
			checkDuplicate: function() {
				var hasDuplicate = false;
				for (var i = 0; i < rowsPanel.items.length; i++) {
					var currRowPanel = rowsPanel.items.itemAt(i);
					
					currRowPanel.getCategoryComboBox().clearInvalid();
					currRowPanel.getValueComboBox().clearInvalid();
				}
				
				for (var i = 0; i < rowsPanel.items.length; i++) {
					var currRowPanel = rowsPanel.items.itemAt(i);
					var currCategoryRecord = currRowPanel.getCategoryComboBox().getSelectedRecord();
					
					var currCategory = currCategoryRecord == null ?
											null :
											currCategoryRecord.category;

					// Use getRawValue() instead of getValue() because getRawValue() returns whatever text typed by users.
					var currValue = currRowPanel.getValueComboBox().getRawValue();
					
					for (var j = i + 1; currCategory != null && currValue !== '' && j < rowsPanel.items.length; j++) {
						var currTestRowPanel = rowsPanel.items.itemAt(j);
						var currTestCategoryRecord = currTestRowPanel.getCategoryComboBox().getSelectedRecord();
						
						var currTestCategory = currTestCategoryRecord == null ?
													null :
													currTestCategoryRecord.category;
	
						var currTestValue = currTestRowPanel.getValueComboBox().getRawValue();
						
						if (currCategory === currTestCategory && currValue === currTestValue) {
							currRowPanel.getCategoryComboBox().markInvalid();
							currRowPanel.getValueComboBox().markInvalid();
							currTestRowPanel.getCategoryComboBox().markInvalid();
							currTestRowPanel.getValueComboBox().markInvalid();

							hasDuplicate = true;
						}
					}
				}
				return hasDuplicate;
			},
			reset: function() {
				this.selectExperimentTags(originalExperimentTagSelections, originalGeneSelection);
				if (originalExperimentTagSelections == null) {
					this.hide();
				}
			},
			setCurrentGeneTaxonId: function(newCurrentGeneTaxonId) {
				currentGeneTaxonId = newCurrentGeneTaxonId;

				for (var i = 0; i < rowsPanel.items.length; i++) {
				    var currValueComboBox = rowsPanel.items.itemAt(i).getValueComboBox();
				    currValueComboBox.currentGeneTaxonId = currentGeneTaxonId;
				}
			},
			isValid: function() {
				for (var i = 0; i < rowsPanel.items.length; i++) {
					var currRowPanel = rowsPanel.items.itemAt(i);
				
					if (currRowPanel.getCategoryComboBox().getValue() === '' ||
						currRowPanel.getValueComboBox().getRawValue() === '') {
						return false;
					}
				}
				return true;
			},
			listeners: {
				show: function(thisComponent) {
					visiblityHandler(true);
				},
				hide: function(thisComponent) {
					visiblityHandler(false);
				},
				scope:this
			},
	    	items: [
		    	rowsPanel,
				{
					xtype: 'compositefield',
					border: false,
					layout: 'form',
					hideLabel: true,
				    items: [
						addExperimentTagRowButton,
						{
							xtype: 'displayfield',
							value: 'You can add more experiment tags by clicking this button.',
							style: 'color: grey;',
							margins: '4 0 0 0'			
						}
				    ]
				}
	    	]
		});
		this.superclass().initComponent.call(this);
    }
});
