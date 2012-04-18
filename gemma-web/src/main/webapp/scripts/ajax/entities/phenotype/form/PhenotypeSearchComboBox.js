/**
 * This ComboBox lets users search for all existing phenotypes in the system. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.PhenotypeSearchComboBox = Ext.extend(Ext.form.ComboBox, {
	currentGeneNcbiId: null,    
	allowBlank: false,
	enableKeyEvents: true,
	forceSelection: true,				
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
    initComponent: function() {
    	var id = 0;

		Ext.apply(this, {
			selectPhenotype: function(phenotypeSelection) {
				if (phenotypeSelection != null) {
					id = phenotypeSelection.id;

					this.getStore().loadData(
						[ phenotypeSelection ],
						true); // true to append the new record
					this.setValue(phenotypeSelection.valueUri);
				}    	
			},
			getSelectedPhenotype: function() {
				var record = this.store.getById(this.getValue());
				
				record.data.id = id;
				
				return record.data;
			},
		    store: new Ext.data.JsonStore({
				proxy: new Ext.data.DWRProxy(PhenotypeController.searchOntologyForPhenotypes),
				// Don't use 'id' because if this combo box is not removed, I should return the same id back to the server.
			    fields: [ 'valueUri', 'value', 'alreadyPresentInDatabase', 'alreadyPresentOnGene', 'urlId' ],
			    idProperty: 'valueUri'
			})
		});
		this.superclass().initComponent.call(this);
    }
});
