/**
 * This ComboBox lets users specify type of evidence.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.EvidenceTypeComboBox = Ext.extend(Ext.form.ComboBox, {
	hiddenName: 'evidenceClassName',
	valueField: 'evidenceClassName',
	fieldLabel: 'Type of Evidence',
	allowBlank: false,
	mode: 'local',
	store: new Ext.data.ArrayStore({
		fields:  ['evidenceClassName', 'evidenceType'],
		data: [['LiteratureEvidenceValueObject', 'Literature']]
	}),
	forceSelection: true,				
	displayField:'evidenceType',
	width: 200,
	typeAhead: true,
	triggerAction: 'all',
	selectOnFocus:true,
    initComponent: function() {
		var originalEvidenceClassName = null;
    	
		Ext.apply(this, {
			selectEvidenceType: function(evidenceClassName) {
				originalEvidenceClassName = evidenceClassName;

				if (evidenceClassName == null) {
				    this.setValue('');
					this.clearInvalid();
				} else {
					this.setValue(evidenceClassName);
				}    	
			},
			reset: function() {
			    this.selectEvidenceType(originalEvidenceClassName);
			}
		});
		
		this.superclass().initComponent.call(this);
    }
});
