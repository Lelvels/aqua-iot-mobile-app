package pro.aquaiot.aquablue;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.IBinder;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayDeque;
import java.util.Locale;

import pro.aquaiot.aquablue.constants.WifiMethodNames;
import pro.aquaiot.aquablue.data.model.AquaDeviceTwin;
import pro.aquaiot.aquablue.data.model.Desired;
import pro.aquaiot.aquablue.data.model.DesiredJSON;
import pro.aquaiot.aquablue.data.model.SensorDataValues;
import pro.aquaiot.aquablue.data.model.SensorsData;
import pro.aquaiot.aquablue.data.remote.ApiUtils;
import pro.aquaiot.aquablue.data.remote.AquaService;
import pro.aquaiot.aquablue.fragments.TextUtil;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UiControl extends Fragment implements ServiceConnection, SerialListener, WifiMethodNames {
    private enum BluetoothConnected {False, Pending, True}
    private enum ESPWifiConnected {False, True}
    private final String TAG = "UiControl";
    private final int pullingPeriod = 10000;
    //Internet services members
    private Desired desired = new Desired();
    private SensorDataValues sensorDataValues = new SensorDataValues();
    private String dataLatestDate;
    private AquaService aquaService;
    private final Handler pollingDataHandler = new Handler();
    private ESPWifiConnected espWifiConnected = ESPWifiConnected.False;
    //Bluetooth services members
    private String deviceAddress;
    private String deviceName;
    private SerialService service;
    private BluetoothConnected bluetoothConnected = BluetoothConnected.False;
    private boolean initialStart = true;
    private final String newline = TextUtil.newline_crlf;
    private boolean pendingNewLine = false;
    private boolean keepSendRequestToServer = true;
    //UI members
    private TextView deviceNameTxt;
    private ImageButton wifiSettingButton;
    private ImageView iconWifi;
    private ImageView iconBluetooth;
    private TextView phDisplay;
    private TextView tdsDisplay;
    private TextView temperatureDisplay;
    private TextView lastUpdateDateDisplay;
    private SwitchCompat heaterSwitch;
    private SwitchCompat chillerSwitch;
    private TextView modeButton;
    private TextView modeStateTxt;
    private EditText desiredTemp;
    private Button controlButton;
    private Button choosingFishButton;

    private Button fishAnalysisButton;
    //Pop-up UI members
    private EditText cpSSID;
    private EditText cpPassword;
    private Button cpCancelBtn;
    //Dialog Builder
    private AlertDialog.Builder getDialogBuilder;
    private AlertDialog dialog;

    public UiControl() {
    }

    /*
     * Life cycle
     * */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device_address");
        deviceName = getArguments().getString("device_name");
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null){
            service.attach(this);
        } else {
            getActivity().startService(new Intent(getActivity(), SerialService.class));
        }
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations()){
            service.detach();
            keepSendRequestToServer = false;
        }
        super.onStop();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        getActivity().bindService(new Intent(getActivity(), SerialService.class),
                this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try {
            getActivity().unbindService(this);
            keepSendRequestToServer = false;
        } catch (Exception ignored){}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null){
            initialStart = false;
            keepSendRequestToServer = true;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ui_control, container, false);
        deviceNameTxt = (TextView) view.findViewById(R.id.device_address_text);
        deviceNameTxt.setText(deviceName);
        iconWifi = view.findViewById(R.id.iconWifi);
        iconBluetooth = view.findViewById(R.id.iconBluetooth);
        phDisplay = view.findViewById(R.id.pHDisplay);
        tdsDisplay = view.findViewById(R.id.tdsDisplay);
        temperatureDisplay = view.findViewById(R.id.temperatureDisplay);
        lastUpdateDateDisplay = view.findViewById(R.id.lasted_update_date_display);
        modeStateTxt = view.findViewById(R.id.mode_state);
        //Nút điều khiển wifi
        choosingFishButton = view.findViewById(R.id.choosing_fish_btn);
        wifiSettingButton = view.findViewById(R.id.wifi_setting_btn);
        heaterSwitch = view.findViewById(R.id.heater_switch);
        chillerSwitch = view.findViewById(R.id.chiller_switch);
        modeButton = view.findViewById(R.id.mode_button);
        desiredTemp = view.findViewById(R.id.desired_temp); desiredTemp.setText("0");
        controlButton = view.findViewById(R.id.control_button);
        fishAnalysisButton = view.findViewById(R.id.recommendation_button);
        //Onclick listeners
        wifiSettingButton.setOnClickListener(v -> {
            createNewWifiSettingDialog();
        });
        fishAnalysisButton.setOnClickListener(v->{
            openEnvironmentAnalysisDialog();
        });
        modeButton.setOnClickListener(v -> {
            desired.setAutoMode(!desired.getAutoMode());
            if(desired.getAutoMode()){
                modeStateTxt.setText(R.string.auto_mode);
            } else {
                modeStateTxt.setText(R.string.manual_mode);
            }
        });
        heaterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            desired.setHeater(isChecked);
        });
        chillerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            desired.setChiller(isChecked);
        });
        controlButton.setOnClickListener(v -> {
            patchDesired();
        });
        choosingFishButton.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("device_name", deviceName);
            Intent switchIntent = new Intent(getActivity(), FishForAquariumActivity.class);
            switchIntent.putExtras(bundle);
            startActivity(switchIntent);
        });
        //Starting services
        aquaService = ApiUtils.getAquaService();
        UpdateSensorDataThread updateSensorDataThread = new UpdateSensorDataThread();
        new Thread(updateSensorDataThread).start();
        getDeviceDesired();
        return view;
    }
    /*
    * Internet services
    * */
    private void getLatestSensorValues(){
        aquaService.getLatestSensor().enqueue(new Callback<SensorsData>() {
            @Override
            public void onResponse(Call<SensorsData> call, Response<SensorsData> response) {
                if(response.isSuccessful()){
                    if(sensorDataValues != null){
                        Gson gson = new GsonBuilder().create();
                        sensorDataValues = gson.fromJson(response.body().getData(), SensorDataValues.class);
                        try{
                            dataLatestDate = Instant.parse (response.body().getCreatedAt())
                                    .atZone (ZoneId.of ( "VST" ))
                                    .format (DateTimeFormatter.ofLocalizedDateTime (FormatStyle.SHORT)
                                            .withLocale(Locale.FRANCE));
                        } catch (Exception e){
                            errorLog(e.getMessage());
                        }
                        status("GET: " + response.body().getData());
                        renderSensorData();
                    } else {
                        errorLog("Cannot get the data because it is null!");
                    }
                }
            }

            @Override
            public void onFailure(Call<SensorsData> call, Throwable t) {
                Toast.makeText(getActivity(), "Không thể kết nối đến Server!", Toast.LENGTH_SHORT).show();
                errorLog(t.getMessage());
            }
        });
    }
    private void getDeviceDesired() {
        aquaService.getDeviceTwin().enqueue(new Callback<AquaDeviceTwin>() {
            @Override
            public void onResponse(@NonNull Call<AquaDeviceTwin> call, @NonNull Response<AquaDeviceTwin> response) {
                if (response.isSuccessful()) {
                    Gson gson = new GsonBuilder().create();
                    desired = gson.fromJson(response.body().getData().getDesired(), Desired.class);
                    String connectionState = gson.fromJson(response.body().getData().getConnectionState(), String.class);
                    status("Connection state: " +connectionState.trim());
                    if(connectionState.trim().equals("Connected")){
                        status("Update connection state: " + connectionState);
                        espWifiConnected = ESPWifiConnected.True;
                        renderWifiSymbol();
                    } else if(connectionState.trim().equals("Disconnected")) {
                        status("Update connection state: " + connectionState);
                        espWifiConnected = ESPWifiConnected.False;
                        renderWifiSymbol();
                    } else {
                        status("Invalid connection state is " + connectionState);
                        espWifiConnected = ESPWifiConnected.False;
                    }

                    if(desired != null){
                        status("update Latest Desired:" + response.body().getData().getDesired());
                        renderAutoButton();
                        renderHeaterSwitch();
                        renderChillerSwitch();
                        renderDesiredTemp();
                    } else {
                        Toast.makeText(getActivity(), "Hãy kết nối với mạng Internet!", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    status(response.toString());
                }
            }
            @Override
            public void onFailure(@NonNull Call<AquaDeviceTwin> call, @NonNull Throwable t) {
                errorLog(t.getMessage());
                Toast.makeText(getActivity(), "Fail to get data, maybe data is null!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void patchDesired(){
        if(!desiredTemp.getText().toString().equals("")){
            desired.setSetTemper(Integer.parseInt(desiredTemp.getText().toString()));
            DesiredJSON desiredJSON = new DesiredJSON(desired);
            aquaService.putRequest(desiredJSON).enqueue(new Callback<AquaDeviceTwin>() {
                @Override
                public void onResponse(Call<AquaDeviceTwin> call, Response<AquaDeviceTwin> response) {
                    status("PUT: " + response.body());
                    Toast.makeText(getActivity(), "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Call<AquaDeviceTwin> call, Throwable t) {
                    errorLog(t.getMessage());
                    Toast.makeText(getActivity(), "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getActivity(), "Hãy nhập nhiệt độ mong muốn", Toast.LENGTH_SHORT).show();
        }
    }

    class UpdateSensorDataThread implements Runnable {
        @Override
        public void run() {
            while (keepSendRequestToServer){
                try{
                    pollingDataHandler.post(UiControl.this::getLatestSensorValues);
                    Thread.sleep(pullingPeriod);
                } catch (Exception e){
                    errorLog(e.getMessage());
                }
            }
        }
    }

    /*
    * Internet services UI render
    * */

    //TODO: Need to check whether the key is missing, or else the app will be crash
    private void renderAutoButton(){
        if(desired.getAutoMode() != null){
            if(desired.getAutoMode()){
                modeStateTxt.setText(R.string.auto_mode);
            } else {
                modeStateTxt.setText(R.string.manual_mode);
            }
        } else {
            Toast.makeText(getActivity(), "Đã có lỗi xảy ra với hệ thống tự động, hãy thử lại sau!", Toast.LENGTH_SHORT).show();
            status("The autoMode key is null");
        }

    }

    private void renderHeaterSwitch(){
        if(desired.getHeater() != null){
            if(desired.getHeater()){
                heaterSwitch.setChecked(desired.getHeater());
            } else {
                heaterSwitch.setChecked(desired.getHeater());
            }
        } else {
            Toast.makeText(getActivity(), "Đã có lỗi xảy ra với hệ thống Sưởi, hãy thử lại sau!", Toast.LENGTH_SHORT).show();
            status("The heater key is null");
        }
    }

    private void renderChillerSwitch(){
        if(desired.getChiller() != null){
            if(desired.getChiller()){
                chillerSwitch.setChecked(desired.getChiller());
            } else {
                chillerSwitch.setChecked(desired.getChiller());
            }
        } else {
            Toast.makeText(getActivity(), "Đã có lỗi xảy ra với hệ thống Làm mát, hãy thử lại sau!", Toast.LENGTH_SHORT).show();
            status("The chiller key is null");
        }
    }

    private void renderWifiSymbol(){
        if(espWifiConnected == ESPWifiConnected.True){
            Drawable res = ResourcesCompat.getDrawable(getResources(), R.drawable.wifi_line, null);
            status("Setting wifi icon to connected");
            iconWifi.setImageDrawable(res);
        } else {
            Drawable res = ResourcesCompat.getDrawable(getResources(), R.drawable.wifi_disable_icon, null);
            status("Setting wifi icon to disconnected");
            iconWifi.setImageDrawable(res);
        }
    }

    private void renderDesiredTemp(){
        desiredTemp.setText(desired.getSetTemper().toString());
    }

    private void renderSensorData(){
        try{
            DecimalFormat formatter = new DecimalFormat("#0.0");
            String temperValue = getString(R.string.stringPlaceHolderWithDegreeSymbol,formatter.format(sensorDataValues.getNhietdo()));
            temperatureDisplay.setText(temperValue);
            tdsDisplay.setText(formatter.format(sensorDataValues.getTds()));
            phDisplay.setText(formatter.format(sensorDataValues.getpH()));
            lastUpdateDateDisplay.setText(dataLatestDate);
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private void openEnvironmentAnalysisDialog(){
        getDialogBuilder = new AlertDialog.Builder(getActivity());
        final View envAnalysisPopupView = getLayoutInflater().inflate(R.layout.recommendation_for_environment_popup, null);
        dialog = getDialogBuilder.create();
        dialog.setView(envAnalysisPopupView);
        dialog.show();
    }

    /*
     * Bluetooth UI
     * */
    private String getWifiJson(String wifiSSID, String password){
        //{"method":"ActivateWifi", "SSID":"", "Password":""}
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("method", WifiMethodNames.WIFI_CONNECT_METHOD);
            jsonObject.put("SSID", wifiSSID);
            jsonObject.put("Password", password);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        return jsonObject.toString();
    }

    private void createNewWifiSettingDialog(){
        getDialogBuilder = new AlertDialog.Builder(getActivity());
        final View connectivityPopupView = getLayoutInflater().inflate(R.layout.connection_popup_ui_control, null);
        cpSSID = connectivityPopupView.findViewById(R.id.SSID_edit_text);
        cpPassword = connectivityPopupView.findViewById(R.id.password_edit_text);
        cpSSID.setText(getString(R.string.default_ssid_wifi));
        cpPassword.setText(R.string.default_ssid_password);
        Button cpSubmitButton = connectivityPopupView.findViewById(R.id.wifi_submit_btn);
        cpCancelBtn = connectivityPopupView.findViewById(R.id.cancel_btn);
        cpSubmitButton.setOnClickListener(v->{
            if(bluetoothConnected == BluetoothConnected.True){
                String wifiName = cpSSID.getText().toString();
                String password = cpPassword.getText().toString();
                status("Connecting to Wifi: " + wifiName);
                send(getWifiJson(wifiName, password));
                Toast.makeText(getActivity(), "Đang kết nối Wifi...", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } else {
                errorLog("Bluetooth not connected to device!");
                Toast.makeText(getActivity(), "Hãy kết nối với thiết bị bằng Bluetooth", Toast.LENGTH_SHORT).show();
            }
        });
        cpCancelBtn.setOnClickListener(v -> {
            dialog.dismiss();
        });
        dialog = getDialogBuilder.create();
        dialog.setView(connectivityPopupView);
        dialog.show();
    }

    /*
     * Bluetooth service connection
     * */
    private void status(String str) {
        Log.i(TAG, "Message: " + str);
    }

    private void errorLog(String str){
        Log.e(TAG, "Error: " + str);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service = null;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        service = ((SerialService.SerialBinder) iBinder).getService();
        service.attach(this);
        if(initialStart && isResumed()){
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
            status("Bluetooth connection service started!");
        }
    }

    private void connect(){
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("Bluetooth connecting...");
            bluetoothConnected = UiControl.BluetoothConnected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e){
            errorLog(e.getMessage());
            onSerialConnectError(e);
        }
    }

    private void send(String msg){
        if(bluetoothConnected != BluetoothConnected.True){
            status("Bluetooth not connected, connect to bluetooth before sending!");
            return;
        }
        try {
            byte[] data;
            data = (msg + newline).getBytes(StandardCharsets.UTF_8);
            status("Sending via Bluetooth: " + msg);
            service.write(data);
        } catch (Exception e){
            onSerialIoError(e);
        }
    }

    private void disconnect(){
        bluetoothConnected = BluetoothConnected.False;
        Drawable res = ResourcesCompat.getDrawable(getResources(), R.drawable.bluetooth_disabled_icon, null);
        iconBluetooth.setImageDrawable(res);
        service.disconnect();
    }

    private void receive(ArrayDeque<byte[]> datas){
        SpannableStringBuilder spn = new SpannableStringBuilder();
        for(byte[] data : datas){
            String msg = new String(data);
            if(newline.equals(TextUtil.newline_crlf) && msg.length() > 0){
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                // special handling if CR and LF come in separate fragments
                if (pendingNewLine && msg.charAt(0) == '\n') {
                    if(spn.length() >= 2) {
                        spn.delete(spn.length() - 2, spn.length());
                    }
                }
                pendingNewLine = msg.charAt(msg.length() - 1) == '\r';
            }
            spn.append(TextUtil.toCaretString(msg, newline.length() != 0));
        }
        processMessageFromBluetooth(spn.toString());
    }

    private void processMessageFromBluetooth(String msg) {
        status("Message from esp: " + msg);
        if (msg.contains("WiFi connected")) {
            Toast.makeText(getActivity(), "Thiết bị đã kết nối WiFi", Toast.LENGTH_SHORT).show();
        } else if (msg.contains("WiFi not connected")){
            Toast.makeText(getActivity(), "Thiết bị chưa kết nối được WiFi", Toast.LENGTH_SHORT).show();
        } else {
            status("This string is out of scope of process!");
        }
        return;
    }

    /*
     * Serial Listener services implementation
     * */

    @Override
    public void onSerialConnect() {
        status("Bluetooth connected!");
        bluetoothConnected = BluetoothConnected.True;
        Drawable res = ResourcesCompat.getDrawable(getResources(), R.drawable.bluetooth_icon, null);
        iconBluetooth.setImageDrawable(res);
    }

    @Override
    public void onSerialRead(byte[] data) {
        ArrayDeque<byte[]> datas = new ArrayDeque<>();
        datas.add(data);
        receive(datas);
    }

    @Override
    public void onSerialRead(ArrayDeque<byte[]> datas) {
        receive(datas);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connected failed: " + e.getMessage());
        disconnect();
    }
}