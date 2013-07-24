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

    private String teaser;

    private String body;

    public String getBody() {
        return body;
    }

    public Date getDate() {
        return date;
    }

    public String getTeaser() {
        return teaser;
    }

    public String getTitle() {
        return title;
    }

    public void setBody( String body ) {
        this.body = body;
    }

    public void setDate( Date date ) {
        this.date = date;
    }

    public void setTeaser( String teaser ) {
        this.teaser = teaser;
    }

    public void setTitle( String title ) {
        this.title = title;
    }

}
