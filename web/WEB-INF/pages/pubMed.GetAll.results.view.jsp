<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page
	import="edu.columbia.gemma.common.description.BibliographicReference"
	import="edu.columbia.gemma.common.description.DatabaseEntry" 
	import="java.util.Calendar"
	%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<HEAD>
</HEAD>
<BODY>
<%--uncomment when using webflow <FORM name="newSearchForm" action="search.htm">--%>
<FORM name="newSearchForm" action="bibRefs.htm">
<%-- uncomment when using webflows 
    <input type="hidden" name="_flowExecutionId" value="<%=request.getAttribute("flowExecutionId") %>"> 
	<input type="hidden" name="_eventId" value="newSearch">
--%>	
</FORM>
<DIV align="left">
<P>
<TABLE width="100%">
	<TR>
		<TD>
		<DIV align="left"><b>Search Results</b></DIV>
		</TD>
	</TR>
	<TR>
		<TD>
		<HR>
		</TD>
	</TR>
	<TR>
	<display:table name="bibliographicReferences" class="list" requestURI="" id="bibliographicReferenceList" export="true">		
		<display:column property="pubAccession.accession" sort="true" href="bibRefDetails.htm" paramId="pubMedId" paramProperty="pubAccession.accession" titleKey="pubMed.id"/>
		<display:column property="title" sort="true" titleKey="pubMed.title"/>
		<display:column property="publication" sort="true" titleKey="pubMed.publication"/>
		<display:column property="authorList" sort="true" titleKey="pubMed.authors"/>
		<display:column property="publicationDate" sort="true" titleKey="pubMed.year"/>
		<display:column property="volume" sort="true" titleKey="pubMed.volume"/>		
		<display:column property="pages" sort="true" titleKey="pubMed.pages"/>
		<display:setProperty name="basic.empty.showtable" value="true"/>
	</display:table>	
	</TR>
	<TR>
		<TD>
		<HR>
		</TD>
	</TR>
	<TR>
		<TD>
		<DIV align="right"><INPUT type="button"
			onclick="javascript:document.newSearchForm.submit()"
			value="New Search"></DIV>
		</TD>
	</TR>
</TABLE>
</P>
</DIV>
</BODY>
</HTML>
