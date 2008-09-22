<%@ include file="/common/taglibs.jsp"%>
<head>

	<title>Expression data upload</title>

	<jwr:script src='/scripts/ajax/util/FileUploadForm.js' useRandomParam="false" />
	<jwr:script src='/scripts/app/UserExpressionDataUpload.js' useRandomParam="false" />

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

	<div id="messages"></div>
	<div id="form"></div>
	<div id="progress-area" style="margin: 20px; padding: 5px;"></div>

</body>
