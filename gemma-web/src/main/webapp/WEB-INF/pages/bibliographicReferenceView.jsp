<%@ include file="/common/taglibs.jsp"%>

<%-- $Id$ --%>
<%-- Shows the results of a search for pubmed references. --%>

<jsp:useBean id="bibliographicReference" scope="request"
	class="ubic.gemma.model.common.description.BibliographicReferenceImpl"></jsp:useBean>
<head>
	<title>pubMed.Search.results.view</title>
</head>


<table width="100%">
	<tr>
		<td colspan="2">
			<b>Bibliographic Reference Search Results</b>
		</td>
	</tr>
	<tr>
		<td colspan="2">
			<HR>
		</td>
	</tr>
	<tr>
		<td colspan="2">
			<Gemma:bibref bibliographicReference="${bibliographicReference}" />
		<td>
	</tr>
	<tr>
		<td colspan="2">
			<HR>
		</td>
	</tr>
	<tr>

		<td colspan="2">
			<table>
				<tr>
					<td align="left">
						<c:if test="${!requestScope.existsInSystem}">
							<div align="left">
								<input type="button" method="get"
									action="bibRefEdit.html?action=add&acc=${bibliographicReference.pubAccession.accession}"
									value="Add to Gemma Database">
							</div>
						</c:if>
					</td>
					<td>
						<c:if test="${requestScope.existsInSystem}">
							<authz:acl domainObject="${bibliographicReference}" hasPermission="1,6">
								<div align="right">
									<input type="button" method="get"
										action="bibRefEdit.html?action=delete&acc=${bibliographicReference.pubAccession.accession}"
										value="Delete from Gemma" />
								</div>
							</authz:acl>
						</c:if>
					</td>

					<td>
						<c:if test="${requestScope.existsInSystem}">
							<authz:acl domainObject="${bibliographicReference}" hasPermission="1,6">

								<div align="right">
									<input type="button" method="get"
										action="bibRefEdit.html?action=edit&acc=${bibliographicReference.pubAccession.accession}" value="Edit" />
								</div>

							</authz:acl>
						</c:if>
					</td>
					<td align="right">
						<div align="right">
							<input type="submit" method="get" action="/bibRefSearch.html" value="New Search">
						</div>
					</td>
				</tr>
			</table>
		</td>
	</tr>
</table>

