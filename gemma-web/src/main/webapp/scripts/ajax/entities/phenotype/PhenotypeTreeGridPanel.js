/**
 * It displays all the available phenotypes in the system.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypeTreeGridPanel = Ext.extend(Ext.ux.maximgb.tg.GridPanel, {
	storeAutoLoad: false,
	title: "Phenotypes Tree",
    autoScroll: true,
    stripeRows: true,
	loadMask: true,
	master_column_id: 'value', // for using maximgb's GridPanel
	autoExpandColumn: 'value', // for using maximgb's GridPanel
    viewConfig: {
        forceFit: true
    },
    initComponent: function() {
		var DISABLED_CLASS = 'x-item-disabled';
	 	var PHENOTYPE_COLUMN_INDEX = 1;
	 	
		var currentSearchedCellRowIndex = -1;
				
		var setDescendantRowsLookSelected = function(parentRecord, isLookSelected) {
			var childrenRecords = this.getStore().getNodeChildren(parentRecord);
			var view = this.getView();
		
			for (var i = 0; i < childrenRecords.length; i++) {
				if (isLookSelected) {
					if (this.getSelectionModel().isSelected(this.getStore().indexOf(childrenRecords[i]))) {
						this.getSelectionModel().deselectRow(this.getStore().indexOf(childrenRecords[i]));
					}
		
					view.addRowClass(this.getStore().indexOf(childrenRecords[i]), DISABLED_CLASS);
					view.addRowClass(this.getStore().indexOf(childrenRecords[i]), view.selectedRowClass);
				} else {
					view.removeRowClass(this.getStore().indexOf(childrenRecords[i]), DISABLED_CLASS);
					view.removeRowClass(this.getStore().indexOf(childrenRecords[i]), view.selectedRowClass);
				} 
				
				if (this.getStore().hasChildNodes(childrenRecords[i])) {
					setDescendantRowsLookSelected(childrenRecords[i], isLookSelected);
				}
			}
		}.createDelegate(this);

		var checkboxSelectionModel = new Ext.grid.CheckboxSelectionModel({
			singleSelect: false,
			header: '', // remove the "select all" checkbox on the header 
		    listeners: {
				beforerowselect : function (selectionModel, rowIndex, keep, record) {
					var ancestors = this.getStore().getNodeAncestors(record);
					var isAncestorSelected = false;
					for (var i = 0; !isAncestorSelected && i < ancestors.length; i++) {
						isAncestorSelected = selectionModel.isSelected(ancestors[i]); 
					}
					
					// If ancestor is selected, don't allow children to be selected.
					return !isAncestorSelected;
				},
				rowdeselect: function(selectionModel, rowIndex, record) {
					setDescendantRowsLookSelected(record, false);
				},
				rowselect: function(selectionModel, rowIndex, record) {
					setDescendantRowsLookSelected(record, true);
				},
				scope: this
		    }
		});

		var phenotypeSearchComboBox = new Ext.form.ComboBox({
			allowBlank: true,
			editable: true,
			forceSelection: true,				
			mode: 'local',
			store: new Ext.data.JsonStore({
				fields: [
						{ name: 'value', sortType: Ext.data.SortTypes.asUCString }, // case-insensitively
						'urlId'
					],
				idProperty: 'urlId',
				sortInfo: {	field: 'value', direction: 'ASC' }
			}),
		    valueField: 'urlId',
		    displayField: 'value',
		    width: 200,
		    hideTrigger: true,
		    typeAhead: false,
		    emptyText: 'Search Phenotypes',
		    triggerAction: 'all',
			selectOnFocus:true
		});
		
		phenotypeSearchComboBox.on({
			select: function(comboBox, record, index) {
				applyPhenotypeSearch();
			},
			scope: this
		});

		var applyPhenotypeSearch = function() {
				if (currentSearchedCellRowIndex >= 0) {
					this.getView().onCellDeselect(currentSearchedCellRowIndex, PHENOTYPE_COLUMN_INDEX);
				}
				currentSearchedCellRowIndex = this.getStore().findExact('urlId', phenotypeSearchComboBox.getValue(), 0);
				if (currentSearchedCellRowIndex >= 0) {
					this.getView().onCellSelect(currentSearchedCellRowIndex, PHENOTYPE_COLUMN_INDEX);
					this.getView().ensureVisible(currentSearchedCellRowIndex, PHENOTYPE_COLUMN_INDEX, false); // false for hscroll
				}
		}.createDelegate(this);

		var commonConfig = new Gemma.PhenotypeGridPanelCommonConfig();

		Ext.apply(this, {
			store: new Ext.ux.maximgb.tg.AdjacencyListStore({
				proxy: commonConfig.getStoreProxy(this.phenotypeStoreProxy),
baseParams: commonConfig.getBaseParams(), 
				reader: commonConfig.getStoreReader(),					
				autoLoad: this.storeAutoLoad}),
			columns:[
				checkboxSelectionModel,
				commonConfig.getPhenotypeValueColumn({ id: 'value' }), // for using maximgb's GridPanel
				commonConfig.getGeneCountColumn()
			],
		    sm: checkboxSelectionModel,
		    listeners: {
				hide: commonConfig.getHideHandler,
				cellclick: commonConfig.getCellClickHandler
		    },
			tbar: [
				phenotypeSearchComboBox,		
				commonConfig.getAddNewPhenotypeAssociationButton(this),
				{
					handler: function() {
						this.loadMask.show();
						
						// Defer the call. Otherwise, the loading mask does not show.
						Ext.defer(
							function() {
								this.getStore().collapseAll();
							},
							10,
							this);
						
					},
					scope: this,
					icon: "/Gemma/images/icons/details_hidden.gif",
					tooltip: "Collapse all"
				},
				{
					handler: function() {
						this.loadMask.show();
						
						// Defer the call. Otherwise, the loading mask does not show.
						Ext.defer(
							function() {
								this.getStore().expandAll();
							},
							10,
							this);
					},
					scope: this,
					icon: "/Gemma/images/icons/details.gif",
					tooltip: "Expand all"
				}
			]
		});

		this.superclass().initComponent.call(this);

		this.getStore().on('load', 
			function(store, records, options) {
				commonConfig.resetSelectionConfig();

				var phenotypeSearchComboBoxData = [];
				for (var i = 0; i < records.length; i++) {
					phenotypeSearchComboBoxData.push({
						value: records[i].data.value,
						urlId: records[i].data.urlId
					});
				}
				phenotypeSearchComboBox.getStore().loadData(phenotypeSearchComboBoxData);
				
				if (phenotypeSearchComboBox.getValue() !== '') {
					currentSearchedCellRowIndex = -1;
					
					applyPhenotypeSearch();
					
					// If previously selected cell cannot be found, we assume that
					// the previously phenotype is private. So, clear the search field.
					if (currentSearchedCellRowIndex < 0) {
						phenotypeSearchComboBox.setValue('');
					}
				}
			},
			this, // scope
			{
				delay: 500 // Delay the handler. Otherwise, the current record is selected but not viewable in FireFox as of 2012-02-01 if it is not in the first page of the grid. There is no such issue in Chrome.
			}
		);
		
		// Hide the loadMask which is shown when collapse all or expand all is executed. 
		this.getStore().on('datachanged',
			function(store, rc) {
				this.loadMask.hide();
			},
			this
		);
	}
});
