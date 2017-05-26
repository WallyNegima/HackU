package com.example.wally_nagama.paripigrass;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity implements Runnable, View.OnClickListener {

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
                    valueMsg.obj = readMsg;//
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
            //接続されていない場合のみ//
            if(!connectFlg) {
                mStatusTextView.setText("try connect");

                mThread = new Thread(this);
                isRunning = true;
                mThread.start();

            }
        } else if(v.equals(writeButton)) {
            //接続中のみ書き込みを行う//
            if(connectFlg) {
                try {
                    //---------------書き込み
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
