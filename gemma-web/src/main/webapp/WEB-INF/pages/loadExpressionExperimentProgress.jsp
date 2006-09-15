<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="command" scope="request"
   class="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentLoadCommand" />


<html>
    <head>
<title><fmt:message key="expressionExperiment.load.title" /></title>
<content tag="heading">
<fmt:message key="expressionExperiment.load.title" />
</content>



        <script type='text/javascript' src='dwr/engine.js'></script>
        <script type='text/javascript' src='dwr/util.js'></script>
        <script type='text/javascript' src='dwr/interface/ProgressMonitor'></script>
        <script type='text/javascript' src="<c:url value="scripts/progress.js"/>"></script>
        <style type="text/css">

</style>
    </head>
    <body>




	
<script type="text/javascript">
	createProgressBar();
	startProgress();
</script>
  </body>
</html>




