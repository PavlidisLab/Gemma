<%@ include file="/common/taglibs.jsp"%>
<%@ page import="java.util.*"%>
<%@ page
	import="edu.columbia.gemma.common.description.BibliographicReference"%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<HTML>
<script language="JavaScript">
	function selectAction(event){
		document.newSearchForm._eventId.value=event;
 		document.newSearchForm.submit();
	}
</script>
<HEAD></HEAD>
<BODY>
<FORM name="newSearchForm" action="pubMedSearch.htm"><INPUT
	type="hidden" name="_flowExecutionId"
	value="<%=request.getAttribute("flowExecutionId") %>"> <INPUT
	type="hidden" name="_eventId" value=""> <INPUT type="hidden"
	name="pubMedId" value="<%=request.getAttribute("pubMedId") %>"></FORM>
<TABLE width="100%">
	<TR>
		<TD><b>Bibliographic Reference</b></TD>
	</TR>
	<TR>
		<TD COLSPAN="2">
		<HR>
		</TD>
	</TR>
	<%
        List results = ( List ) request.getAttribute( "bibliographicReferences" );
        for ( int i = 0; i < results.size(); i++ ) {
            BibliographicReference bibliographicReference = ( BibliographicReference ) results.get( i );
            %>

	<TR>
		<TD><B>Author List</B></TD>
		<TD><%=bibliographicReference.getAuthorList() %></TD>
	</TR>
	<TR>
		<TD><B>Title</B></TD>
		<TD><%=bibliographicReference.getTitle() %></TD>
	</TR>
	<TR>
		<TD><B>Volume</B></TD>
		<TD><%=bibliographicReference.getVolume() %></TD>
	</TR>
	<TR>
		<TD><B>Issue</B></TD>
		<TD><%=bibliographicReference.getIssue() %></TD>
	</TR>
	<TR>
		<TD><B>Publication</B></TD>
		<TD><%=bibliographicReference.getPublication() %></TD>
	</TR>
	<TR>
		<TD><B>Abstract Text</B></TD>
		<TD><%=bibliographicReference.getAbstractText() %></TD>
	</TR>
	<TR>
		<TD><B>Publication Date</B></TD>
		<TD><%=bibliographicReference.getPublicationDate() %></TD>
	</TR>
	<%
        }
    %>

	<TR>
		<TD COLSPAN="2">
		<HR>
		</TD>
	</TR>
	<TR>
		<TD>
		<DIV align="right"><INPUT type="button"
			onclick="javascript:selectAction('newSearch')" value="New Search"></DIV>
		</TD>
		<TD>
		<DIV align="right"><INPUT type="button"
			onclick="javascript:selectAction('saveBibRef')" value="Save"></DIV>
		</TD>
	</TR>
</TABLE>
</BODY>
</HTML>
