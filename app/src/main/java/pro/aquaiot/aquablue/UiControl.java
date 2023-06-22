package pro.aquaiot.aquablue;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import pro.aquaiot.aquablue.constants.WifiMethodNames;
import pro.aquaiot.aquablue.data.model.AquaDeviceTwin;
import pro.aquaiot.aquablue.data.model.Aquarium;
import pro.aquaiot.aquablue.data.model.AquariumData;
import pro.aquaiot.aquablue.data.model.CreateAquariumRequest;
import pro.aquaiot.aquablue.data.model.Desired;
import pro.aquaiot.aquablue.data.model.DesiredJSON;
import pro.aquaiot.aquablue.data.model.Fish;
import pro.aquaiot.aquablue.data.model.FishData;
import pro.aquaiot.aquablue.data.model.SensorDataValues;
import pro.aquaiot.aquablue.data.model.SensorsData;
import pro.aquaiot.aquablue.data.remote.ApiUtils;
import pro.aquaiot.aquablue.data.remote.AquaApplicationService;
import pro.aquaiot.aquablue.data.remote.AquaService;
import pro.aquaiot.aquablue.data.remote.RetrofitInstance;
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
    private List<Fish> fishList;
    private final Handler pollingDataHandler = new Handler();
    private ESPWifiConnected espWifiConnected = ESPWifiConnected.False;
    //Bluetooth services members
    private String deviceAddress;
    private String deviceName;
    private Aquarium aquarium = null;
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
        fishList = new ArrayList<>();
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

    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
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
        if(isNetworkAvailable(getActivity().getApplicationContext())) {
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
            UpdateDataThread updateDataThread = new UpdateDataThread();
            new Thread(updateDataThread).start();
            getDeviceInformation();
        } else {
            Toast.makeText(getActivity(), "Hiện không có kết nối mạng, vui lòng thử lại sau", Toast.LENGTH_SHORT).show();
        }

        return view;
    }
    /*
    * Update Data Thread
    * */

    class UpdateDataThread implements Runnable {
        @Override
        public void run() {
            while (keepSendRequestToServer){
                try{
                    pollingDataHandler.post(UiControl.this::getLatestSensorValues);
                    pollingDataHandler.post(UiControl.this::getDeviceInformation);
                    Thread.sleep(pullingPeriod);
                } catch (Exception e){
                    errorLog(e.getMessage());
                }
            }
        }
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
                        status("GET id: " + response.body().getId() + ", with data: " + response.body().getData());
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
    private void getDeviceInformation() {
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
    public void startGetFishData(String aquariumName) {
        AquaApplicationService aquaApplicationService = RetrofitInstance.getService();
        Call<AquariumData> call = aquaApplicationService.getAquariumByName(aquariumName);
        call.enqueue(new Callback<AquariumData>() {
            @Override
            public void onResponse(Call<AquariumData> call, Response<AquariumData> response) {
                if(response.isSuccessful()){
                    if(response.body().getAquarium() != null){
                        aquarium = response.body().getAquarium();
                        Log.i(TAG, "Get aquarium: " + aquarium.toString());
                        getFishInAquarium();
                    } else {
                        createAquarium(aquariumName, "New aquarium created by AquaBlue");
                    }
                } else {
                    Toast.makeText(getActivity(),
                            "Không thể nhận dữ liệu từ Server, hãy kiểm tra đường truyền và thử lại sau",
                            Toast.LENGTH_SHORT).show();
                }

            }
            @Override
            public void onFailure(Call<AquariumData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }

    public void createAquarium(String aquariumName, String description){
        AquaApplicationService aquaApplicationService = RetrofitInstance.getService();
        CreateAquariumRequest createAquariumRequest = new CreateAquariumRequest(aquariumName, description);
        Call<AquariumData> call = aquaApplicationService.createNewAquarium(createAquariumRequest);
        call.enqueue(new Callback<AquariumData>() {
            @Override
            public void onResponse(Call<AquariumData> call, Response<AquariumData> response) {
                if(response.isSuccessful()){
                    if(response.body() != null) {
                        aquarium = response.body().getAquarium();
                        Log.i(TAG, "Create new aquarium: " + aquarium.toString());
                        getFishInAquarium();
                    }
                    else {
                        Toast.makeText(getActivity(),
                                "Không thể nhận dữ liệu từ Server, hãy kiểm tra đường truyền và thử lại sau",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getActivity(),
                            "Không thể nhận dữ liệu từ Server, hãy kiểm tra đường truyền và thử lại sau",
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<AquariumData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }

    public void getFishInAquarium(){
        AquaApplicationService aquaApplicationService = RetrofitInstance.getService();
        Call<FishData> call = aquaApplicationService.getFishesByAquariumId(aquarium.getId());
        call.enqueue(new Callback<FishData>() {
            @Override
            public void onResponse(Call<FishData> call, Response<FishData> response) {
                FishData fishData = response.body();
                if(fishData != null && fishData.getData() != null){
                    List<Fish> result = fishData.getData();
                    Log.i(TAG, "Fishes in aquarium: ");
                    for(Fish fish : result){
                        Log.i(TAG, fish.toString());
                    }
                    fishList.clear();
                    fishList.addAll(result);
                    //UI render and given analysis
                    renderAnalysisPopUpView();
                }
            }
            @Override
            public void onFailure(Call<FishData> call, Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });
    }

    private void renderAnalysisPopUpView(){
        getDialogBuilder = new AlertDialog.Builder(getActivity());
        final View envAnalysisPopupView = getLayoutInflater().inflate(R.layout.recommendation_for_environment_popup, null);
        TextView fishTypeText = envAnalysisPopupView.findViewById(R.id.fish_types_details);
        TextView pHText = envAnalysisPopupView.findViewById(R.id.analysis_ph_text);
        TextView tempText = envAnalysisPopupView.findViewById(R.id.analysis_temp_text);
        TextView TdsText = envAnalysisPopupView.findViewById(R.id.analysis_tds_text);

        StringBuilder fishTypeBuilder = new StringBuilder();
        StringBuilder fishPhBuilder = new StringBuilder();
        StringBuilder fishTempBuilder = new StringBuilder();
        StringBuilder fishTdsBuilder = new StringBuilder();

        if(fishList != null && fishList.size() > 0){
            Fish firstFish = fishList.get(0);
            float idealMinPh = firstFish.getFishMinpH();
            float idealMaxPh = firstFish.getFishMaxpH();
            int idealMinTemp = firstFish.getFishMinTemperature();
            int idealMaxTemp = firstFish.getFishMaxTemperature();
            int idealMinTDS = firstFish.getFishMinTds();
            int idealMaxTDS = firstFish.getFishMaxTds();
            for (int i = 0; i < fishList.size(); i++) {
                Fish fish = fishList.get(i);
                fishTypeBuilder.append(fish.getFishName()).append(", ");
                //pH
                if(idealMinPh < fish.getFishMinpH()){
                    idealMinPh = fish.getFishMinpH();
                }
                if(idealMaxPh > fish.getFishMaxpH()){
                    idealMaxPh = fish.getFishMaxpH();
                }
                //temperature
                if(idealMinTemp < fish.getFishMinTemperature()){
                    idealMinTemp = fish.getFishMinTemperature();
                }
                if(idealMaxTemp > fish.getFishMaxTemperature()){
                    idealMaxTemp = fish.getFishMaxTemperature();
                }
                //Tds
                if(idealMinTDS < fish.getFishMinTds()){
                    idealMinTDS = fish.getFishMinTds();
                }
                if(idealMaxTDS > fish.getFishMaxTds()){
                    idealMaxTDS = fish.getFishMaxTds();
                }
            }
            //fish name
            if(fishTypeBuilder.length() > 2){
                fishTypeText.setText(fishTypeBuilder.substring(0, fishTypeBuilder.length() - 2));
            }
            //pH
            if(idealMinPh < idealMaxPh){
                fishPhBuilder.append("pH trong khoảng: ");
                fishPhBuilder.append(idealMinPh).append(" đến ").append(idealMaxPh);
                pHText.setText(fishPhBuilder.toString());
            } else {
                fishPhBuilder.append("pH mâu thuẫn với các loài cá: ");
                float finalIdealMaxPh = idealMaxPh;
                float finalIdealMinPh = idealMinPh;
                List<Fish> fishesCannotTogether = fishList.stream()
                        .filter(s -> s.getFishMinpH() == finalIdealMinPh || s.getFishMaxpH() == finalIdealMaxPh)
                        .collect(Collectors.toList());
                if(fishesCannotTogether.size() > 0){
                    for(Fish fish : fishesCannotTogether){
                        fishPhBuilder.append(fish.getFishName()).append(", ");
                    }
                    pHText.setText(fishPhBuilder.substring(0, fishPhBuilder.length()-2));
                }
            }
            //Temperature
            if(idealMinTemp < idealMaxTemp){
                fishTempBuilder.append("Nhiệt độ trong khoảng: ");
                fishTempBuilder.append(idealMinTemp).append("°C đến ").append(idealMaxTemp).append("°C");
                tempText.setText(fishTempBuilder.toString());
            } else {
                fishTempBuilder.append("Nhiệt độ mâu thuẫn với các loài cá: ");
                int finalIdealMaxTemp = idealMaxTemp;
                int finalIdealMinTemp = idealMinTemp;
                List<Fish> fishesCannotTogether = fishList.stream()
                        .filter(s -> s.getFishMaxTemperature() == finalIdealMaxTemp || s.getFishMinTemperature() == finalIdealMinTemp)
                        .collect(Collectors.toList());
                if(fishesCannotTogether.size() > 0){
                    for(Fish fish : fishesCannotTogether){
                        fishTempBuilder.append(fish.getFishName()).append(", ");
                    }
                    tempText.setText(fishTempBuilder.substring(0, fishTempBuilder.length()-2));
                }
            }
            //tds
            if(idealMinTDS < idealMaxTDS){
                fishTdsBuilder.append("TDS trong khoảng: ");
                fishTdsBuilder.append(idealMinTDS);
                fishTdsBuilder.append(" ppm đến ").append(idealMaxTDS).append(" ppm");
                TdsText.setText(fishTdsBuilder.toString());
            } else {
                fishTdsBuilder.append("TDS mâu thuẫn với các loài cá: ");
                int finalIdealMaxTDS = idealMaxTDS;
                int finalIdealMinTDS = idealMinTDS;
                List<Fish> fishesCannotTogether = fishList.stream()
                        .filter(s -> s.getFishMaxTds() == finalIdealMaxTDS || s.getFishMinTds() == finalIdealMinTDS)
                        .collect(Collectors.toList());
                if(fishesCannotTogether.size() > 0){
                    for(Fish fish : fishesCannotTogether){
                        fishTdsBuilder.append(fish.getFishName()).append(", ");
                    }
                    TdsText.setText(fishTdsBuilder.substring(0, fishTdsBuilder.length()-2));
                }
            }
        }
        dialog = getDialogBuilder.create();
        dialog.setView(envAnalysisPopupView);
        dialog.show();
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
        //Get information about the fish in aquarium
        startGetFishData(deviceName);
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
            Toast.makeText(getActivity(), "Đã xảy ra lỗi, có thể do tên WiFi hoặc mật khẩu bị nhập sai!",
                    Toast.LENGTH_SHORT).show();
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