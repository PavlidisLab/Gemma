/**
 * This ComboBox lets users search for all existing phenotypes in the system. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.PhenotypeSearchComboBox = Ext.extend(Ext.form.ComboBox, {
	hiddenName: 'phenotypes[]',
	currentGeneNcbiId: null,    
	allowBlank: false,
	forceSelection: true,				
    store: new Ext.data.JsonStore({
		proxy: new Ext.data.DWRProxy(PhenotypeController.searchOntologyForPhenotypes),
	    fields: [ 'valueUri', 'value', 'alreadyPresentInDatabase', 'alreadyPresentOnGene', 'urlId', {
	    	name: 'comboText',
	    	convert: function(value, record) {
	    		return '<div style="font-size:12px;" class="x-combo-list-item" >' +
	    			record.value + '</div>';
	    	}
	    }],
	    idProperty: 'valueUri'
	}),
    valueField: 'valueUri', 
    displayField: 'value',
    typeAhead: false,
    loadingText: 'Searching...',
    minChars: 2,
    width: 400,
    listWidth: 400,
    pageSize: 0,
    hideTrigger: true,
    triggerAction: 'all',
	listEmptyText : 'No results found',
	getParams : function(query) {
		return [
			query,
			this.currentGeneNcbiId ];
	},
	autoSelect: false,
	tpl: new Ext.XTemplate('<tpl for=".">' +
			'{[ this.renderItem(values) ]}' +
			'</tpl>', {
				renderItem: function(values){
					var valueToBeDisplayed = "{value}";
					
					if (values.alreadyPresentInDatabase || values.alreadyPresentOnGene) {
						valueToBeDisplayed = "<b>" + valueToBeDisplayed + "</b>";
					}
					
					return new Ext.XTemplate('<div style="font-size:11px; background-color:#ECF4FF;" class="x-combo-list-item" ' +
										'ext:qtip="{value}">' + valueToBeDisplayed + '</div>').apply(values);
				}
			}),
	selectPhenotype: function (phenotypeSelection) {
		if (phenotypeSelection != null) {
			var PhenotypeRecord = Ext.data.Record.create([	
				'valueUri', 'value'
			]);
			this.getStore().add(new PhenotypeRecord({
				valueUri: phenotypeSelection.valueUri,
				value: phenotypeSelection.value
			}));
			this.setValue(phenotypeSelection.valueUri);
		}    	
	}
});
