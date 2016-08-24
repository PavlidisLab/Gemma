<%@ include file="/common/taglibs.jsp"%>
<%@ page language="java" isErrorPage="true"%>
<%-- $Id$ --%>

<%-- This is not being used. --%>

		<title>Invalid GEO accession</title>

	<body>
		<content tag="heading">
		Invalid GEO accession
		</content>

		<a href="home.html" onclick="history.back();return false">&#171; Back</a>

		<Gemma:exception exception="${requestScope.exception}" showStackTrace="false" />

		<p>
			Either that accession does not exist in GEO, or if you entered a GDS number, Gemma could not locate a matching series
			(GSE) on the GEO web site. Please check the
			<a href="http://www.ncbi.nlm.nih.gov/geo/">GEO web site</a> to make sure you selected a valid accession.
		</p>

