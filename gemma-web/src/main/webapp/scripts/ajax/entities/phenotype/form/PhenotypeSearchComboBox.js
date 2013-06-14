/**
 * This ComboBox lets users search for all existing phenotypes in the system.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.PhenotypeSearchComboBox = Ext.extend(Ext.form.ComboBox, {
      currentGeneNcbiId : null,
      allowBlank : false,
      enableKeyEvents : true,
      forceSelection : true,
      valueField : 'valueUri',
      displayField : 'value',
      typeAhead : false,
      loadingText : 'Searching...',
      minChars : 2,
      width : 400,
      listWidth : 400,
      pageSize : 0,
      hideTrigger : true,
      triggerAction : 'all',
      listEmptyText : 'No results found',
      getParams : function(query) {
         return [query, this.currentGeneNcbiId];
      },
      autoSelect : false,
      tpl : new Ext.XTemplate('<tpl for="."><div ext:qtip="{qtip}" style="font-size:11px" class="x-combo-list-item {style}">{value}</div></tpl>'),
      initComponent : function() {
         var id = 0;

         Ext.apply(this, {
               selectPhenotype : function(phenotypeSelection) {
                  if (phenotypeSelection != null) {
                     id = phenotypeSelection.id;

                     this.getStore().loadData([phenotypeSelection], true); // true to append the new record
                     this.setValue(phenotypeSelection.valueUri);
                  }
               },
               getSelectedPhenotype : function() {
                  var record = this.store.getById(this.getValue());

                  record.data.id = id;

                  return record.data;
               },
               store : new Ext.data.JsonStore({
                     proxy : new Ext.data.DWRProxy(PhenotypeController.searchOntologyForPhenotypes),
                     // Don't use 'id' because if this combo box is not removed, I should return the same id back to the
                     // server.
                     fields : ['valueUri', 'value', 'alreadyPresentInDatabase', 'alreadyPresentOnGene', 'urlId', {
                           name : 'qtip',
                           convert : function(value, record) {
                              return record.value + (record.valueUri ? '<br />' + record.valueUri : '');
                           }
                        }, {
                           name : 'style',
                           convert : function(value, record) {
                              if (record.alreadyPresentInDatabase) {
                                 return "usedWithUri";
                              } else {
                                 return "unusedWithUri";
                              }
                           }
                        }],
                     idProperty : 'valueUri'
                  })
            });
         Gemma.PhenotypeAssociationForm.PhenotypeSearchComboBox.superclass.initComponent.call(this);
      }
   });
