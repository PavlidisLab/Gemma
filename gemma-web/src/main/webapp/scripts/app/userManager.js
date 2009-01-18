/**
 * User management tool.
 * 
 * @author keshav
 * @version $Id$
 */
Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.onReady(function() {
	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	if (Ext.isIE && !Ext.isIE7) {
		Ext.DomHelper.append('errorMessage', {
			tag : 'p',
			cls : 'trouble',
			html : 'This page may display improperly in older versions of Internet Explorer. Please upgrade to Internet Explorer 7 or newer.'
		});
	}

	// A simple test store using canned "data" and an array "reader".
	// var store = new Ext.data.Store({
	// data : [[1, "mjudge", "Judge", "foo@foo.com", true],
	// [2, "mjudge", "Judge", "foo@foo.com", true],
	// [3, "mjudge", "Judge", "foo@foo.com", true],
	// [4, "mjudge", "Judge", "foo@foo.com", false]],
	//
	// reader : new Ext.data.ArrayReader({
	// id : 'id'
	// }, ['id', 'userName', 'lastName', 'email', 'enabled'])
	//
	// });

	var record = Ext.data.Record.create([{
		name : "id",
		type : "int"
	}, {
		name : "userName",
		type : "string"
	}, {
		name : "email",
		type : "string"
	}, {
		name : "role",
		type : "string"
	}, {
		name : "enabled",
		type : "boolean"
	}]);

	var store = new Ext.data.Store({
		proxy : new Ext.data.DWRProxy(UserListController.getUsers),
		reader : new Ext.data.ListRangeReader({
			id : 'id'
		}, record),
		remoteSort : false
	});

	/* create editors for the grid */
	var emailEdit = new Ext.form.TextField({
		vtype : 'email'
	});

	var checkColumn = new Ext.grid.CheckColumn({
		header : "Enabled?",
		dataIndex : 'enabled',
		width : 55
	});

	// the combo box editor needs a store
	var possibleRoles = new Ext.data.Store({
		data : [[1, "user"], [2, "admin"]],

		reader : new Ext.data.ArrayReader({
			id : 'id'
		}, ['id', 'role'])

	});

	var roleEdit = new Ext.form.ComboBox({
		typeAhead : true,
		lazyRender : true,
		triggerAction : 'all',
		mode : 'local',
		store : possibleRoles,
		displayField : 'role',
		valueField : 'id'
	});

	var userGrid = new Ext.grid.EditorGridPanel({
		renderTo : "userList",
		title : "User Management",
		frame : true,
		height : 300,
		width : 900,
		store : store,
		stripeRows : true,
		clicksToEdit : 1,
		plugins : checkColumn,
		loadMask : true,
		autoScroll : true,

		cm : new Ext.grid.ColumnModel([{
			header : "Username",
			dataIndex : 'userName',
			sortable : true
		}, {
			header : "Email",
			dataIndex : 'email',
			editor : emailEdit
		}, {
			header : "Role",
			dataIndex : 'role',
			editor : roleEdit
		}, checkColumn]),

		viewConfig : {
			forceFit : true
		},

		sm : new Ext.grid.RowSelectionModel({
			singleSelect : true
		})
	});
	/* load the data store with values from the server */
	store.load();

});