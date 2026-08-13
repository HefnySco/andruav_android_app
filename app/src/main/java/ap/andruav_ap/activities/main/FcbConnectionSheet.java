package ap.andruav_ap.activities.main;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.andruav.Constants;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import ap.andruav_ap.App;
import ap.andruav_ap.DeviceManagerFacade;
import ap.andruav_ap.R;
import ap.andruav_ap.activities.fcb.drone.Adapter_BluetoothList;
import ap.andruav_ap.activities.fcb.drone.ListItem_BluetoothUnit;
import ap.andruav_ap.communication.telemetry.TelemetryModeer;
import ap.andruavmiddlelibrary.factory.communication.NetInfoAdapter;
import ap.andruavmiddlelibrary.preference.Preference;

/***
 * Restyled entry point for the FCB (flight controller board) connection settings,
 * per the Home screen redesign handoff. The field-handling here is intentionally
 * duplicated (not shared) from {@link ap.andruav_ap.activities.fcb.drone.FCB_AndruavShashaL2},
 * which stays in the codebase unreferenced until a later cleanup pass; controls it
 * exposes that aren't part of the new design (auto-connect toggle, dual-band toggle,
 * smart-telemetry dialog, live SysID readout) are not reproduced here.
 */
public class FcbConnectionSheet extends BottomSheetDialogFragment implements Adapter_BluetoothList.OnCustomClickListener {

    public static final String TAG = "FcbConnectionSheet";

    public static FcbConnectionSheet newInstance() {
        return new FcbConnectionSheet();
    }

    private boolean isConnected;
    private int selectedType;

