/**
 * This ComboBox lets users specify experiment tag value.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.ExperimentTagValueComboBox = Ext.extend(Ext.form.ComboBox, {
      currentCategoryUri : null,
      currentGeneTaxonId : null,
      minLength : 2,
      maxLength : 255,
      allowBlank : false,
      enableKeyEvents : true,
      forceSelection : false,
      valueField : 'value',
      displayField : 'value',
      typeAhead : false,
      loadingText : 'Searching...',
      emptyText : 'Enter term',
      minChars : 3,
      width : 200,
      listWidth : 320,
      pageSize : 0,
      hideTrigger : true,
      triggerAction : 'all',
      listEmptyText : 'No results found',
      getParams : function(query) {
         return [query, this.currentCategoryUri, this.currentGeneTaxonId];
      },
      autoSelect : false,
      tpl : new Ext.XTemplate('<tpl for="."><div ext:qtip="{qtip}" style="font-size:11px" class="x-combo-list-item {style}">{value}</div></tpl>'),
      getSelectedRecord : function() {
         // Use getRawValue() instead of getValue() because getRawValue()
         // returns whatever text typed by users.
         var rawValue = this.getRawValue();

         var selectedRecord;
         if (rawValue === '') {
            selectedRecord = null;
         } else {
            var storeRecord = this.store.getById(rawValue);
            selectedRecord = {
               value : rawValue,
               // Note that users may type a value that has corresponding valueUri without selecting
               // it from the drop-down list. So, storeRecord can be null in this case.
               valueUri : storeRecord == null ? '' : storeRecord.data.valueUri
            };

         }

         return selectedRecord;
      },
      initComponent : function() {
         var originalExperimentTagSelection = null;

         Ext.apply(this, {
               autoCreate : {
                  tag : 'input',
                  type : 'text',
                  maxlength : this.maxLength
               },
               store : new Ext.data.JsonStore({
                     proxy : new Ext.data.DWRProxy(PhenotypeController.findExperimentOntologyValue),
                     // Don't use 'id' because it is always zero.
                     fields : ['valueUri', 'value', {
                           name : 'qtip',
                           convert : function(value, record) {
                              return record.value + (record.valueUri ? '<br />' + record.valueUri : '');
                           }
                        }, {
                           name : 'style',
                           convert : function(value, record) {
                              if (record.alreadyPresentInDatabase) {
                                 return record.valueUri ? "usedWithUri" : "usedNoUri";
                              } else {
                                 return "unusedWithUri";
                              }
                           }
                        }],
                     idProperty : 'value'
                  }),
               selectExperimentTagValue : function(experimentTagSelection) {
                  originalExperimentTagSelection = experimentTagSelection;

                  if (experimentTagSelection == null) {
                     this.setValue('');
                     this.reset(); // If I don't have this line, I always see the invalid red border around the
                                    // component.
                     this.clearInvalid();
                  } else {
                     this.getStore().loadData([{
                           valueUri : experimentTagSelection.valueUri,
                           value : experimentTagSelection.value
                        }]);

                     this.setValue(experimentTagSelection.value);
                  }
               }
            });
         Gemma.PhenotypeAssociationForm.ExperimentTagValueComboBox.superclass.initComponent.call(this);
      }
   });
