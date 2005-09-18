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
		<TD>
		<TABLE BORDER="1">
			<TR>
				<TD><B>PubMed ID</B></TD>
				<td><b>Title</b></td>
				<TD><B>Publication</B></TD>
				<TD><B>Authors</B></TD>
				<TD><B>Year</B></TD>
				<td><b>Volume</b></td>
				<td><b>Pages</b></td>
				
			</TR>
			<%
        Collection results = ( Collection ) request.getAttribute( "bibliographicReferences" );
        Iterator iter = results.iterator();
        while ( iter.hasNext() ) {
            BibliographicReference bibliographicReference = ( BibliographicReference ) iter.next();
            DatabaseEntry databaseEntry = bibliographicReference.getPubAccession();
            String pubmedId= "?";
            if (databaseEntry != null) { 
            pubmedId = databaseEntry.getAccession();
            }  
            Calendar c = Calendar.getInstance();
          	c.setTime( bibliographicReference.getPublicationDate());
            %>
			<TR>
				<%-- uncomment this if you are using the webflow version<TD><A
					href="search.htm?_flowExecutionId=<%=request.getAttribute("flowExecutionId") %>&_flowId=pubMed.Detail&_eventId=select&pubMedId=<%=pubmedId %>">
				<%=pubmedId %> </A></TD>--%>
				<TD><A
					href="bibRefDetails.htm?pubMedId=<%=pubmedId %>">
				<%=pubmedId %> </A></TD>
			 	<td><%=bibliographicReference.getTitle() %></td>
				<TD><%=bibliographicReference.getPublication() %></TD>
				<TD><%=bibliographicReference.getAuthorList() %></TD>
				<TD><%=c.get(Calendar.YEAR) %></TD>
				<td><%=bibliographicReference.getVolume() %></td>
				<td><%=bibliographicReference.getPages() %></td>
			</TR>
			<%
        }
    %>
		</TABLE>
		</TD>	
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
