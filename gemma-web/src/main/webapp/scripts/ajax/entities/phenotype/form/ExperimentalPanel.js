/**
 * This experimental panel allows users to enter data for experimental evidence.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma.PhenotypeAssociationForm');

Gemma.PhenotypeAssociationForm.ExperimentalPanel = Ext.extend(Ext.Panel, {
      border : false,
      layout : 'form',
      hidden : true,
      autoHeight : true,
      defaultType : 'textfield',
      initComponent : function() {
         var isPrimaryPubMedIdValid = true;
         var isSecondaryPubMedIdValid = true;
         var pubMedIdErrorMessages = [];
         var experimentTagErrorMessages = [];

         // Specify if the given literaturePanel is valid.
         var updatePubMedIdsValidity = function(isModifying, literaturePanel, isValid) {
            var primaryPubMedId = primaryLiteraturePanel.getPubMedId();
            var secondaryPubMedId = secondaryLiteraturePanel.getPubMedId();

            pubMedIdErrorMessages.clear();

            if (literaturePanel === primaryLiteraturePanel) {
               isPrimaryPubMedIdValid = isValid;
            } else if (literaturePanel === secondaryLiteraturePanel) {
               isSecondaryPubMedIdValid = isValid;
            }

            if (primaryPubMedId === '') {
               if (secondaryPubMedId !== '') {
                  pubMedIdErrorMessages.push(String.format(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubMedIdOnlyPrimaryEmpty,
                     primaryLiteraturePanel.pubMedIdFieldLabel, secondaryLiteraturePanel.pubMedIdFieldLabel));
               }
            } else if (primaryPubMedId === secondaryPubMedId) {
               pubMedIdErrorMessages.push(String.format(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubMedIdsDuplicate,
                  primaryLiteraturePanel.pubMedIdFieldLabel, secondaryLiteraturePanel.pubMedIdFieldLabel));
            } else {
               if (primaryPubMedId !== '' && (primaryPubMedId <= 0 || !isPrimaryPubMedIdValid)) {
                  pubMedIdErrorMessages.push(String.format(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid,
                     primaryLiteraturePanel.pubMedIdFieldLabel));
               }
               if (secondaryPubMedId !== '' && (secondaryPubMedId <= 0 || !isSecondaryPubMedIdValid)) {
                  pubMedIdErrorMessages.push(String.format(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.pubmedIdInvalid,
                     secondaryLiteraturePanel.pubMedIdFieldLabel));
               }
            }

            this.fireEvent('validtyStatusChanged', isModifying, pubMedIdErrorMessages.concat(experimentTagErrorMessages));
         }.createDelegate(this);

         var updateExperimentTagsValidity = function(isModifying) {
            experimentTagErrorMessages.clear();

            if (experimentTagsPanel.checkDuplicate()) {
               experimentTagErrorMessages.push(Gemma.HelpText.WidgetDefaults.PhenotypeAssociationForm.ErrorMessage.experimentTagsDuplicate);
               this.fireEvent('validtyStatusChanged', isModifying, pubMedIdErrorMessages.concat(experimentTagErrorMessages));
            } else {
               this.fireEvent('validtyStatusChanged', isModifying, pubMedIdErrorMessages);
            }
         }.createDelegate(this);

         var pubMedIdFieldBlurHandler = function(literaturePanel) {
            var pubMedId = literaturePanel.getPubMedId();

            if (pubMedId === "") {
               updatePubMedIdsValidity(false, literaturePanel, true);
            } else if (pubMedId <= 0) {
               updatePubMedIdsValidity(false, literaturePanel, false);
            }
         };
         var pubMedIdFieldKeyUpHandler = function(literaturePanel, event) {
            updatePubMedIdsValidity(true, literaturePanel, true);
         };
         var pubMedIdStoreLoadHandler = function(literaturePanel, store, records, options) {
            // Because it takes time to reload the store, show errors
            // only when PudMed Id has not been changed
            if (options.params.pubMedId === literaturePanel.getPubMedId()) {
               if (store.getTotalCount() > 0) {
                  updatePubMedIdsValidity(false, literaturePanel, true);
               } else {
                  updatePubMedIdsValidity(false, literaturePanel, false);
               }
            }
         };

         var primaryLiteraturePanel = new Gemma.PhenotypeAssociationForm.LiteraturePanel({
               isErrorShown : false,
               pubMedIdFieldAllowBlank : true,
               pubMedIdFieldLabel : 'PubMed Id',
               listeners : {
                  pubMedIdFieldBlur : pubMedIdFieldBlurHandler,
                  pubMedIdFieldKeyUp : pubMedIdFieldKeyUpHandler,
                  pubMedIdStoreLoad : pubMedIdStoreLoadHandler
               }
            });

         var secondaryLiteraturePanel = new Gemma.PhenotypeAssociationForm.LiteraturePanel({
               isErrorShown : false,
               pubMedIdFieldAllowBlank : true,
               pubMedIdFieldLabel : 'Secondary PubMed Id',
               listeners : {
                  pubMedIdFieldBlur : pubMedIdFieldBlurHandler,
                  pubMedIdFieldKeyUp : pubMedIdFieldKeyUpHandler,
                  pubMedIdStoreLoad : pubMedIdStoreLoadHandler
               }
            });

         var experimentTagsPanel = new Gemma.PhenotypeAssociationForm.ExperimentTagsPanel({
               listeners : {
                  keyup : function(component) {
                     updateExperimentTagsValidity(true);
                  },
                  select : function(component) {
                     updateExperimentTagsValidity(false);
                  },
                  experimentTagFieldRemoved : function() {
                     updateExperimentTagsValidity(false);
                  }
               }
            });

         var setAllItemsVisible = function(container, isVisible) {
            for (var i = 0; i < container.items.length; i++) {
               container.items.items[i].setVisible(isVisible);
            }
         };

         Ext.apply(this, {
               setEvidenceId : function(evidenceId) {
                  primaryLiteraturePanel.setEvidenceId(evidenceId);
               },
               selectExperimentalData : function(primaryPubMedId, secondaryPubMedId, experimentTagSelections, geneSelection) {
                  primaryLiteraturePanel.setPubMedId(primaryPubMedId);
                  secondaryLiteraturePanel.setPubMedId(secondaryPubMedId);
                  experimentTagsPanel.selectExperimentTags(experimentTagSelections, geneSelection);
               },
               listeners : {
                  show : function(thisComponent) {
                     setAllItemsVisible(thisComponent, true);
                  },
                  hide : function(thisComponent) {
                     setAllItemsVisible(thisComponent, false);
                  },
                  scope : this
               },
               isValid : function() {
                  var primaryPubMedId = primaryLiteraturePanel.getPubMedId();
                  var secondaryPubMedId = secondaryLiteraturePanel.getPubMedId();

                  return primaryLiteraturePanel.isValid() && secondaryLiteraturePanel.isValid()
                     && ((primaryPubMedId !== secondaryPubMedId) || (primaryPubMedId === '' && secondaryPubMedId === '')) && (primaryPubMedId !== '' || secondaryPubMedId === '')
                     && experimentTagsPanel.isValid();
               },
               getValues : function() {
                  var secondaryCitationValueObject = secondaryLiteraturePanel.getCitationValueObject();

                  return {
                     primaryPublicationCitationValueObject : primaryLiteraturePanel.getCitationValueObject(),
                     relevantPublicationsCitationValueObject : secondaryLiteraturePanel.getCitationValueObject(),
                     relevantPublicationsCitationValueObjects : secondaryCitationValueObject == null ? [] : [secondaryCitationValueObject],
                     experimentCharacteristics : experimentTagsPanel.getSelectedExperimentTags()
                  };
               },
               setCurrentGeneTaxonId : function(newCurrentGeneTaxonId) {
                  experimentTagsPanel.setCurrentGeneTaxonId(newCurrentGeneTaxonId);
               },
               showAnnotationError : function(errorEvidenceIds, errorColor) {
                  primaryLiteraturePanel.showAnnotationError(errorEvidenceIds, errorColor);
               },
               items : [primaryLiteraturePanel, secondaryLiteraturePanel, experimentTagsPanel]
            });
         Gemma.PhenotypeAssociationForm.ExperimentalPanel.superclass.initComponent.call(this);
      }
   });