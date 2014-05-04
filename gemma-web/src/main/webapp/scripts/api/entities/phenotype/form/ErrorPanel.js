/**
 * Error panel displays errors and/or warnings.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.ErrorPanel = Ext.extend(Ext.Panel, {
      layout : 'form',
      border : false,
      style : 'padding: 5px; background-color: white;',
      autoHeight : true,
      hidden : true,
      initComponent : function() {
         var errorDisplayField = new Ext.form.DisplayField({
               hideLabel : true,
               value : ''
            });

         var showMessageInColor = function(message, color) {
            this.getEl().applyStyles('border: 3px solid ' + color + ';');
            errorDisplayField.getEl().applyStyles('color: ' + color + ';');
            errorDisplayField.setValue(message);
            this.show();
            this.ownerCt.doLayout();
         }.createDelegate(this);

         Ext.apply(this, {
               items : [errorDisplayField],
               getErrorMessage : function() {
                  return errorDisplayField.getValue();
               },
               hide : function() {
                  errorDisplayField.setValue('');
                  Gemma.PhenotypeAssociationForm.ErrorPanel.superclass.hide.call(this);
                  this.ownerCt.doLayout();
               },
               showError : function(message) {
                  showMessageInColor(message, 'red');
               },
               showWarning : function(message) {
                  showMessageInColor(message, 'orange');
               }
            });
         Gemma.PhenotypeAssociationForm.ErrorPanel.superclass.initComponent.call(this);
      }
   });
