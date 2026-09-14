package ap.andruav_ap.activities.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.greenrobot.eventbus.EventBus;

import ap.andruav_ap.App;
import ap.andruav_ap.R;
import ap.andruav_ap.guiEvent.GUIEvent_MissionServerChanged;
import ap.andruavmiddlelibrary.factory.util.DialogHelper;
import ap.andruavmiddlelibrary.preference.Preference;

import com.andruav.AndruavEngine;
import com.andruav.AndruavSettings;

/***
 * Restyled entry point for the Mission Server (Andruav HUB) connection settings, per the
 * Home screen redesign handoff. The field-handling here is intentionally duplicated (not
 * shared) from {@link ap.andruav_ap.activities.HUBCommunication}, which stays in the codebase
 * unreferenced until a later cleanup pass; controls it exposes that aren't part of the new
 * design (email-support menu action, manual "refresh from saved" action, the save-before-exit
 * back-press dialog that also force-stopped the WS) are not reproduced here - the sheet always
 * reloads from saved preferences on open, and dismissing without tapping Save simply discards
 * edits, same convention as {@link FcbConnectionSheet}.
 */
public class HubConnectionSheet extends BottomSheetDialogFragment {

    public static final String TAG = "HubConnectionSheet";

    public static HubConnectionSheet newInstance() {
        return new HubConnectionSheet();
    }

    private EditText edtServerIp;
    private EditText edtServerPort;
    private TextView btnCloudToggle;
    private EditText edtUsername;
    private EditText edtDescription;
    private TextView btnSave;

    private boolean cloudServer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sheet_hub_connection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        bindViews(view);
        wireCloudToggle();
        wireSave();
        loadFields();
    }

    private void bindViews(View view) {
        edtServerIp = view.findViewById(R.id.hub_sheet_edt_server_ip);
        edtServerPort = view.findViewById(R.id.hub_sheet_edt_server_port);
        btnCloudToggle = view.findViewById(R.id.hub_sheet_btn_cloud_toggle);
        edtUsername = view.findViewById(R.id.hub_sheet_edt_username);
        edtDescription = view.findViewById(R.id.hub_sheet_edt_description);
        btnSave = view.findViewById(R.id.hub_sheet_btn_save);
    }

    private void wireCloudToggle() {
        btnCloudToggle.setOnClickListener(v -> {
            cloudServer = !cloudServer;
            updateCloudToggleUi();
        });
    }

    private void updateCloudToggleUi() {
        btnCloudToggle.setText(cloudServer ? R.string.home_fcb_bt_on : R.string.home_fcb_bt_off);
        btnCloudToggle.setBackgroundResource(cloudServer ? R.drawable.bg_home_bt_toggle_on : R.drawable.bg_home_bt_toggle_off);

        edtServerIp.setEnabled(!cloudServer);
        edtServerPort.setEnabled(!cloudServer);
        if (cloudServer) {
            edtServerIp.setText(AndruavEngine.getPreference().getContext().getResources().getString(ap.andruavmiddlelibrary.R.string.pref_auth_URL));
            edtServerPort.setText(String.valueOf(AndruavEngine.getPreference().getContext().getResources().getInteger(ap.andruavmiddlelibrary.R.integer.pref_auth_Port)));
        }
    }

    private void loadFields() {
        edtServerPort.setText(String.valueOf(Preference.getAuthServerPort(null)));
        edtServerIp.setText(String.valueOf(Preference.getAuthServerURL(null)));
        edtUsername.setText(String.valueOf(Preference.getWebServerUserName(null)));
        edtDescription.setText(String.valueOf(Preference.getWebServerUserDescription(null)));

        cloudServer = !Preference.isLocalServer(null);
        updateCloudToggleUi();
    }

    private void wireSave() {
        btnSave.setOnClickListener(v -> {
            if (savePreferences()) {
                dismiss();
            }
        });
    }

    private boolean savePreferences() {
        if (edtServerIp.getText().length() == 0) {
            DialogHelper.doModalDialog(requireContext(), getString(ap.andruavmiddlelibrary.R.string.websocket_IP), getString(ap.andruavmiddlelibrary.R.string.err_nullValue), null);
            return false;
        }
        if (edtServerPort.getText().length() == 0) {
            DialogHelper.doModalDialog(requireContext(), getString(ap.andruavmiddlelibrary.R.string.websocket_Port), getString(ap.andruavmiddlelibrary.R.string.err_nullValue), null);
            return false;
        }
        if (edtUsername.getText().length() == 0) {
            DialogHelper.doModalDialog(requireContext(), getString(ap.andruavmiddlelibrary.R.string.websocket_UserName), getString(ap.andruavmiddlelibrary.R.string.err_nullValue), null);
            return false;
        }
        if (AndruavSettings.isValidAndruavUnitName(String.valueOf(edtUsername.getText()))) {
            DialogHelper.doModalDialog(requireContext(), getString(ap.andruavmiddlelibrary.R.string.websocket_UserName), getString(ap.andruavmiddlelibrary.R.string.err_invalid_unitname), null);
            return false;
        }
        if (edtDescription.getText().length() == 0) {
            DialogHelper.doModalDialog(requireContext(), getString(ap.andruavmiddlelibrary.R.string.websocket_Description), getString(ap.andruavmiddlelibrary.R.string.err_nullValue), null);
            return false;
        }

        Preference.setAuthServerPort(null, Integer.parseInt(edtServerPort.getText().toString()));
        Preference.setAuthServerURL(null, edtServerIp.getText().toString());
        Preference.setWebServerUserName(null, edtUsername.getText().toString().toLowerCase());
        Preference.setWebServerGroupName(null, App.getAppContext().getString(ap.andruavmiddlelibrary.R.string.pref_groupname).toLowerCase());
        Preference.setWebServerUserDescription(null, edtDescription.getText().toString());
        Preference.isLocalServer(null, !cloudServer);
        Preference.isEnforceName(null, true); // ActivityMosa3ed Fix

        App.updateWe7daInfo();
        AndruavSettings.andruavWe7daBase.setVehicleType(Preference.getVehicleType(null));

        AndruavEngine.notification().Speak(getString(ap.andruavmiddlelibrary.R.string.action_saved));

        EventBus.getDefault().post(new GUIEvent_MissionServerChanged());

        return true;
    }
}
