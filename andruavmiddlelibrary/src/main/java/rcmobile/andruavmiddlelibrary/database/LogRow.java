package rcmobile.andruavmiddlelibrary.database;

/**
 * Created by mhefny on 2/13/16.
 */
public class LogRow {

    private Long id;

    private String UserName;

    private String Tag;

    private  String Error;


    public LogRow()
    {

    }

    public LogRow (Long id)
    {
        this.id = id;
    }

    public LogRow (Long id, final String UserName, final String Tag, final String Error)
    {
        this.id = id;
        this.UserName = UserName.replaceAll("\"","\"").replaceAll("'", "'");
        this.Tag = Tag.replaceAll("\"","\"").replaceAll("'", "'");
        //this.Error = Error.replace('"','\"').replace("'","\'");
        this.Error = Error.replaceAll("\"","\"").replaceAll("'", "'");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String  getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        this.UserName = userName;
    }

    public String  getTag() {
        return Tag;
    }

    public void setTag(String tag) {
        this.Tag = tag;
    }

    public String  getError() {
        return Error;
    }

    public void setError(String error) {
        this.Error = error;
    }


}
