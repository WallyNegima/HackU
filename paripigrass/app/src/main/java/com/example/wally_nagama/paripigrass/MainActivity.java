package com.example.wally_nagama.paripigrass;

import android.app.ActionBar;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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
import android.widget.ListView;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_FINISHED;
import static android.bluetooth.BluetoothAdapter.ACTION_DISCOVERY_STARTED;
import static android.bluetooth.BluetoothDevice.ACTION_FOUND;
import static android.bluetooth.BluetoothDevice.ACTION_NAME_CHANGED;

public class MainActivity extends AppCompatActivity implements Runnable, View.OnClickListener {

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int REQUEST_ENABLE_BT = 6;
    
    User user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    Button button, roomCreateButton, btListButton, lightOnLed, lightOffLed;
    EditText editText, userName, roomNumber;
    TextView btText, deviceText;
    int userNum;
    Context act = this;
    ChildEventListener childEventListener;
    String key;
    BluetoothAdapter btAdapter;
    BlueToothReceiver btReceiver;
    List<BluetoothDevice> devices1;
    ArrayList<String> itemArray = new ArrayList<String>();
    ArrayList<String> mArrayAdapter = new ArrayList<String>();
    final List<Integer> checkedItems = new ArrayList<>();  //選択されたアイテム

    /* tag */
    private static final String TAG = "BluetoothSample";

    /* Bluetooth Adapter */
    private BluetoothAdapter mAdapter;

    /* Bluetoothデバイス */
    private BluetoothDevice mDevice;

    /* Bluetooth UUID */
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /* デバイス名 */
    private final String DEVICE_NAME = "RN52-FF5A";

    /* Soket */
    private BluetoothSocket mSocket;

    /* Thread */
    private Thread mThread;

    /* Threadの状態を表す */
    private boolean isRunning;

    /** 接続ボタン. */
    private Button connectButton;

    /** 書込みボタン. */
    private Button writeButton;

    /** ステータス. */
    private TextView mStatusTextView;

    /** Bluetoothから受信した値. */
    private TextView mInputTextView;

    /** Action(ステータス表示). */
    private static final int VIEW_STATUS = 0;

    /** Action(取得文字列). */
    private static final int VIEW_INPUT = 1;

    /** Connect確認用フラグ */
    private boolean connectFlg = false;

    /** BluetoothのOutputStream. */
    OutputStream mmOutputStream = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //テストボタン
        button = (Button)findViewById(R.id.button);
        editText = (EditText)findViewById(R.id.edittext);

        roomCreateButton = (Button)findViewById(R.id.userCreate); //部屋を作る･退出する
        btListButton = (Button)findViewById(R.id.btbutton); //ペアリングしているbt機器をダイアログで表示･接続する機器を選択
        lightOnLed = (Button)findViewById(R.id.amin_light_on_led); //LEDを光らせる
        lightOffLed = (Button)findViewById(R.id.amin_light_off_led); //LEDoffにする
        userName = (EditText)findViewById(R.id.userName); //部屋に入る時のユーザー名
        roomNumber = (EditText)findViewById(R.id.roomNumber); //入る部屋の番号
        btText = (TextView)findViewById(R.id.bt_text); //ペアリングしている機器一覧
        deviceText = (TextView)findViewById(R.id.device); //わからない
        userNum = 0;
        devices1 = new ArrayList<>();

        user = new User();

