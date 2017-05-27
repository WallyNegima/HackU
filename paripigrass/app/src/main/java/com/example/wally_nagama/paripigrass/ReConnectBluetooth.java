package com.example.wally_nagama.paripigrass;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by horitadaichi on 2017/05/27.
 */

public class ReConnectBluetooth {

    public static final String TAG = "Bluetooth";
    public static final int VIEW_STATUS = 0;
    public static final int VIEW_INPUT = 1;
    public BluetoothSocket mSocket;
    public final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public BluetoothDevice mDevice;
    public OutputStream mmOutputStream = null;
    public boolean connectFlg = false;
    public boolean isRunning;
    public TextView mInputTextView, mStatusTextView;



/*
    public ReConnectBluetooth(TextView mInputTextView, TextView mStatusTextView) {
        this.mInputTextView = mInputTextView;
        this.mStatusTextView = mStatusTextView;

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int action = msg.what;
                String msgStr = (String)msg.obj;
                if(action == VIEW_INPUT) {
                    this.mInputTextView.setText(" " + msgStr);
                } else if(action == VIEW_STATUS) {
                    this.mStatusTextView.setText(msgStr);
                }
            }
        };

    }
*/

    public void ReConnectSocket () {

        InputStream mmlnStream = null;
        Message valueMsg = new Message();
        valueMsg.what = VIEW_STATUS;
        valueMsg.obj = "connecting...";
        //sendMessage(valueMsg);
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
            //mHandler.sendMessage(valueMsg);

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
                    //mHandler.sendMessage(valueMsg);
                } else {
                    Log.i(TAG, "value = nodata");
                }
            }
        } catch (Exception e) {
            valueMsg = new Message();
            valueMsg.what = VIEW_STATUS;
            valueMsg.obj = "Error1:" + e;
            //mHandler.sendMessage(valueMsg);

            try {
                mSocket.close();
            } catch (Exception ee) {}
            isRunning = false;
            connectFlg = false;
        }
    }
}
