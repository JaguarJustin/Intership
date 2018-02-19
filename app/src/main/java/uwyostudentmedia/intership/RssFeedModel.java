package uwyostudentmedia.intership;

/**
 * Created by studentmedia on 2/14/18.
 */

public class RssFeedModel {

    public String title;
    public String link;

    public RssFeedModel(String title,String link) {

        this.title = title;
        this.link = link;
    }

    public String getLink() {
        return link;
    }
}
