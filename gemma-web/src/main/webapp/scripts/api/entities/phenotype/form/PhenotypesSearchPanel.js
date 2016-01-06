/**
 * This panel contains one or more instances of PhenotypeSearchComboBox to let users specify phenotypes.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace( 'Gemma.PhenotypeAssociationForm' );

Gemma.PhenotypeAssociationForm.PhenotypesSearchPanel = Ext.extend( Ext.Panel, {
   fieldLabel : 'Phenotype',
   border : false,
   layout : 'form',
   initComponent : function() {
      var errorMessages = [];

      var comboBoxCount = 0;
      var currentGeneNcbiId = null;

      var createRowPanel = function( phenotypeSelection ) {
         var phenotypeSearchComboBox = new Gemma.PhenotypeAssociationForm.PhenotypeSearchComboBox( {
            currentGeneNcbiId : currentGeneNcbiId
         } );

         phenotypeSearchComboBox.on( {
            keyup : function( comboBox, event ) {
               updateValidity( true );
            },
            select : function( comboBox, record, index ) {
               updateValidity( false );
            }
         } );

         phenotypeSearchComboBox.selectPhenotype( phenotypeSelection );

         var rowPanel = new Ext.Panel( {
            border : false,
            layout : 'hbox',
            getPhenotypeSearchComboBox : function() {
               return phenotypeSearchComboBox;
            },
            items : [ phenotypeSearchComboBox, {
               xtype : 'button', // Remove phenotype button
               icon : '/Gemma/images/icons/subtract.png',
               margins : '0 0 3 3',
               handler : function() {
                  if ( comboBoxCount > 1 ) {
                     rowsPanel.remove( rowPanel );
                     rowsPanel.doLayout();
                     comboBoxCount--;
                  } else {
                     phenotypeSearchComboBox.clearValue();
                  }

                  // Clearing combo box should also update validity.
                  updateValidity( false );
               },
               scope : this
            } ]
         } );

         comboBoxCount++;

         return rowPanel;
      }.createDelegate( this );

      var rowsPanel = new Ext.Panel( {
         border : false,
         layout : 'form',
         items : [ createRowPanel() ]
      } );

      var addPhenotypeRowButton = new Ext.Button( {
         icon : '/Gemma/images/icons/add.png',
         fieldLabel : "&nbsp;",
         labelSeparator : '',
         handler : function() {
            rowsPanel.add( createRowPanel() );
            rowsPanel.doLayout();
         },
         scope : this
      } );

      var checkDuplicate = function() {
         var hasDuplicate = false;
         for (var i = 0; i < rowsPanel.items.length; i++) {
            rowsPanel.items.itemAt( i ).getPhenotypeSearchComboBox().clearInvalid();
         }

         for (var i = 0; i < rowsPanel.items.length; i++) {
            var currComboBox = rowsPanel.items.itemAt( i ).getPhenotypeSearchComboBox();
            var currValue = currComboBox.getRawValue();

            for (var j = i + 1; currValue !== '' && j < rowsPanel.items.length; j++) {
               var currTestComboBox = rowsPanel.items.itemAt( j ).getPhenotypeSearchComboBox();
               var currTestValue = currTestComboBox.getRawValue();

               if ( currValue === currTestValue ) {
                  currComboBox.markInvalid();
                  currTestComboBox.markInvalid();

                  hasDuplicate = true;
               }
            }
         }
         return hasDuplicate;
      };

      var updateValidity = function( isModifying ) {
         errorMessages = [];

         if ( checkDuplicate() ) {
            errorMessages
               .push( Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.phenotypesDuplicate );
         }
         this.fireEvent( 'validtyStatusChanged', isModifying, errorMessages );
      }.createDelegate( this );

      Ext.apply( this,
         {
            selectPhenotypes : function( phenotypeSelections, geneSelection ) {
               if ( geneSelection == null ) {
                  currentGeneNcbiId = null;
               } else {
                  currentGeneNcbiId = geneSelection.ncbiId;
               }

               // Keep only the first combo box and remove the rest.
               for (comboBoxCount -= 1; comboBoxCount > 0; comboBoxCount--) {
                  rowsPanel.remove( rowsPanel.getComponent( comboBoxCount ) );
               }
               comboBoxCount++;

               var firstPhenotypeSearchComboBox = rowsPanel.items.itemAt( comboBoxCount - 1 )
                  .getPhenotypeSearchComboBox();
               firstPhenotypeSearchComboBox.currentGeneNcbiId = currentGeneNcbiId;

               if ( phenotypeSelections == null || phenotypeSelections.length <= 0 ) {
                  firstPhenotypeSearchComboBox.setValue( '' );
                  firstPhenotypeSearchComboBox.reset();
                  firstPhenotypeSearchComboBox.clearInvalid();
               } else {
                  firstPhenotypeSearchComboBox.selectPhenotype( phenotypeSelections[0] );

                  for (var i = 1; i < phenotypeSelections.length; i++) {
                     rowsPanel.add( createRowPanel( phenotypeSelections[i] ) );
                  }
                  rowsPanel.doLayout();
               }
            },
            getSelectedPhenotypes : function() {
               var selectedPhenotypes = errorMessages.length === 0 ? [] : null;

               for (var i = 0; selectedPhenotypes != null && i < rowsPanel.items.length; i++) {
                  var currPhenotypeSearchComboBox = rowsPanel.items.itemAt( i ).getPhenotypeSearchComboBox();

                  if ( currPhenotypeSearchComboBox.getValue() === '' ) {
                     selectedPhenotypes = null;
                  } else {
                     selectedPhenotypes.push( currPhenotypeSearchComboBox.getSelectedPhenotype() );
                  }
               }

               return selectedPhenotypes;
            },
            setCurrentGeneNcbiId : function( newCurrentGeneNcbiId ) {
               currentGeneNcbiId = newCurrentGeneNcbiId;

               for (var i = 0; i < rowsPanel.items.length; ++i) {
                  var currPhenotypeSearchComboBox = rowsPanel.items.itemAt( i ).getPhenotypeSearchComboBox();
                  currPhenotypeSearchComboBox.currentGeneNcbiId = currentGeneNcbiId;
               }
            },
            items : [ rowsPanel, {
               border : false,
               layout : 'hbox',
               hideLabel : true,
               items : [ addPhenotypeRowButton, {
                  xtype : 'displayfield',
                  value : 'To describe a complex phenotype, you can add more terms by clicking this button.',
                  style : 'color: grey;',
                  margins : '4 0 0 4'
               } ]
            } ]
         } );
      Gemma.PhenotypeAssociationForm.PhenotypesSearchPanel.superclass.initComponent.call( this );
   }
} );
