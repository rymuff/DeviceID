package com.kweisa.deviceid;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;

import java.lang.reflect.Method;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {
    private SimpleAdapter simpleAdapter;
    private ArrayList<HashMap<String, String>> list = new ArrayList<>();
    private String[] keyList;
    private final int READ_PHONE_STATE = 0;
    private boolean permissionGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String text = "";
                for (HashMap hashMap : list) {
                    text += hashMap.get("key") + ": " + hashMap.get("value") + "\n";
                }
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, text);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
            }
        });

        keyList = new String[]{getString(R.string.imei), getString(R.string.imsi), getString(R.string.mac_address), getString(R.string.serial_number), getString(R.string.android_id), getString(R.string.advertising_id)};

        ListView listView = (ListView) findViewById(R.id.list_view);
        simpleAdapter = new SimpleAdapter(this, list, R.layout.list_item, new String[]{"key", "value"}, new int[]{R.id.textView1, R.id.textView2});

        listView.setAdapter(simpleAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipdata = ClipData.newPlainText(list.get(i).get("key"), list.get(i).get("value"));
                clipboardManager.setPrimaryClip(clipdata);
                Toast.makeText(getApplicationContext(), R.string.copy_clipboard, Toast.LENGTH_SHORT).show();
            }
        });

        for (String key : keyList) {
            HashMap<String, String> item = new HashMap<>();
            item.put("key", key);
            item.put("value", getString(R.string.not_found));
            list.add(item);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE);
        } else {
            permissionGranted = true;
        }

        new UpdateTask().execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case READ_PHONE_STATE: {
                permissionGranted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            }
        }
        new UpdateTask().execute();
    }

    private class UpdateTask extends AsyncTask<Void, Void, Void> {
        private final int IMEI = 0;
        private final int IMSI = 1;
        private final int MAC_ADDRESS = 2;
        private final int SERIAL_NUMBER = 3;
        private final int ANDROID_ID = 4;
        private final int ADVERTISING_ID = 5;

        private void updateItem(int key, String value) {
            if (value == null) {
                value = getString(R.string.not_found);
            }
            HashMap<String, String> item = new HashMap<>();
            item.put("key", keyList[key]);
            item.put("value", value);
            list.set(key, item);
        }

        private String getMacAddress() {
            try {
                List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface nif : all) {
                    if (!nif.getName().equalsIgnoreCase("wlan0"))
                        continue;
                    byte[] macBytes = nif.getHardwareAddress();
                    if (macBytes == null) {
                        return "";
                    }
                    StringBuilder res1 = new StringBuilder();
                    for (byte b : macBytes) {
                        res1.append(String.format("%02X:", b));
                    }
                    if (res1.length() > 0) {
                        res1.deleteCharAt(res1.length() - 1);
                    }
                    return res1.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "02:00:00:00:00:00";
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String imei = null;
            String imsi = null;
            String macAddress = null;
            String serialNumber = null;
            String androidId = null;
            String advertisingId = null;
            try {
                if (permissionGranted) {
                    imei = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                    imsi = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getSubscriberId();
                } else {
                    imei = getString(R.string.permission_required);
                    imsi = getString(R.string.permission_required);
                }
                macAddress = getMacAddress();
                Class<?> c = Class.forName("android.os.SystemProperties");
                Method get = c.getMethod("get", String.class, String.class);
                serialNumber = (String) (get.invoke(c, "ro.serialno", null));
                androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                advertisingId = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext()).getId();
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateItem(IMEI, imei);
            updateItem(IMSI, imsi);
            updateItem(MAC_ADDRESS, macAddress);
            updateItem(SERIAL_NUMBER, serialNumber);
            updateItem(ANDROID_ID, androidId);
            updateItem(ADVERTISING_ID, advertisingId);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            simpleAdapter.notifyDataSetChanged();
            super.onPostExecute(aVoid);
        }
    }
}
