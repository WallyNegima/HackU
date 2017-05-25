package com.example.wally_nagama.paripigrass;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Comment;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.bluetooth.BluetoothDevice.ACTION_NAME_CHANGED;

public class MainActivity extends AppCompatActivity {
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    
    User user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    Button button, roomCreateButton, btListButton, btSearchButton, connectButton;
    EditText editText, userName, roomNumber;
    TextView btText, deviceText;
    int userNum;
    Context act = this;
    ChildEventListener childEventListener;
    String key;
    BluetoothAdapter btAdapter;
    BlueToothReceiver btReceiver;
    List<BluetoothDevice> devices;
    ArrayList<String> itemArray = new ArrayList<String>();
    final List<Integer> checkedItems = new ArrayList<>();  //選択されたアイテム
    String devList = "";
    private String deviceName;

    private static BluetoothResponseHandler mHandler;
    public DeviceConnector connector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.button);
        editText = (EditText)findViewById(R.id.edittext);

        roomCreateButton = (Button)findViewById(R.id.userCreate);
        btListButton = (Button)findViewById(R.id.btbutton);
        btSearchButton = (Button)findViewById(R.id.search_bt);
        connectButton = (Button)findViewById(R.id.connect);
        userName = (EditText)findViewById(R.id.userName);
        roomNumber = (EditText)findViewById(R.id.roomNumber);
        btText = (TextView)findViewById(R.id.bt_text);
        deviceText = (TextView)findViewById(R.id.device);
        userNum = 0;
        mHandler = new BluetoothResponseHandler(this);
        devices = new ArrayList<>();

        user = new User();

        //btlistを表示
        //デバイスを検索する
        btReceiver = new BlueToothReceiver(this, devList, btText);
        btReceiver.register();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter.isEnabled()) {
            Log.d("bt", "bt ok");
        }else{
            Log.d("bt", "bt ng");
        }

        btListButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                devList = "";
                itemArray.clear();
                // ペアリング済みのデバイス一覧を取得
                Set<BluetoothDevice> btDevices = btAdapter.getBondedDevices();
                for (BluetoothDevice device : btDevices) {
                    devList += device.getName() + "(" + getBondState(device.getBondState()) + ")\n";
                    itemArray.add(device.getName());
                    devices.add(device);
                }

                btText.setText(devList);
                String[] items = (String[])itemArray.toArray(new String[0]);
                int defaultItem = 0; // デフォルトでチェックされているアイテム
                checkedItems.add(defaultItem);
                new AlertDialog.Builder(act)
                        .setTitle("Selector")
                        .setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                checkedItems.clear();
                                checkedItems.add(which);
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!checkedItems.isEmpty()) {
                                    Log.d("checkedItem:", "" + checkedItems.get(0));
                                    deviceText.setText(devices.get(checkedItems.get(0)).getName());
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        btSearchButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!btAdapter.isDiscovering()){
                    btAdapter.startDiscovery();
                    Log.d("bt", "start!!");
                }else{
                    btAdapter.cancelDiscovery();
                    Log.d("bt", "stop");
                    btAdapter.startDiscovery();
                    Log.d("bt", "and start!!");
                }
            }
        });

        //roomを作成する
        roomCreateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String roomId = roomNumber.getText().toString();
                Room room = new Room(roomId);
                myRef = database.getReference("room" + roomId);
                userNum = 0;

                //roomのuser数を見て人数を数える
                //ユーザーのリストなどを見張る
                childEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildAdded:" + dataSnapshot);

                        // A new comment has been added, add it to the displayed list
                        userNum+=1;
                        // ...
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildChanged:" + dataSnapshot.getKey());

                        // A comment has changed, use the key to determine if we are displaying this
                        // comment and if so displayed the changed comment.
                        //Comment newComment = dataSnapshot.getValue(Comment.class);
                        //String commentKey = dataSnapshot.getKey();
                        // ...
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Log.d("a", "onChildRemoved:" + dataSnapshot.getKey());

                        // A comment has changed, use the key to determine if we are displaying this
                        // comment and if so remove it.
                        String commentKey = dataSnapshot.getKey();

                        // ...
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildMoved:" + dataSnapshot.getKey());

                        // A comment has changed position, use the key to determine if we are
                        // displaying this comment and if so move it.
                        Comment movedComment = dataSnapshot.getValue(Comment.class);
                        String commentKey = dataSnapshot.getKey();

                        // ...
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.w("a", "postComments:onCancelled", databaseError.toException());
                        Toast.makeText(act, "Failed to load comments.",
                                Toast.LENGTH_SHORT).show();
                    }
                };
                if(user.joined){
                    //すでに部屋に入っているので何もしない
                }else{
                    myRef.addChildEventListener(childEventListener);
                    user.joined = true;
                    key = myRef.push().getKey();
                    user.userName = userName.getText().toString();
                    user.userKey = key;
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // ここに1秒後に実行したい処理
                            Log.d("Firebase", String.format("usernum:%d", userNum));
                            myRef.child(key).child("userID").setValue(String.valueOf(userNum));
                            myRef.child(key).child("userName").setValue(user.userName);
                            user.userId = String.valueOf(userNum);
                        }
                    }, 500);
                }
            }
        });

        //機器とコネクトする
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupConnector(devices.get(checkedItems.get(0)));
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.v("LifeCycle", "onDestroy");
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }
        btReceiver.unregister();
    }

    String getBondState(int state) {
        String strState="";
        switch (state) {
            case BluetoothDevice.BOND_BONDED:
                strState = "接続履歴あり";
                break;
            case BluetoothDevice.BOND_BONDING:
                break;
            case BluetoothDevice.BOND_NONE:
                strState = "接続履歴なし";
                break;
            default :strState = "エラー";
        }
        return strState;
    }
    private void stopConnection() {
        if (connector != null) {
            connector.stop();
            connector = null;
            deviceName = null;
        }
    }

    private void setupConnector(BluetoothDevice connectedDevice) {
        stopConnection();
        try {
            String emptyName = "empty_device_name";
            DeviceData data = new DeviceData(connectedDevice, emptyName);
            connector = new DeviceConnector(data, mHandler);
            connector.connect();
        } catch (IllegalArgumentException e) {
            Utils.log("setupConnector failed: " + e.getMessage());
        }
    }

    private static class BluetoothResponseHandler extends Handler {
        private WeakReference<MainActivity> mActivity;

        public BluetoothResponseHandler(MainActivity activity) {
            mActivity = new WeakReference<MainActivity>(activity);
        }

        public void setTarget(MainActivity target) {
            mActivity.clear();
            mActivity = new WeakReference<MainActivity>(target);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if (activity != null) {
                switch (msg.what) {
                    case MESSAGE_STATE_CHANGE:

                        //Utils.log("MESSAGE_STATE_CHANGE: " + msg.arg1);
                        final ActionBar bar = activity.getActionBar();
                        switch (msg.arg1) {
                            case DeviceConnector.STATE_CONNECTED:
                                bar.setSubtitle("connected");
                                break;
                            case DeviceConnector.STATE_CONNECTING:
                                bar.setSubtitle("connecting");
                                break;
                            case DeviceConnector.STATE_NONE:
                                bar.setSubtitle("not_connected");
                                break;
                        }
                        activity.invalidateOptionsMenu();
                        break;

                    case MESSAGE_READ:
                        final String readMessage = (String) msg.obj;
                        if (readMessage != null) {
                            //activity.appendLog(readMessage, false, false, activity.needClean);
                        }
                        break;

                    case MESSAGE_DEVICE_NAME:
                        activity.deviceName = (String) msg.obj;
                        break;

                    case MESSAGE_WRITE:
                        // stub
                        break;

                    case MESSAGE_TOAST:
                        // stub
                        break;
                }
            }
        }
    }

}