    private View rowBt, rowUsb, rowWifi, rowUdp;
    private View dotBt, dotUsb, dotWifi, dotUdp;
    private View groupBt, groupUsb, groupWifi, groupUdp;
    private TextView btnBtToggle;
    private TextView btnBtScan;
    private ListView listBt;
    private Adapter_BluetoothList btAdapter;
    private View selectedBtRow;
    private Spinner spinnerBaud;
    private TextView txtUsbStatus;
    private EditText edtHubIp;
    private EditText edtHubPort;
    private TextView txtLocalIp;
    private EditText edtUdpPort;
    private TextView btnConnect;
    private TextView btnDisconnect;

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, android.content.Intent intent) {
            final String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                final BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if ((device != null) && (btAdapter != null)) {
                    btAdapter.add(device);
                    btAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                btnBtScan.setText(getString(R.string.home_fcb_bt_scanning));
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                btnBtScan.setText(getString(R.string.home_fcb_bt_scan));
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_fcb_connection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isConnected = TelemetryModeer.getConnectionInfo() != TelemetryModeer.CURRENTCONNECTION_NON;
        selectedType = Preference.getFCBTargetComm(null);

        bindViews(view);
        wireAccordion();
        updateAccordionVisibility();
        updateAccordionEnabled();
        wireBluetooth();
        loadFields();
        wireConnectButtons();
    }

    @Override
    public void onStart() {
        super.onStart();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        requireContext().registerReceiver(btReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            requireContext().unregisterReceiver(btReceiver);
        } catch (IllegalArgumentException ignored) {
            // was never registered (e.g. onViewCreated threw before onStart) - nothing to undo.
        }
    }

    private void bindViews(View view) {
        rowBt = view.findViewById(R.id.fcb_sheet_row_bt);
        rowUsb = view.findViewById(R.id.fcb_sheet_row_usb);
        rowWifi = view.findViewById(R.id.fcb_sheet_row_wifi);
        rowUdp = view.findViewById(R.id.fcb_sheet_row_udp);

        dotBt = view.findViewById(R.id.fcb_sheet_dot_bt);
        dotUsb = view.findViewById(R.id.fcb_sheet_dot_usb);
        dotWifi = view.findViewById(R.id.fcb_sheet_dot_wifi);
        dotUdp = view.findViewById(R.id.fcb_sheet_dot_udp);

        groupBt = view.findViewById(R.id.fcb_sheet_group_bt);
        groupUsb = view.findViewById(R.id.fcb_sheet_group_usb);
        groupWifi = view.findViewById(R.id.fcb_sheet_group_wifi);
        groupUdp = view.findViewById(R.id.fcb_sheet_group_udp);

        btnBtToggle = view.findViewById(R.id.fcb_sheet_btn_bt_toggle);
        btnBtScan = view.findViewById(R.id.fcb_sheet_btn_bt_scan);
        listBt = view.findViewById(R.id.fcb_sheet_list_bt);

        spinnerBaud = view.findViewById(R.id.fcb_sheet_spinner_baud);
        txtUsbStatus = view.findViewById(R.id.fcb_sheet_txt_usb_status);

        edtHubIp = view.findViewById(R.id.fcb_sheet_edt_hub_ip);
        edtHubPort = view.findViewById(R.id.fcb_sheet_edt_hub_port);
        txtLocalIp = view.findViewById(R.id.fcb_sheet_txt_local_ip);
        edtUdpPort = view.findViewById(R.id.fcb_sheet_edt_udp_port);

        btnConnect = view.findViewById(R.id.fcb_sheet_btn_connect);
        btnDisconnect = view.findViewById(R.id.fcb_sheet_btn_disconnect);
    }

    private void wireAccordion() {
        final View.OnClickListener rowClick = v -> {
            if (isConnected) return; // must disconnect before changing transport
            if (v == rowBt) selectedType = Preference.FCB_COM_BT;
            else if (v == rowUsb) selectedType = Preference.FCB_COM_USB;
            else if (v == rowWifi) selectedType = Preference.FCB_COM_TCP;
            else selectedType = Preference.FCB_COM_UDP;
            Preference.setFCBTargetComm(null, selectedType);
            updateAccordionVisibility();
        };
        rowBt.setOnClickListener(rowClick);
        rowUsb.setOnClickListener(rowClick);
        rowWifi.setOnClickListener(rowClick);
        rowUdp.setOnClickListener(rowClick);
    }

    private void updateAccordionVisibility() {
        groupBt.setVisibility(selectedType == Preference.FCB_COM_BT ? View.VISIBLE : View.GONE);
        groupUsb.setVisibility(selectedType == Preference.FCB_COM_USB ? View.VISIBLE : View.GONE);
        groupWifi.setVisibility(selectedType == Preference.FCB_COM_TCP ? View.VISIBLE : View.GONE);
        groupUdp.setVisibility(selectedType == Preference.FCB_COM_UDP ? View.VISIBLE : View.GONE);

        tintDot(dotBt, selectedType == Preference.FCB_COM_BT);
        tintDot(dotUsb, selectedType == Preference.FCB_COM_USB);
        tintDot(dotWifi, selectedType == Preference.FCB_COM_TCP);
        tintDot(dotUdp, selectedType == Preference.FCB_COM_UDP);
    }

    private void tintDot(View dot, boolean selected) {
        setColor(dot, selected ? R.color.home_amber : R.color.home_border_input);
    }

    private void setColor(View view, @ColorRes int colorRes) {
        final Drawable bg = view.getBackground();
        if (bg instanceof GradientDrawable) {
            ((GradientDrawable) bg.mutate()).setColor(ContextCompat.getColor(requireContext(), colorRes));
        }
    }

    private void updateAccordionEnabled() {
        final float alpha = isConnected ? 0.55f : 1f;
        for (View row : new View[]{rowBt, rowUsb, rowWifi, rowUdp}) {
            row.setEnabled(!isConnected);
            row.setAlpha(alpha);
        }
    }

    private boolean isBluetoothUsable() {
        if (!DeviceManagerFacade.hasBlueTooth()) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void wireBluetooth() {
        final boolean usable = isBluetoothUsable() && !isConnected;
        rowBt.setEnabled(usable);
        rowBt.setAlpha(usable ? 1f : 0.55f);

        btAdapter = new Adapter_BluetoothList(requireActivity(), this);
        listBt.setAdapter(btAdapter);

        btnBtToggle.setOnClickListener(v -> {
            if (App.BT.Bluetooth.isEnabled()) {
                turnOffBlueTooth();
            } else {
                turnOnBlueTooth();
            }
            updateBtToggleUi();
        });

        btnBtScan.setOnClickListener(v -> {
            if (App.BT.Bluetooth.isEnabled()) {
                scanForDevices();
            }
        });

        updateBtToggleUi();
    }

    private void turnOnBlueTooth() {
        if (App.BT.Bluetooth.GetAdapter()) {
            App.BT.Bluetooth.Enable();
        }
    }

    private void turnOffBlueTooth() {
        App.BT.StopPersistentConnection();
        if (App.BT.Bluetooth.isDiscovering()) {
            App.BT.Bluetooth.cancelDiscovery();
        }
        App.BT.Bluetooth.disable();
    }

    private void scanForDevices() {
        App.BT.StopPersistentConnection();
        if (App.BT.Bluetooth.isDiscovering()) {
            App.BT.Bluetooth.cancelDiscovery();
        }
        btAdapter.clear();
        btAdapter.notifyDataSetChanged();
        selectedBtRow = null;
        App.BT.Bluetooth.startDiscovery();
    }

    private void updateBtToggleUi() {
        final boolean on = (App.BT.Bluetooth != null) && App.BT.Bluetooth.isEnabled();
        btnBtToggle.setText(on ? R.string.home_fcb_bt_on : R.string.home_fcb_bt_off);
        btnBtToggle.setBackgroundResource(on ? R.drawable.bg_home_bt_toggle_on : R.drawable.bg_home_bt_toggle_off);
        listBt.setVisibility(on ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onItemClick(View aView, int position) {
        final ListItem_BluetoothUnit item = (ListItem_BluetoothUnit) btAdapter.getItem(position);
        if (item == null) return;

        Preference.setFCBBlueToothMAC(null, item.getDeviceMAC());
        Preference.setFCBBlueToothName(null, item.getDeviceName());

        if (selectedBtRow != null) {
            selectedBtRow.setBackgroundResource(android.R.color.transparent);
        }
        aView.setBackgroundResource(R.drawable.bg_home_bt_row_selected);
        selectedBtRow = aView;
    }

    private void loadFields() {
        final ArrayAdapter<CharSequence> baudAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_home, Constants.baudRateItems);
        baudAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_home);
        spinnerBaud.setAdapter(baudAdapter);
        spinnerBaud.setSelection(Preference.getFCBUSBBaudRateSelector(null));

        final boolean usbSupported = DeviceManagerFacade.hasUSBHost();
        txtUsbStatus.setText(usbSupported ? R.string.home_fcb_usb_supported : R.string.home_fcb_usb_unsupported);
        final boolean usbUsable = usbSupported && !isConnected;
        rowUsb.setEnabled(usbUsable);
        rowUsb.setAlpha(usbUsable ? 1f : 0.55f);

        edtHubIp.setText(Preference.getFCBDroneTCPServerIP(null));
        edtHubPort.setText(Preference.getFCBDroneTCPServerPort(null));
        edtUdpPort.setText(Preference.getFCBDroneUDPServerPort(null));

        // Same source the old FCB activity used: falls through wlan0/tethering/AP interface
        // names, then any private-range IPv4 as a last resort - so this reads correctly
        // whether the phone is on Wi-Fi, USB/Wi-Fi tethering, or acting as its own AP.
        String localIp = NetInfoAdapter.getIPWifi();
        if (localIp == null) localIp = "127.0.0.1";
        txtLocalIp.setText(localIp);
    }

    private void savePreferences() {
        Preference.setFCBDroneTCPServerIP(null, edtHubIp.getText().toString());
        Preference.setFCBDroneTCPServerPort(null, edtHubPort.getText().toString());
        Preference.setFCBDroneUDPServerPort(null, edtUdpPort.getText().toString());
        Preference.setFCBTargetLib(null, Preference.FCB_LIB_3DR);
        Preference.setFCBTargetComm(null, selectedType);
        Preference.setFCBUSBBaudRateSelector(null, spinnerBaud.getSelectedItemPosition());
    }

    private void wireConnectButtons() {
        btnConnect.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        btnDisconnect.setVisibility(isConnected ? View.VISIBLE : View.GONE);

        btnConnect.setOnClickListener(v -> {
            savePreferences();
            TelemetryModeer.connectToPreferredConnection(requireActivity(), false);
            dismiss();
        });

        btnDisconnect.setOnClickListener(v -> {
            TelemetryModeer.closeAllConnections();
            dismiss();
        });
    }
}