        //--------------BlueToothLED
        mInputTextView = (TextView)findViewById(R.id.inputValue);
        mStatusTextView = (TextView)findViewById(R.id.statusValue);
        connectButton = (Button)findViewById(R.id.connectButton);
        writeButton = (Button)findViewById(R.id.writeButton);
        connectButton.setOnClickListener(this);
        writeButton.setOnClickListener(this);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mStatusTextView.setText("SearchDevice");
        Set<BluetoothDevice> devices = mAdapter.getBondedDevices();
        for(BluetoothDevice device: devices) {
            if(device.getName().equals(DEVICE_NAME)) {
                mStatusTextView.setText("find:" + device.getName());
                mDevice = device;
            }
        }

//      BlueTooth取得
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter.isEnabled()) {
            Log.d("bt", "bt ok");
        }else{
            Log.d("bt", "bt ng");
//          BlueToothの有効化を要求
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        btListButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                itemArray.clear();
                // ペアリング済みのデバイス一覧を取得
                Set<BluetoothDevice> btDevices = btAdapter.getBondedDevices();
                if(btDevices.size() > 0) {
                    for (BluetoothDevice device1 : btDevices) {
                        itemArray.add(device1.getName());
                        devices1.add(device1);
                        mArrayAdapter.add(device1.getName() + "\n" + device1.getAddress());

                    }
                }
                //接続するデバイスを選択してもらうためにダイアログを表示
                String[] items = (String[])itemArray.toArray(new String[0]);
                int defaultItem = 0; // デフォルトでチェックされているアイテム
                checkedItems.add(defaultItem);
                new AlertDialog.Builder(act)
                        .setTitle("Selector") //ダイアログのタイトル
                        .setSingleChoiceItems(items, defaultItem, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //選択したら．．．
                                checkedItems.clear();
                                checkedItems.add(which); //checkedItems の0番目に選んだデバイスのindexが入る
                            }
                        })
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (!checkedItems.isEmpty()) {
                                    Log.d("checkedItem:", "" + checkedItems.get(0));
                                    deviceText.setText(devices1.get(checkedItems.get(0)).getName());
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
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

        /*-------------------------　LEDを光らす------------------*/
        lightOnLed.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //BlueToothが繋がっているか確認
                if(btAdapter == null)
                {
                    //Show a mensag. that thedevice has no bluetooth adapter
                    Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
                    //finish apk
                    finish();
                }
                else
                {
                    if (btAdapter.isEnabled())
                    { }
                    else
                    {
                        //Ask to the user turn the bluetooth on
                        Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(turnBTon,1);
                    }
                }
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


    @Override
    protected void onPause() {
        super.onPause();

        isRunning = false;
        try {
            mSocket.close();
        } catch (Exception e) {}
    }

    @Override
    public void run() {
        InputStream mmlnStream = null;
        Message valueMsg = new Message();
        valueMsg.what = VIEW_STATUS;
        valueMsg.obj = "connecting...";
        mHandler.sendMessage(valueMsg);


        try {

            // 取得したデバイス名を使ってBlueToothでSocket通信
            mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
            mSocket.connect();
            mmlnStream = mSocket.getInputStream();
            mmOutputStream = mSocket.getOutputStream();

            //InputStreamのバッファを格納
            byte[] buffer = new byte[1024];

            //習得したバッファのサイズを格納
            int bytes;
            valueMsg = new Message();
            valueMsg.what = VIEW_STATUS;
            valueMsg.obj = "connected...";
            mHandler.sendMessage(valueMsg);

            connectFlg = true;

            while(isRunning) {
                //InputStream の読み込み
                bytes = mmlnStream.read(buffer);
                Log.i(TAG, "bytes=" + bytes);

                //String型に変換
                String readMsg = new String(buffer, 0, bytes);

                //null以外なら表示
                if(readMsg.trim() != null && !readMsg.trim().equals("")) {
                    Log.i(TAG, "value=" + readMsg.trim());

                    valueMsg = new Message();
                    valueMsg.what = VIEW_INPUT;
                    valueMsg.obj = readMsg;
                    mHandler.sendMessage(valueMsg);
                } else {
                    Log.i(TAG, "value = nodata");
                }
            }
        } catch (Exception e) {
            valueMsg = new Message();
            valueMsg.what = VIEW_STATUS;
            valueMsg.obj = "Error1:" + e;
            mHandler.sendMessage(valueMsg);

            try {
                mSocket.close();
            } catch (Exception ee) {}
            isRunning = false;
            connectFlg = false;
        }
    }

    @Override
    public void onClick(View v) {
        if(v.equals(connectButton)) {
            //説ぞ↑うされていない場合のみ
            if(!connectFlg) {
                mStatusTextView.setText("try connect");

                mThread = new Thread(this);
                isRunning = true;
                mThread.start();

            }
        } else if(v.equals(writeButton)) {
            //接続中のみ書き込みを行う
            if(connectFlg) {
                try {
                    mmOutputStream.write("2".getBytes());
                    mStatusTextView.setText("Write");
                } catch (IOException e) {
                    Message valueMsg = new Message();
                    valueMsg.what = VIEW_STATUS;
                    valueMsg.obj = "Error3:" + e;
                    mHandler.sendMessage(valueMsg);
                }
            } else {
                mStatusTextView.setText("Please push the connect button");
            }
        }
    }

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int action = msg.what;
            String msgStr = (String)msg.obj;
            if(action == VIEW_INPUT) {
                mInputTextView.setText(msgStr);
            } else if(action == VIEW_STATUS) {
                mStatusTextView.setText(msgStr);
            }
        }
    };
}
