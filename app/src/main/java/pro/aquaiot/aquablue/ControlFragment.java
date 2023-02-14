package pro.aquaiot.aquablue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ControlFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ControlFragment extends Fragment implements ServiceConnection, SerialListener {
    private enum Connected {False, Pending, True}
    //Services members
    private String deviceAddress;
    private SerialService service;
    private ControlFragment.Connected connected = ControlFragment.Connected.False;
    private boolean initialStart = true;
    private String newline = TextUtil.newline_crlf;
    private boolean pendingNewLine = false;

    //UI member
    private TextView wifiStatusText;
    private TextView bluetoothStatusText;
    private TextView messageText;
    private EditText ssidText;
    private EditText passwordText;
    private Button submitWifiBtn;
    private Button changeUIButton;

    /*
    * Life cycle
    * */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
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
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
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
        try { getActivity().unbindService(this); } catch (Exception ignored){}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null){
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    /*
     * UI
     * */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_control, container, false);
        wifiStatusText = view.findViewById(R.id.cp_wifi_status_txt);
        bluetoothStatusText = view.findViewById(R.id.cp_bluetooth_status_txt);
        messageText = view.findViewById(R.id.message_txt);
        submitWifiBtn = view.findViewById(R.id.wifi_submit_btn);
        ssidText = view.findViewById(R.id.SSID_edit_text);
        passwordText = view.findViewById(R.id.etxt_password);
        changeUIButton = view.findViewById(R.id.debug_mode_btn);
        wifiStatusText.setText(R.string.off_status);
        bluetoothStatusText.setText(R.string.off_status);
        ssidText.setText("Lethuy_2.4Ghz");
        passwordText.setText("11336688");
        submitWifiBtn.setOnClickListener(v -> send(getWifiJson()));
        return view;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        service = ((SerialService.SerialBinder) iBinder).getService();
        service.attach(this);
        if(initialStart && isResumed()){
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        service = null;
    }

    private void connect(){
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e){
            onSerialConnectError(e);
        }
    }

    private String getWifiJson(){
        //{"method":"ActivateWifi", "SSID":"ThaoAn/2.4G", "Password":"123456789"}
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("method", "ActivateWifi");
            jsonObject.put("SSID", ssidText.getText().toString());
            jsonObject.put("Password", passwordText.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    private void status(String str) {
        messageText.append(str + newline);
    }

    private void send(String str){
        if(connected != Connected.True){
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            msg = str;
            data = (str + newline).getBytes(StandardCharsets.UTF_8);
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + "/n");
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)),
                    0,
                    spn.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            status(spn.toString());
            service.write(data);
        } catch (Exception e){
            onSerialIoError(e);
        }
    }

    private void disconnect(){
        connected = ControlFragment.Connected.False;
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
                    } else {
                        Editable edt = messageText.getEditableText();
                        if (edt != null && edt.length() >= 2)
                            edt.delete(edt.length() - 2, edt.length());
                    }
                }
                pendingNewLine = msg.charAt(msg.length() - 1) == '\r';
            }
            spn.append(TextUtil.toCaretString(msg, newline.length() != 0));

        }
        messageText.append(spn);
        if(spn.toString().contains("WiFi connected"))
            wifiStatusText.setText(R.string.on_status);
        else
            wifiStatusText.setText(R.string.off_status);
    }

    /*
     * Serial Listener services implementation
     * */

    @Override
    public void onSerialConnect() {
        status("connected");
        connected = ControlFragment.Connected.True;
        bluetoothStatusText.setText(R.string.on_status);
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

    public ControlFragment() {
        // Required empty public constructor
    }

    public static ControlFragment newInstance() {
        ControlFragment fragment = new ControlFragment();
        return fragment;
    }
}