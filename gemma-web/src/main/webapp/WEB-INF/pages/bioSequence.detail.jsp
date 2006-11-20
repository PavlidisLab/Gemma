<%@ include file="/common/taglibs.jsp"%>
<jsp:useBean id="bioSequence" scope="request"
    class="ubic.gemma.model.genome.biosequence.BioSequenceImpl" />
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
    <body>
        <h2>
            <fmt:message key="bioSequence.details" />
        </h2>
        <table width="100%" cellspacing="10">
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioSequence.name" />
                    </b>
                </td>
                <td>
                	<%if (bioSequence.getName() != null){%>
                    	<jsp:getProperty name="bioSequence" property="name" />
                    <%}else{
                    	out.print("No name available");
                    }%>
                </td>
            </tr>
                 
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioSequence.description" />
                    </b>
                </td>
                <td>
                	<%if (bioSequence.getDescription() != null){%>
                    <textarea name="" rows=5 cols=80 readonly><jsp:getProperty name="bioSequence" property="description" /></textarea>
                    <%}else{
                    	out.print("Description unavailable");
                    }%>
                </td>
            </tr>
            
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioSequence.taxon" />
                    </b>
                </td>
                <td>
                    <%if (bioSequence.getTaxon() != null) {
                    	out.print(bioSequence.getTaxon().getScientificName());
                	} else{
                    	out.print("Taxon unavailable");
                    }%>
                </td>
            </tr>
            
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioSequence.isCircular" />
                    </b>
                </td>
                <td>
                	<%if ( (bioSequence.getIsCircular() != null) && (bioSequence.getIsCircular()) ){
                    	out.print("Yes");
                    }else{
                    	out.print("No");
                    }%>
                </td>
            </tr>
            
            <tr>
                <td valign="top">
                    <b>
                        <fmt:message key="bioSequence.length" />
                    </b>
                </td>
                <td>
                	<%if (bioSequence.getLength() != null) {%>
                    	<jsp:getProperty name="bioSequence" property="length" />
                    <%	if (bioSequence.getIsApproximateLength())  {
                        	out.print("(approximate)");
                    	}
                    }
                	else {
                    	out.print("Length unavailable");
                    }%>
                </td>
            </tr>
            
        </table>
        <h2>
        Sequence
        </h2>
        <br />
        <%if (bioSequence.getSequence() != null){%>
        <textarea name="" rows=5 cols=80 readonly=true><jsp:getProperty name="bioSequence" property="sequence" /></textarea>
        <%}else{
        	out.print("Sequence unavailable");
        }%>
 

		<br />

    </body>
</html>
