<%@ page contentType="text/html; charset=iso-8859-1" errorPage="" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="edu.columbia.gemma.expression.experiment.ExpressionExperiment" %>
<%@ page import="edu.columbia.gemma.common.auditAndSecurity.Person" %>
<%@ page import="edu.columbia.gemma.common.description.OntologyEntry" %>
<%@ page import="edu.columbia.gemma.expression.experiment.ExperimentalDesign" %>
<%@ page import="edu.columbia.gemma.expression.experiment.ExperimentalFactor" %>
<%@ page import="edu.columbia.gemma.expression.experiment.FactorValue" %>
<html>
<head>
	<title>Experiment Detail</title>
	<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body bgcolor="#ffffff">

<content tag="heading">Experiment Detail</content>
<jsp:getProperty name="expressionExperiment" property="name"/>
<jsp:getProperty name="expressionExperiment" property="provider"/>

</body>
</html>