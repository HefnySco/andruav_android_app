package ap.andruavmiddlelibrary.database;

/**
 * Created by mhefny on 2/13/16.
 */
public class GenericDataRow {

    public static final int DB_TYPE_KML_POINT = 1;
    public static final int DB_TYPE_KML_IMAGE = 2;


    private Long id;

    private Long Type;

    private  String Data;


    public GenericDataRow()
    {

    }

    public GenericDataRow (Long id)
    {
        this.id = id;
    }


    public GenericDataRow (final Long id, final Long Type, final String Data)
    {
        this.id = id;
        this.Type = Type;
        this.Data = Data;
    }



    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }




    public Long  getType() {
        return Type;
    }

    public void setType(final Long Type) {
        this.Type = Type;
    }


    public String  getData() {
        return Data;
    }

    public void setData(final String Data) {
        this.Data = Data;
    }

}
