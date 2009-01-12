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

	var store = new Ext.data.Store({
		proxy : new Ext.data.DWRProxy(UserListController.getUsers),
		reader : new Ext.data.ListRangeReader({
			id : 'id'
		}, Ext.data.Record.create([{
			name : "id",
			type : "int"
		}, {
			name : "userName",
			type : "string"
		}, {
			name : "lastName",
			type : "string"
		}, {
			name : "email",
			type : "string"
		}, {
			name : "enabled",
			type : "boolean"
		}])),
		remoteSort : false
	});
	store.load();

	var userGrid = new Ext.grid.GridPanel({
		renderTo : "userList",
		title : "User Management",
		frame : true,
		height : 300,
		width : 900,
		store : store,
		stripeRows : true,

		cm : new Ext.grid.ColumnModel([{
			header : "Username",
			dataIndex : 'userName',
			sortable : true
		}, {
			header : "Last Name",
			dataIndex : 'lastName',
			sortable : true
		}, {
			header : "Email",
			dataIndex : 'email'
		}, {
			header : "Account Enabled",
			dataIndex : 'enabled'
		}]),

		viewConfig : {
			forceFit : true
		},

		sm : new Ext.grid.RowSelectionModel({
			singleSelect : true
		})

	});
});