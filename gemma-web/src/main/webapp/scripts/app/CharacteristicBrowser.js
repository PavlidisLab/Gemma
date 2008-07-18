Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
Ext.namespace('Gemma.CharacteristicBrowser');
Ext.onReady(function() {
	Ext.QuickTips.init();

	Gemma.CharacteristicBrowser.grid = new Gemma.AnnotationGrid({
		renderTo : "characteristicBrowser",
		readMethod : CharacteristicBrowserController.findCharacteristics,
		readParams : [],
		editable : true,
		tbar : new Ext.Toolbar(),
		useDefaultToolbar : false,
		showParent : true,
		width : 1200,
		noInitialLoad : true,
		pageSize : 30
	});

	Gemma.CharacteristicBrowser.handleError = function(msg, e) {
		Ext.DomHelper.overwrite("messages", {
			tag : 'img',
			src : '/Gemma/images/icons/warning.png'
		});
		Ext.DomHelper.append("messages", {
			tag : 'span',
			html : "&nbsp;&nbsp;" + msg
		});
		Gemma.CharacteristicBrowser.grid.loadMask.hide();
		saveButton.enable();
	};

	var charCombo = new Gemma.CharacteristicCombo();

	var searchButton = new Ext.Toolbar.Button({
		text : "search",
		tooltip : "Find matching characteristics in the database",
		handler : function() {
			Ext.DomHelper.overwrite("messages", "");
			var query = charCombo.getCharacteristic().value;
			if (!query) {
				Ext.DomHelper.overwrite("messages", "Please enter a query");
				return;
			}
			Gemma.CharacteristicBrowser.grid.loadMask.msg = "Searching ...";
			var searchEEs = eeCheckBox.getValue();
			var searchBMs = bmCheckBox.getValue();
			var searchFVs = fvCheckBox.getValue();
			var searchNos = noCheckBox.getValue();
			Gemma.CharacteristicBrowser.grid.refresh([query, searchNos, searchEEs, searchBMs, searchFVs]);
		}
	});

	var saveButton = new Ext.Toolbar.Button({
		text : "save",
		tooltip : "Saves your changes to the database",
		disabled : true,
		handler : function() {
			saveButton.disable();
			Gemma.CharacteristicBrowser.grid.loadMask.msg = "Saving ...";
			Gemma.CharacteristicBrowser.grid.loadMask.show();
			Ext.DomHelper.overwrite("messages", "");
			var chars = Gemma.CharacteristicBrowser.grid.getEditedCharacteristics();
			var callback = Gemma.CharacteristicBrowser.grid.refresh.bind(Gemma.CharacteristicBrowser.grid);
			var errorHandler = Gemma.CharacteristicBrowser.handleError.createDelegate(this, [], true);
			CharacteristicBrowserController.updateCharacteristics(chars, {
				callback : callback,
				errorHandler : errorHandler
			});
		}
	});

	Gemma.CharacteristicBrowser.grid.on("afteredit", function(e) {
		saveButton.enable();
	});

	var deleteButton = new Ext.Toolbar.Button({
		text : "delete",
		tooltip : "Delete selected characteristics",
		disabled : true,
		handler : function() {
			Ext.DomHelper.overwrite("messages", "");
			Gemma.CharacteristicBrowser.grid.loadMask.msg = "Deleting ...";
			Gemma.CharacteristicBrowser.grid.loadMask.show();
			var chars = Gemma.CharacteristicBrowser.grid.getSelectedCharacteristics();
			CharacteristicBrowserController.removeCharacteristics(chars);

			/*
			 * remove the records from the data store manually instead of just refreshing so that we don't lose any
			 * edits that are in progress...
			 */
			var selected = Gemma.CharacteristicBrowser.grid.getSelectionModel().getSelections();
			for (var i = 0; i < selected.length; ++i) {
				Gemma.CharacteristicBrowser.grid.getStore().remove(selected[i]);
			}
			Gemma.CharacteristicBrowser.grid.getView().refresh();

			// var callback = Gemma.CharacteristicBrowser.grid.refresh.bind(
			// Gemma.CharacteristicBrowser.grid );
			// CharacteristicBrowserController.removeCharacteristics( chars,
			// callback );
		}
	});
	Gemma.CharacteristicBrowser.grid.getSelectionModel().on("selectionchange", function(model) {
		var selected = model.getSelections();
		Ext.DomHelper.overwrite("messages", "");
		if (selected.length > 0) {
			deleteButton.enable();
		} else {
			deleteButton.disable();
		}
	});

	var revertButton = new Ext.Toolbar.Button({
		text : "revert",
		tooltip : "Undo changes to selected characteristics",
		disabled : true,
		handler : function() {
			var selected = Gemma.CharacteristicBrowser.grid.getSelectionModel().getSelections();
			for (var i = 0; i < selected.length; ++i) {
				var record = selected[i];
				record.reject();
			}
			Gemma.CharacteristicBrowser.grid.getView().refresh();
		}
	});
	Gemma.CharacteristicBrowser.grid.getSelectionModel().on("selectionchange", function(model) {
		var selected = model.getSelections();
		revertButton.disable();
		for (var i = 0; i < selected.length; ++i) {
			if (selected[i].dirty) {
				revertButton.enable();
				break;
			}
		}
	});
	Gemma.CharacteristicBrowser.grid.on("afteredit", function(e) {
		revertButton.enable();
	});

	var savedCharacteristic;
	var copyHandler = function() {
		var selected = Gemma.CharacteristicBrowser.grid.getSelectionModel().getSelections();
		for (var i = 0; i < selected.length; ++i) {
			var record = selected[i];
			savedCharacteristic = record.data;
			break;
		}
		pasteButton.enable();
		pasteCategoryButton.enable();
	};

	var copyButton = new Ext.Toolbar.Button({
		text : "copy",
		tooltip : "Copy values from the selected characteristic",
		disabled : true,
		handler : copyHandler
	});

	Gemma.CharacteristicBrowser.grid.getSelectionModel().on("selectionchange", function(model) {
		var selected = model.getSelections();
		if (selected.length > 0) {
			copyButton.enable();
		} else {
			copyButton.disable();
		}
	});

	var pasteHandler = function() {
		var selected = Gemma.CharacteristicBrowser.grid.getSelectionModel().getSelections();
		for (var i = 0; i < selected.length; ++i) {
			var record = selected[i];
			record.set("classUri", savedCharacteristic.classUri);
			record.set("className", savedCharacteristic.className);
			record.set("termUri", savedCharacteristic.termUri);
			record.set("termName", savedCharacteristic.termName);
		}
		Gemma.CharacteristicBrowser.grid.getView().refresh();
		saveButton.enable();
	};

	var pasteCategoryHandler = function() {
		var selected = Gemma.CharacteristicBrowser.grid.getSelectionModel().getSelections();
		for (var i = 0; i < selected.length; ++i) {
			var record = selected[i];
			record.set("classUri", savedCharacteristic.classUri);
			record.set("className", savedCharacteristic.className);
		}
		Gemma.CharacteristicBrowser.grid.getView().refresh();
		saveButton.enable();
	};

	var toggleDetails = function(btn, e) {
		var view = Gemma.CharacteristicBrowser.grid.getView();
		view.showDetails = btn.pressed;
		view.refresh();
	};

	var pasteButton = new Ext.Toolbar.Button({
		text : "paste",
		tooltip : "Paste copied values onto the selected characteristics; both Class and Term will be updated.",
		disabled : true,
		handler : pasteHandler
	});

	var pasteCategoryButton = new Ext.Toolbar.Button({
		text : "paste category",
		tooltip : "Paste copied Class values onto the selected characteristics. Term will be left alone.",
		disabled : true,
		handler : pasteCategoryHandler
	});

	var toggleDetailsButton = new Ext.Toolbar.Button({
		text : "Details",
		enableToggle : true,
		tooltip : "Show/hide more information",
		disabled : false,
		handler : toggleDetails
	});

	Gemma.CharacteristicBrowser.grid.on("keypress", function(e) {
		if (e.ctrlKey) {
			if (e.getCharCode() == 99) { // 'c'
				copyHandler();
			} else if (e.getCharCode() == 118) { // 'v'
				pasteHandler();
			}
		}
	});

	var eeCheckBox = new Ext.form.Checkbox({
		boxLabel : 'Expression Experiments',
		checked : true,
		name : 'searchEEs',
		width : 'auto'
	});
	var bmCheckBox = new Ext.form.Checkbox({
		boxLabel : 'BioMaterials',
		checked : true,
		name : 'searchBMs',
		width : 'auto'
	});
	var fvCheckBox = new Ext.form.Checkbox({
		boxLabel : 'Factor Values',
		checked : true,
		name : 'searchFVs',
		width : 'auto'
	});
	var noCheckBox = new Ext.form.Checkbox({
		boxLabel : 'No parent',
		checked : true,
		name : 'searchNos',
		width : 'auto'
	});

	var toolbar = Gemma.CharacteristicBrowser.grid.getTopToolbar();

	toolbar.addField(charCombo);
	toolbar.addSpacer();
	toolbar.addField(searchButton);
	toolbar.addSeparator();
	toolbar.addField(saveButton);
	toolbar.addSeparator();
	toolbar.addField(deleteButton);
	toolbar.addSeparator();
	toolbar.addField(revertButton);
	toolbar.addSeparator();
	toolbar.addField(copyButton);
	toolbar.addSeparator();
	toolbar.addField(pasteButton);
	toolbar.addSeparator();
	toolbar.addField(pasteCategoryButton);
	toolbar.addFill();
	toolbar.add(toggleDetailsButton);

	/*
	 * toolbar.addSeparator(); toolbar.addField( testButton );
	 */

	var secondToolbar = new Ext.Toolbar(toolbar.getEl().createChild());
	secondToolbar.addSpacer();
	secondToolbar.addText("Find characteristics from");
	secondToolbar.addSpacer();
	secondToolbar.addField(eeCheckBox);
	secondToolbar.addSpacer();
	secondToolbar.addField(bmCheckBox);
	secondToolbar.addSpacer();
	secondToolbar.addField(fvCheckBox);
	secondToolbar.addSpacer();
	secondToolbar.addField(noCheckBox);

});