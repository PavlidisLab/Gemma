/**
 * This ComboBox lets users specify experiment tag value. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.ExperimentTagValueComboBox = Ext.extend(Ext.form.ComboBox, {
	currentCategoryUri: null,	
	currentGeneTaxonId: null,    
	allowBlank: false,
	enableKeyEvents: true,
	forceSelection: false,				
    store: new Ext.data.JsonStore({
		proxy: new Ext.data.DWRProxy(PhenotypeController.findExperimentOntologyValue),
		 // Don't use 'id' because it is always zero.
	    fields: [ 'valueUri', 'value', {	    
		    	name: 'qtip',
		    	convert: function(value, record) {
		    		return record.valueUri ? 
		    			record.valueUri :
		    			record.value;
		    	}
			}, {
		    	name: 'style',
		    	convert: function(value, record) {
					if (record.alreadyPresentInDatabase) {
						return record.valueUri ? "usedWithUri" : "usedNoUri";
					} else {
						return record.valueUri ? "unusedWithUri" : "unusedNoUri";
					}
		    	}
			}			
	    ],
	    idProperty: 'value'
	}),
    valueField: 'value', 
    displayField: 'value',
    typeAhead: false,
    loadingText: 'Searching...',
    emptyText: 'Enter term',
    minChars: 2,
    width: 200,
    listWidth: 200,
    pageSize: 0,
    hideTrigger: true,
    triggerAction: 'all',
	listEmptyText : 'No results found',
	getParams : function(query) {
		return [
			query,
			this.currentCategoryUri,
			this.currentGeneTaxonId 
		];
	},
	autoSelect: false,
	tpl: new Ext.XTemplate('<tpl for="."><div ext:qtip="{qtip}"  style="font-size:11px" class="x-combo-list-item {style}">{value}</div></tpl>'),
    initComponent: function() {
		var originalExperimentTagSelection = null;
    	
		Ext.apply(this, {
			selectExperimentTagValue: function(experimentTagSelection) {
				originalExperimentTagSelection = experimentTagSelection;
				
				if (experimentTagSelection == null) {
					this.setValue('');
					this.reset(); // If I don't have this line, I always see the invalid red border around the component.
					this.clearInvalid();
				} else {
					this.getStore().loadData([{
						valueUri: experimentTagSelection.valueUri,
						value: experimentTagSelection.value
					}]);
					
					this.setValue(experimentTagSelection.value);
				}
			}
		});
		this.superclass().initComponent.call(this);
    }
});
