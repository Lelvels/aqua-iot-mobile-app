package pro.aquaiot.aquablue;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class DevicesFragment extends ListFragment {
    private final String TAG = "DeviceFragment";
    private BluetoothAdapter bluetoothAdapter;
    private final ArrayList<BluetoothDevice> listItems = new ArrayList<>();
    private ArrayAdapter<BluetoothDevice> listAdapter;
    ActivityResultLauncher<String> requestBluetoothPermissionLauncherForRefresh;
    private Menu menu;
    private boolean permissionMissing;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if(getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        listAdapter = new ArrayAdapter<BluetoothDevice>(getActivity(), 0, listItems){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                BluetoothDevice device = listItems.get(position);
                if(convertView == null)
                    convertView= getActivity().getLayoutInflater().inflate(R.layout.device_list_item, parent, false);
                @SuppressLint("MissingPermission") String deviceName = device.getName();
                TextView text_device_name = convertView.findViewById(R.id.text_device_name);
                TextView text_mac_address = convertView.findViewById(R.id.text_mac_address);
                text_device_name.setText(deviceName);
                text_mac_address.setText(device.getAddress());
                return convertView;
            }
        };
        requestBluetoothPermissionLauncherForRefresh = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> BluetoothUlti.onPermissionResult(this, granted, this::refresh)
        );
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.menu_devices, menu);
        if(permissionMissing)
            menu.findItem(R.id.bt_refresh).setVisible(true);
        if(bluetoothAdapter == null)
            menu.findItem(R.id.bt_settings).setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.bt_settings){
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
            return true;
        } else if(id == R.id.bt_refresh){
            if(BluetoothUlti.hasPermissions(this, requestBluetoothPermissionLauncherForRefresh))
                refresh();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(null);
        View header = getActivity().getLayoutInflater().inflate(R.layout.device_list_header, null, false);
        getListView().addHeaderView(header, null, false);
        setEmptyText("initializing...");
        ((TextView) getListView().getEmptyView()).setTextSize(18);
        setListAdapter(listAdapter);
    }

    @SuppressLint("MissingPermission")
    void refresh() {
        listItems.clear();
        if(bluetoothAdapter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissionMissing =
                        getActivity().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED;
                if(menu != null && menu.findItem(R.id.bt_refresh) != null)
                    menu.findItem(R.id.bt_refresh).setVisible(permissionMissing);
            }
            if(!permissionMissing) {
                for (BluetoothDevice device : bluetoothAdapter.getBondedDevices())
                    if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE)
                        listItems.add(device);
                Collections.sort(listItems, BluetoothUlti::compareTo);
            }
        }
        if(bluetoothAdapter == null)
            setEmptyText("<bluetooth not supported>");
        else if(!bluetoothAdapter.isEnabled())
            setEmptyText("<bluetooth is disabled>");
        else if(permissionMissing)
            setEmptyText("<permission missing, use REFRESH>");
        else
            setEmptyText("<no bluetooth devices found>");
        listAdapter.notifyDataSetChanged();
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        BluetoothDevice device = listItems.get(position-1);
        Bundle args = new Bundle();
        args.putString("device_address", device.getAddress());
        args.putString("device_name", device.getName());
        //Fragment fragment = new ControlFragment();
        Fragment fragment = new UiControl();
        fragment.setArguments(args);
        getParentFragmentManager().beginTransaction().replace(R.id.fragment, fragment, "UiControl").addToBackStack(null).commit();
    }
}