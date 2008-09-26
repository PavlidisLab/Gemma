<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>File upload demo</title>

	<jwr:script src='/scripts/ajax/util/FileUploadForm.js' useRandomParam="false" />

	<style>
.upload-icon {
	background: url('images/icons/add.png') no-repeat 0 0 !important;
}

#fi-button-msg {
	border: 2px solid #ccc;
	padding: 5px 10px;
	background: #eee;
	margin: 5px;
	float: left;
}
</style>

</head>


<body>

	<script>
	
	Ext.namespace('Gemma');
	Ext.form.Field.prototype.msgTarget = 'side';
	Ext.BLANK_IMAGE_URL = "/Gemma/images/s.gif";

	 
	Ext.onReady( function()
	 {
	 	var form = new Gemma.FileUploadForm({renderTo : 'fi-form'});
	 });	
		
		</script>

	<div id="messages"></div>
	<div id="fi-form"></div>
	<div id="progress-area" style="margin: 20px; padding: 5px;"></div>

</body>
