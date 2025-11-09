<%@ include file="/WEB-INF/common/taglibs.jsp" %>

<head>
<title>Manage Search Indexes</title>
<Gemma:script src='/scripts/app/indexer.js' />
</head>

<body>
<div class="padded">
    <h2>Manage Search Indexes</h2>
    <security:authorize access="hasAuthority('GROUP_ADMIN')">
        <p>
            Choose the indexing options that are appropriate and then click
            index.
        </p>
        <div id="index-form"></div>
        <div id="messages" style="margin: 10px; width: 400px"></div>
        <div id="taskId" style="display: none;"></div>
        <div id="progress-area" style="padding: 5px;"></div>
        <br />
    </security:authorize>
</div>
</body>



