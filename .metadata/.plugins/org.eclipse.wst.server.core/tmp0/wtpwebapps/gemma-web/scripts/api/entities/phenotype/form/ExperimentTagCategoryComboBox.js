/**
 * This ComboBox lets users specify experiment tag category. 
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.ExperimentTagCategoryComboBox = Ext.extend(Ext.form.ComboBox, {
	allowBlank: false,
	editable: false,
	forceSelection: true,				
	mode: 'local',
	store: new Ext.data.JsonStore({
		// Don't use 'id' because it is always zero.
		fields:  [ 'category', 'categoryUri' ],
		idProperty: 'categoryUri'
	}),
    valueField: 'categoryUri',
    displayField: 'category',
    width: 200,
    typeAhead: true,
    emptyText: 'Select category',
    triggerAction: 'all',
	selectOnFocus:true,
    initComponent: function() {
		var originalExperimentTagSelection = null;

		Ext.apply(this, {
			selectExperimentTagCategory: function(experimentTagSelection) {
				originalExperimentTagSelection = experimentTagSelection;
				
				if (experimentTagSelection == null) {
					this.setValue('');
					this.reset(); // If I don't have this line, I always see the invalid red border around the component.
					this.clearInvalid();
				} else {
					this.setValue(experimentTagSelection.categoryUri);
				}
			},
			getSelectedRecord: function() {
				var selectedCategoryUri = this.getValue();
				var selectedRecord = null;

				if (selectedCategoryUri !== '') {
					var record = this.getStore().getById(selectedCategoryUri);
					
					if (record != null) {
					selectedRecord = {
							// Use the original id if available.
							id: originalExperimentTagSelection == null ?
									0 :
									originalExperimentTagSelection.id,
							categoryUri: record.data.categoryUri,
							category: record.data.category
					};
				}
				}
				return selectedRecord;
			}
		});
		Gemma.PhenotypeAssociationForm.ExperimentTagCategoryComboBox.superclass.initComponent.call(this);
    }
});
