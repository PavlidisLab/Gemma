package ubic.gemma.web.feed;

import java.util.Date;

/**
 * TODO Document Me
 * 
 * @author paul
 * @version $Id$
 */
public class NewsItem {

    private String title;

    private Date date;

    public String getTitle() {
        return title;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

    public Date getDate() {
        return date;
    }

    public void setDate( Date date ) {
        this.date = date;
    }

    public String getTeaser() {
        return teaser;
    }

    public void setTeaser( String teaser ) {
        this.teaser = teaser;
    }

    public String getBody() {
        return body;
    }

    public void setBody( String body ) {
        this.body = body;
    }

    private String teaser;

    private String body;

}
