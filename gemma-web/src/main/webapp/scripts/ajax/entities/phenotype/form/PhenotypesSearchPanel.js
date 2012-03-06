/**
 * This panel contains one or more instances of PhenotypeSearchComboBox to let users specify phenotypes.  
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.PhenotypesSearchPanel = Ext.extend(Ext.Panel, {
	fieldLabel: 'Phenotype',
	border: false,
	layout: 'form',
    initComponent: function() {
		var originalPhenotypeSelections = null;
		var originalGeneSelection = null;

		var comboBoxCount = 0;
		var currentGeneNcbiId = null;

		var createComboButtonPanel = function(phenotypeSelection) {
			var phenotypeSearchComboBox = new Gemma.PhenotypeAssociationForm.PhenotypeSearchComboBox({
				currentGeneNcbiId: currentGeneNcbiId
			});
			
			this.relayEvents(phenotypeSearchComboBox,	['blur', 'select']);			
			phenotypeSearchComboBox.selectPhenotype(phenotypeSelection);	
			
			var comboButtonPanel = new Ext.Panel({
				border: false,
				layout: 'hbox',
			    	items: [ 
						phenotypeSearchComboBox,
						{
							xtype: 'button', // Remove phenotype button
							icon:'/Gemma/images/icons/subtract.png',
							margins: '0 0 3 3',			
							handler: function() {
								if (comboBoxCount > 1) {
									rowsPanel.remove(comboButtonPanel);
									rowsPanel.doLayout();
									comboBoxCount--;
				    				this.fireEvent('phenotypeFieldRemoved');
								} else {
									phenotypeSearchComboBox.clearValue();
				    				this.fireEvent('phenotypeFieldCleared');
								}
				    		},
							scope: this				    		
				    	}					
			    	] 
			}); 
			
			comboBoxCount++;
			
			return comboButtonPanel;
		}.createDelegate(this);
    	
		var rowsPanel = new Ext.Panel({
			border: false,
			layout: 'form',
	    	items: [
	    		createComboButtonPanel()
	    	] 
		}); 
    	
		var addPhenotypeRowButton = new Ext.Button({
			icon:'/Gemma/images/icons/add.png',
			fieldLabel: "&nbsp;",
			labelSeparator : '',
			handler: function() {
				rowsPanel.add(createComboButtonPanel());
				rowsPanel.doLayout();
				this.fireEvent('phenotypeFieldAdded');
			},
			scope: this			
		});
		
		Ext.apply(this, {
			selectPhenotypes: function (phenotypeSelections, geneSelection) {
				originalPhenotypeSelections = phenotypeSelections;
				originalGeneSelection = geneSelection;
				
				if (geneSelection == null) {
					currentGeneNcbiId = null;
				} else {
					currentGeneNcbiId = geneSelection.ncbiId;
				}

				// Keep only the first combo box and remove the rest.
				for (comboBoxCount -= 1; comboBoxCount > 0; comboBoxCount--) {
					rowsPanel.remove(rowsPanel.getComponent(comboBoxCount));
				}
				comboBoxCount++;

				var firstPhenotypeSearchComboBox = rowsPanel.items.itemAt(comboBoxCount - 1).items.find(function(item) {
				    return item instanceof Ext.form.ComboBox;
				});
				
				
				if (phenotypeSelections == null || phenotypeSelections.length <= 0) {
					firstPhenotypeSearchComboBox.setValue('');
					firstPhenotypeSearchComboBox.reset();
					firstPhenotypeSearchComboBox.clearInvalid();
				} else {
					firstPhenotypeSearchComboBox.selectPhenotype(phenotypeSelections[0]);
					firstPhenotypeSearchComboBox.currentGeneNcbiId = currentGeneNcbiId;
					
					for (var i = 1; i < phenotypeSelections.length; i++) {
						rowsPanel.add(createComboButtonPanel(phenotypeSelections[i]));
					}
					rowsPanel.doLayout();					
				}
			},
			validatePhenotypes: function() {
				var phenotypeValueUris = [];
				for (var i = 0; i < rowsPanel.items.length; ++i) {
					// Find the only one ComboBox in each item in rowsPanel.items.
				    var currPhenotypeSearchComboBox = rowsPanel.items.itemAt(i).items.find(function(item) {
				        return item instanceof Ext.form.ComboBox;
				    });
				
				    if (currPhenotypeSearchComboBox.getValue() === '') {
			        	currPhenotypeSearchComboBox.markInvalid('This field is required');
			        	phenotypeValueUris = null;
				    } else if (phenotypeValueUris != null) {
			        	phenotypeValueUris.push(currPhenotypeSearchComboBox.getValue());
					}
				}
				return phenotypeValueUris;
			},
			reset: function() {
				this.selectPhenotypes(originalPhenotypeSelections, originalGeneSelection);
			},
			setCurrentGeneNcbiId: function(newCurrentGeneNcbiId) {
				currentGeneNcbiId = newCurrentGeneNcbiId;

				for (var i = 0; i < rowsPanel.items.length; ++i) {
					// Find the only one ComboBox in each item in rowsPanel.items.
				    var currPhenotypeSearchComboBox = rowsPanel.items.itemAt(i).items.find(function(item) {
				        return item instanceof Ext.form.ComboBox;
				    });
				    currPhenotypeSearchComboBox.currentGeneNcbiId = currentGeneNcbiId;
				}
			},
	    	items: [ 
		    	rowsPanel,
				{
					xtype: 'compositefield',
					border: false,
					layout: 'form',
					hideLabel: true,
				    items: [
						addPhenotypeRowButton,
						{
							xtype: 'displayfield',
							value: 'To describe a complex phenotype, you can add more terms by clicking this button.',
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
