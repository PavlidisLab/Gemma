/**
 * If the current user is admin, it displays all evidence owned by all users. Otherwise, it displays evidence owned by
 * the current user.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeEvidenceManagerGridPanel = Ext.extend(Gemma.PhenotypeEvidenceGridPanel, {
      storeAutoLoad : true,
      storeSortInfo : {
         field : 'lastUpdated',
         direction : 'DESC'
      },
      displayPhenotypeAsLink : true,
      // Set it to false so that we can create evidence without specifying currentGene in the parent class.
      allowCreateOnlyWhenGeneSpecified : false,
      title : 'Phenotype Association Manager',
      hasRelevanceColumn : false,
      extraColumns : [{
         startIndex : 2,
         columns : [{
            header : 'Gene',
            dataIndex : 'geneOfficialSymbol',
            width : 0.15,
            renderer : function(value, metadata, record, row, col, ds) {
               var geneLink = '/Gemma/gene/showGene.html?id=' + record.data.geneId;

               return String.format("{0} <a target='_blank' href='" + geneLink
                     + "' ext:qtip='Go to {0} Details (in new window)'><img src='/Gemma/images/icons/magnifier.png' height='10' width='10'/></a> ", value);
            },
            sortable : true
         }, {
            header : 'Taxon',
            dataIndex : 'taxonCommonName',
            width : 0.15,
            sortable : true
         }]
      }],
      initComponent : function() {
         var DEFAULT_TAXON_ID = '-1';
         var DEFAULT_LIMIT = 50;
         var DEFAULT_USERNAME = 'All users';

         this.viewConfig.emptyText = 'You have not created any phenotype association yet.';

         Ext.apply(this, {
               evidenceStoreProxy : new Ext.data.DWRProxy({
                     apiActionToHandlerMap : {
                        read : {
                           dwrFunction : PhenotypeController.findEvidenceByFilters,
                           getDwrArgsFunction : function(request) {
                              return [request.params.taxonId === DEFAULT_TAXON_ID ? null : request.params.taxonId, request.params.limit == null ? // It
                                                                                                                                                   // is
                                                                                                                                                   // undefined
                                                                                                                                                   // when
                                                                                                                                                   // the
                                                                                                                                                   // widget
                                                                                                                                                   // is
                                                                                                                                                   // first
                                                                                                                                                   // loaded.
                                    DEFAULT_LIMIT
                                    : request.params.limit, request.params.userName === DEFAULT_USERNAME ? null : request.params.userName];
                           }
                        }
                     }
                  }),
               listeners : {
                  phenotypeAssociationChanged : function(phenotypes, gene) {
                     reloadStore();
                  }
               }
            });

         var taxonCombo = new Gemma.TaxonCombo({
               isDisplayTaxonWithEvidence : true,
               stateId : null, // don't remember taxon value if user navigates away then comes back
               emptyText : "Filter by taxon",
               allTaxa : true, // want an 'All taxa' option
               value : DEFAULT_TAXON_ID,
               listeners : {
                  select : function(combo, record, index) {
                     reloadStore();
                  }
               }
            });
         taxonCombo.getStore().on('doneLoading', function() {
               // I have to do this. Otherwise, the combo box will display DEFAULT_TAXON_ID.
               taxonCombo.setValue(taxonCombo.getValue());
            });

         var dataFilterCombo = new Gemma.DataFilterCombo({
               value : DEFAULT_LIMIT,
               listeners : {
                  select : function(combo, record, index) {
                     reloadStore();
                  }
               }
            });

         var reloadStore = function() {
            this.getStore().reload({
                  params : {
                     taxonId : taxonCombo.getValue(),
                     limit : dataFilterCombo.getValue(),
                     userName : userNameCombo == null ? null : userNameCombo.getValue()
                  }
               });
         }.createDelegate(this);

         Gemma.PhenotypeEvidenceManagerGridPanel.superclass.initComponent.call(this);

         this.getTopToolbar().addButton(taxonCombo);
         this.getTopToolbar().addButton(dataFilterCombo);

         if (this.hasAdminLoggedIn()) {
            var userNameRecord = Ext.data.Record.create([{
                  name : "userName",
                  type : "string"
               }]);
            var userNameCombo = new Ext.form.ComboBox({
                  editable : false,
                  width : 150,
                  triggerAction : 'all',
                  lastQuery : '', // To make sure the filter in the store is not cleared the first time the ComboBox
                                    // trigger is used.
                  store : new Ext.data.Store({
                        proxy : new Ext.data.DWRProxy(PhenotypeController.findEvidenceOwners),
                        reader : new Ext.data.ListRangeReader({}, userNameRecord),
                        autoLoad : true
                     }),
                  valueField : 'userName',
                  displayField : 'userName',
                  value : DEFAULT_USERNAME,
                  listeners : {
                     select : function(combo, record, index) {
                        reloadStore();
                     }
                  }
               });
            userNameCombo.getStore().on('load', function(store, records, options) {
                  store.insert(0, [new userNameRecord({
                           'userName' : DEFAULT_USERNAME
                        })]);
               });

            this.getTopToolbar().addButton(userNameCombo);
         }
      }
   });
