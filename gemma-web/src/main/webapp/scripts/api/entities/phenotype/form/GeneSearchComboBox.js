/**
 * This ComboBox lets users search for all existing genes in the system.
 * 
 * @author frances
 * 
 */
Ext.namespace( 'Gemma.PhenotypeAssociationForm' );

Gemma.PhenotypeAssociationForm.GeneSearchComboBox = Ext
   .extend(
      Ext.form.ComboBox,
      {
         allowBlank : false,
         forceSelection : true,
         store : new Ext.data.JsonStore( {
            proxy : new Ext.data.DWRProxy( GenePickerController.searchGenesWithNCBIId ),
            fields : [
                      'id',
                      'ncbiId',
                      'officialSymbol',
                      'officialName',
                      'taxonCommonName',
                      'taxonId',
                      {
                         name : 'comboText',
                         convert : function( value, record ) {
                            return '<div class="x-combo-list-item" >' + record.officialName
                               + ' <span style="color:grey">(' + record.taxonCommonName + ')</span></div>';
                         }
                      } ],
            idProperty : 'ncbiId'
         } ),
         valueField : 'ncbiId',
         displayField : 'officialSymbol',
         typeAhead : false,
         loadingText : 'Searching...',
         emptyText : 'Search by keyword, ID or symbol',
         minChars : 2,
         width : 150,
         listWidth : 400,
         pageSize : 0,
         hideTrigger : true,
         triggerAction : 'all',
         listEmptyText : 'No results',
         getParams : function( query ) {
            return [ query, null ];
         },
         autoSelect : false,
         tpl : new Ext.XTemplate(
            '<tpl for="."><div style="font-size:11px;background-color:#ECF4FF" class="x-combo-list-item" '
               + 'ext:qtip="{officialSymbol}: {officialName} ({taxonCommonName})"><b>{officialSymbol}</b>: {officialName} <span style="color:grey">({taxonCommonName})</span></div></tpl>' ),
         initComponent : function() {
            var geneSelectedLabel = new Ext.form.Label();

            Ext.apply( this, {
               getGeneSelectedLabel : function() {
                  return geneSelectedLabel;
               },
               listeners : {
                  focus : function( combo ) {
                     geneSelectedLabel.setText( '' );
                  },
                  blur : function( combo ) {
                     // I have to do the following because users may have selected a value from the combobox and then
                     // delete all text right after the selection.
                     if ( this.value === '' ) {
                        geneSelectedLabel.setText( "", false );
                     } else {
                        var record = this.findRecord( this.valueField, this.value );
                        if ( record ) {
                           geneSelectedLabel.setText( record.data.comboText, false );
                        } else {
                           geneSelectedLabel.setText( "", false );
                        }
                     }
                  },
                  select : function( combo, record, index ) {
                     geneSelectedLabel.setText( record.data.comboText, false );

                     this.collapse();
                  },
                  scope : this
               },
               selectGene : function( geneSelection ) {
                  if ( geneSelection == null ) {
                     geneSelectedLabel.setText( "", false );
                     this.setValue( '' );
                     this.reset(); // If I don't have this line, I always see the invalid red border around the
                     // component.
                     this.clearInvalid();
                  } else {
                     this.getStore().loadData( [ {
                        id : geneSelection.id,
                        ncbiId : geneSelection.ncbiId,
                        officialSymbol : geneSelection.officialSymbol,
                        officialName : geneSelection.officialName,
                        taxonCommonName : geneSelection.taxonCommonName,
                        taxonId : geneSelection.taxonId
                     } ] );
                     this.setValue( geneSelection.ncbiId );
                     geneSelectedLabel.setText( this.getStore().getAt( 0 ).data.comboText, false );
                  }
               }
            } );

            Gemma.PhenotypeAssociationForm.GeneSearchComboBox.superclass.initComponent.call( this );
         }
      } );
