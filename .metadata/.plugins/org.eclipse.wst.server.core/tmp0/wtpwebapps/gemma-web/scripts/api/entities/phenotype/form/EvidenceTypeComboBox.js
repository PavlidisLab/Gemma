/**
 * This ComboBox lets users specify type of evidence.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.EvidenceTypeComboBox = Ext.extend(Ext.form.ComboBox, {
      valueField : 'evidenceClassName',
      fieldLabel : 'Type of Evidence',
      allowBlank : false,
      editable : false,
      mode : 'local',
      store : new Ext.data.ArrayStore({
            fields : ['evidenceClassName', 'evidenceType'],
            data : [['ExperimentalEvidenceValueObject', 'Experimental'], ['LiteratureEvidenceValueObject', 'Literature']]
         }),
      forceSelection : true,
      displayField : 'evidenceType',
      width : 200,
      typeAhead : true,
      triggerAction : 'all',
      selectOnFocus : true,
      initComponent : function() {
         Ext.apply(this, {
               selectEvidenceType : function(evidenceClassName) {
                  if (evidenceClassName === null) {
                     this.setValue('');
                     this.clearInvalid();
                  } else {
                     this.setValue(evidenceClassName);
                  }
               }
            });

         Gemma.PhenotypeAssociationForm.EvidenceTypeComboBox.superclass.initComponent.call(this);
      }
   });
