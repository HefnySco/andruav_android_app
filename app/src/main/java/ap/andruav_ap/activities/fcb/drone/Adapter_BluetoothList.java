package ap.andruav_ap.activities.fcb.drone;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import ap.andruav_ap.R;

/**
 * Created by mhefny on 1/22/16.
 */
public class Adapter_BluetoothList extends BaseAdapter {


    /*********** Declare Used Variables *********/
    private final Activity activity;
    final ArrayList<ListItem_BluetoothUnit> data = new ArrayList<ListItem_BluetoothUnit>();
    private static LayoutInflater inflater=null;
    private final OnCustomClickListener callback; // This is our activity

    public Adapter_BluetoothList(Activity a, OnCustomClickListener clickListener) {
        super();
        /********* Take passed values **********/
        activity = a;
        callback=clickListener;
        /**********  Layout inflator to call external xml layout () ***********/
        inflater = ( LayoutInflater )activity.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }


    @Override
    public int getCount() {
        if(data.size()<=0)
            return 0;
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        if (data.size() < (i-1))
        {
            return null;
        }
        return data.get(i);
    }

    public void add (ListItem_BluetoothUnit item){
        data.add(item);
    }


    public void add (final BluetoothDevice device){
        ListItem_BluetoothUnit item = new ListItem_BluetoothUnit();
        item.setDeviceName(device.getName());
        item.setDeviceMAC(device.getAddress());
        if (data.contains(item)) return ;
        data.add(item);
    }


    public int getPosition (String unitID)
    {
        if (data.size()==-1) return -1;
        for (int i=0;i<data.size();i=i+1)
        {
            if (data.get(i).getDeviceName().equals(unitID))
                return i;
        }
        return -1;
    }

    public void clear() {
        data.clear();
    }

    @Override
    public long getItemId(int i) {
        return data.indexOf(data.get(i));
    }
    /********* Create a holder Class to contain inflated xml file elements *********/
    public static class ViewHolder{

        public TextView txtBTName;
        public TextView txtBTMac;
       // public ToggleButton btnSelected;

    }




    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ListItem_BluetoothUnit tempValues;
        View vi = convertView;
        ViewHolder holder;

        if(convertView==null){

            /***** Inflate tabitem.xml file for each row ( Defined below ) *******/
            vi = inflater.inflate(R.layout.listitem_bluetoothitem, null);

            /***** View Holder Object to contain tabitem.xml file elements ******/

            holder = new ViewHolder();
            holder.txtBTName = vi.findViewById(R.id.bluetoothitem_txtName);
            holder.txtBTMac = vi.findViewById(R.id.bluetoothitem_txtMac);

            /***********  Set holder with LayoutInflater ************/
            vi.setTag( holder );
        }
        else
            holder=(ViewHolder)vi.getTag();

        if(data.size()<=0)
        {
            holder.txtBTName.setText("No Data");

        }
        else
        {
            /**** Get each Model object from Arraylist ********/
           tempValues = data.get( position );
            holder.txtBTName.setText(tempValues.deviceName);
            holder.txtBTMac.setText(tempValues.deviceMAC);
            vi.setOnClickListener(new CustomOnClickListener(callback, position));

            /***********  Set Model values in Holder elements ***********/
        }
        return vi;
    }

    /***
     * Interface should be inherited by any activity want to handle click of this list view.
     */
    public interface OnCustomClickListener {
        void onItemClick(View aView, int position);
        // Feel free to add other methods of use. OnCustomTouch for example :)
    }


    public static class CustomOnClickListener implements View.OnClickListener {
        private final int position;
        private final OnCustomClickListener callback;


        public CustomOnClickListener (OnCustomClickListener c, int p)
        {
            callback=c;
            position=p;
        }
        // The onClick method which has NO position information
        @Override
        public void onClick(View v) {

            // Let's call our custom callback with the position we added in the constructor
            callback.onItemClick(v, position);
        }
    }
}
