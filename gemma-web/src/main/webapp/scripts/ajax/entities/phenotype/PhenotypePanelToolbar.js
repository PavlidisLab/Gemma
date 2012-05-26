/**
 * This is the toolbar for the phenotype panel.
 * 
 * @author frances
 * @version $Id$
 */
Ext.namespace('Gemma');

Gemma.PhenotypePanelToolbar = Ext.extend(Ext.Toolbar, {
    initComponent: function() {
    	var DEFAULT_FILTERS_TITLE = 'Filters';
    	var MY_ANNOTATIONS_ONLY_TITLE = 'My annotations only';
    	var SPECIES_RADIO_GROUP_NAME = 'species';

		var currentFilters = {
			taxonCommonName: '',
			showOnlyEditable: false
		};			

    	var loggedIn = false;

    	if (!Gemma.isRunningOutsideOfGemma()) {
			var loggedInDom = Ext.getDom('loggedIn');
			loggedIn = (loggedInDom && loggedInDom.value === "true")

			Gemma.Application.currentUser.on("logIn", function() {
				loggedIn = true;
			});

			Gemma.Application.currentUser.on("logOut", function() {
				loggedIn = false;
			});
    	}

		var showOnlyEditableCheckbox = new Ext.form.Checkbox({
			checked: false, // should not be checked initially.
			fieldLabel: MY_ANNOTATIONS_ONLY_TITLE,
			listeners: {
				check: function(thisCheckbox, checked) {
					if (checked) {
						if (Gemma.isRunningOutsideOfGemma()) {
							Ext.Msg.alert(Gemma.HelpText.WidgetDefaults.PhenotypePanel.filterMyAnnotationsOutsideOfGemmaTitle,
								Gemma.HelpText.WidgetDefaults.PhenotypePanel.filterMyAnnotationsOutsideOfGemmaText);
						
							thisCheckbox.setValue(false);
						} else {
							SignupController.loginCheck({
				                callback: function(result) {
				                	if (!result.loggedIn) {
										Gemma.AjaxLogin.showLoginWindowFn();
								
										thisCheckbox.setValue(false);
										
										Gemma.Application.currentUser.on("logIn",
												function(userName, isAdmin) {	
													Ext.getBody().unmask();
												},
												this,
												{
													single: true
												});
					                	}
				                }
				            });
						}
					}
				}
			}
		});

		var speciesRadioGroup =  new Ext.form.RadioGroup({
			fieldLabel: 'Species',
			items: [
				{ boxLabel: 'All', inputValue: '', name: SPECIES_RADIO_GROUP_NAME, checked: true },
				{ boxLabel: 'Human', inputValue: 'human', name: SPECIES_RADIO_GROUP_NAME },
				{ boxLabel: 'Mouse', inputValue: 'mouse', name: SPECIES_RADIO_GROUP_NAME },
				{ boxLabel: 'Rat', inputValue: 'rat', name: SPECIES_RADIO_GROUP_NAME }
			]
		});

    	var menu = new Ext.menu.Menu({
    		listeners: {
    			hide: function(thisMenu) {
					showOnlyEditableCheckbox.suspendEvents();
					showOnlyEditableCheckbox.setValue(currentFilters.showOnlyEditable);
					showOnlyEditableCheckbox.resumeEvents();
					
					for (var i = 0; i < speciesRadioGroup.items.length; i++) {
						var currRadio = speciesRadioGroup.items.itemAt(i);
						
						if (currRadio.inputValue === currentFilters.taxonCommonName) {
							currRadio.suspendEvents();
							currRadio.setValue(true);
							currRadio.resumeEvents();
						}
					}
    			}
    		},
    		items: [
	            {
	            	xtype: 'form',
					autoHeight: true,
					width: 400,
					labelWidth: 120,
					items: [
						showOnlyEditableCheckbox,
						speciesRadioGroup
					],
					buttonAlign: 'right',
					buttons: [
						{
						    text: 'Apply',
						    formBind: true,
						    handler: function() {
								this.fireEvent('filterApplied', {
									showOnlyEditable: showOnlyEditableCheckbox.getValue(),
									taxonCommonName: speciesRadioGroup.getValue().inputValue
								});
								
								updateFiltersStatus();
								
						    	menu.hide();
						    },
							scope: this
						}
					]		
				}
    		]
    	});
    	
		var filterButton = new Ext.Button({
			text: DEFAULT_FILTERS_TITLE,
			menu: menu
		});
		
		var updateFiltersStatus = function() {
			currentFilters.taxonCommonName = speciesRadioGroup.getValue().inputValue;
			currentFilters.showOnlyEditable = showOnlyEditableCheckbox.getValue();
			
			var filtersApplied = '';
			if (showOnlyEditableCheckbox.getValue()) {
				filtersApplied += '<b>' + MY_ANNOTATIONS_ONLY_TITLE + '</b>';
			}
			if (speciesRadioGroup.getValue().inputValue !== '') {
				if (filtersApplied !== '') {
					filtersApplied += ' + '
				}
				filtersApplied += '<b>' + speciesRadioGroup.getValue().boxLabel + '</b>';
			}
			filterButton.setText((filtersApplied === '') ?
				DEFAULT_FILTERS_TITLE :
				DEFAULT_FILTERS_TITLE + ': ' + filtersApplied);
		}			

		Ext.apply(this, {
			setShowOnlyEditableCheckbox: function(status) {
				showOnlyEditableCheckbox.suspendEvents();
				showOnlyEditableCheckbox.setValue(status);
				showOnlyEditableCheckbox.resumeEvents();
				
				updateFiltersStatus();
			},
			items: [			
				filterButton
            ]
		});

		this.superclass().initComponent.call(this);
    }
});
