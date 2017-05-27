package com.example.wally_nagama.paripigrass;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.annotation.CheckResult;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.Random;

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
    User user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    Button button, roomCreateButton, kanpaiButton, btListButton, twitterButton, tweetButton;
    EditText editText, userName, roomNumber, mDirection;
    Context act = this;
    ChildEventListener childEventListener;
    String key;
    int color;
    TextView test_tv, btdevicename;
    int removedUserId = 0;
    BluetoothAdapter btAdapter;
    BlueToothReceiver btReceiver;
    List<BluetoothDevice> devices1;
    ArrayList<String> itemArray = new ArrayList<String>();
    final List<Integer> checkedItems = new ArrayList<>();  //選択されたアイテム
    SharedPreferences preferences;

    /* twitter */
    private String mCallbackURL;
    private Twitter mTwitter;
    private RequestToken mRequestToken;
    public Tweet tweet;
    String NKANAPI = "numberOfKanapi";

    /* tag */
    private static final String TAG = "BluetoothSample";

    /* Bluetooth Adapter */
    private BluetoothAdapter mAdapter;

    /* Bluetoothデバイス */
    private BluetoothDevice mDevice;

    /* Bluetooth UUID */
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /* デバイス名 */
    //private final String DEVICE_NAME = "RN52-FF5A";

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

    /** 指示送る文字列 **/
    private String sendMessage;

    /* 音声認識で使うよーんwwwwww  */
    private TextView txvAction;
    private TextView txvRec;
    private static final int REQUEST_CODE = 0;
    private String result_voce;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button) findViewById(R.id.button);
        roomCreateButton = (Button) findViewById(R.id.userCreate);
        kanpaiButton = (Button) findViewById(R.id.kanpai);
        btListButton = (Button) findViewById(R.id.btdevice);
        twitterButton = (Button) findViewById(R.id.twitter);
        tweetButton = (Button) findViewById(R.id.tweet);
        editText = (EditText) findViewById(R.id.edittext);
        userName = (EditText) findViewById(R.id.userName);
        mDirection = (EditText)findViewById(R.id.amin_write_direction);
        roomNumber = (EditText) findViewById(R.id.roomNumber);
        test_tv = (TextView) findViewById(R.id.test_tv);
        btdevicename = (TextView) findViewById(R.id.btdevicename);

        user = new User();
        devices1 = new ArrayList<>();
        preferences = act.getSharedPreferences(NKANAPI, Context.MODE_PRIVATE);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                user.now_color = Integer.valueOf(editText.getText().toString());
                Random rnd = new Random();
                myRef.child("Roulette").child("light_now").setValue(1);
                myRef.child("Roulette").child("count").setValue(rnd.nextInt(5) + 10);
            }
        });

        //--------------BlueToothLED
        mInputTextView = (TextView) findViewById(R.id.inputValue);
        mStatusTextView = (TextView) findViewById(R.id.statusValue);
        connectButton = (Button) findViewById(R.id.connectButton);
        writeButton = (Button) findViewById(R.id.writeButton);
        connectButton.setOnClickListener(this);
        writeButton.setOnClickListener(this);
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mStatusTextView.setText("SearchDevice");

        //音声認識
        txvAction = (TextView) findViewById(R.id.amin_txvAction);
        txvRec = (TextView) findViewById(R.id.txv_recog);

        /*---    へーへーボタンリスナー ---*/
        findViewById(R.id.amin_heybutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txvAction.setText("へ〜〜〜〜！！！！");
                //Toast.makeText(context, "乾杯", Toast.LENGTH_SHORT).show();
            }
        });

        /*---     音声認識リスナー   ----*/
        findViewById(R.id.amin_recog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // 音声認識プロンプトを立ち上げるインテント作成
                    Intent intent = new Intent(
                            RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    // 言語モデル： free-form speech recognition
                    // web search terms用のLANGUAGE_MODEL_WEB_SEARCHにすると検索画面
                    intent.putExtra(
                            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    // プロンプトに表示する文字を設定
                    intent.putExtra(
                            RecognizerIntent.EXTRA_PROMPT,
                            "話せや");
                    // インテント発行
                    startActivityForResult(intent, REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    // エラー表示
                    Toast.makeText(MainActivity.this,
                            "ActivityNotFoundException", Toast.LENGTH_LONG).show();
                }
            }
        });

        /*---     ペアリング済みBT機器から接続する機器を選ぶボタン！   ----*/
        //デバイスを検索する
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btListButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                itemArray.clear();
                // ペアリング済みのデバイス一覧を取得
                Set<BluetoothDevice> btDevices = btAdapter.getBondedDevices();
                for (BluetoothDevice device : btDevices) {
                    itemArray.add(device.getName());
                    devices1.add(device);
                }
                String[] items = (String[])itemArray.toArray(new String[0]);
                int defaultItem = 0; // デフォルトでチェックされているアイテム
                checkedItems.add(defaultItem);
                new AlertDialog.Builder(act)
                        .setTitle("接続する機器")
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
                                    btdevicename.setText(devices1.get(checkedItems.get(0)).getName());
                                    mDevice = devices1.get(checkedItems.get(0));
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });

        /*---    twitter認証！ ---*/
        mCallbackURL = getString(R.string.twitter_callback_url);
        mTwitter = TwitterUtils.getTwitterInstance(this);
        tweet = new Tweet(this, mTwitter);
        twitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TwitterUtils.hasAccessToken(act)) {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt(NKANAPI, 1);
                    editor.apply();
                    startAuthorize();
                } else {
                    showToast("もう認証されてるよ");
                }
            }
        });
        /*--  つぶやく！！    --*/
        tweetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putInt(NKANAPI, preferences.getInt(NKANAPI, 0)+1);
                editor.apply();
                tweet.tweet();
            }
        });


        //roomを作成する
        roomCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (user.joined == false) {
                    String roomId = roomNumber.getText().toString();
                    Room room = new Room(roomId);
                    myRef = database.getReference("room" + roomId);

                    //ユーザーのリストなどを見張る
                    childEventListener = new ChildEventListener() {
                        int count = 0;

                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                            count++;
//                            新規ユーザ追加時
                            if (dataSnapshot.child("userId").getValue() != null && !dataSnapshot.getKey().equals(user.userKey)) {
                                if (user.nextUserId == 1 && count > user.userId) {
                                    user.nextUserId = user.userId + 1;
                                    Log.d("nextUserID", "at 151:: " + user.nextUserId);
                                }
                            }

//                            ルーレットON
                            if (dataSnapshot.getKey().equals("Roulette")) {
                                Log.d("Roulette", "Added");
                                if (dataSnapshot.child("light_now").getValue(int.class) == user.userId) {
//                                    TODO::ピカピカ〜
                                    test_tv.setText("hfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhf");
                                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            myRef.child("Roulette").child("light_now").runTransaction(new Transaction.Handler() {
                                                @Override
                                                public Transaction.Result doTransaction(MutableData mutableData) {
                                                    mutableData.setValue(user.nextUserId);
                                                    return Transaction.success(mutableData);
                                                }

                                                @Override
                                                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
//                                         TODO   消す処理
                                                    test_tv.setText("");
                                                }
                                            });
                                        }
                                    }, 100);
                                }
                            }
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                            if (dataSnapshot.getKey().equals("Roulette")) {
                                if (dataSnapshot.child("light_now").getValue(int.class) == user.userId) {
                                    final int count = dataSnapshot.child("count").getValue(int.class);
//                                    TODO::ピカピカ〜
                                    test_tv.setText("hfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhf");
//                                    countが0やったら終了
                                    if (count > 0) {
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                myRef.child("Roulette").child("light_now").runTransaction(new Transaction.Handler() {
                                                    @Override
                                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                                        mutableData.setValue(user.nextUserId);
                                                        return Transaction.success(mutableData);
                                                    }

                                                    @Override
                                                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
//                                                      TODO   消す処理
                                                        myRef.child("Roulette").child("count").setValue(count - 1);
                                                        test_tv.setText("");
                                                    }
                                                });
                                            }
                                        }, 100);
                                    } else {
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
//                                        Roulette削除
                                                myRef.child("Roulette").removeValue();
                                            }
                                        }, 2000);
                                    }
                                }
                            }

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {
                            //userが部屋からいなくなったときの処理
                            if (dataSnapshot.child("userId").getValue() != null) {
                                removedUserId = dataSnapshot.child("userId").getValue(int.class);
                                myRef.child(user.userKey).child("userId").runTransaction(new Transaction.Handler() {
                                    @Override
                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                        if (mutableData.getValue(int.class) > removedUserId) {
                                            //自分よりも先に部屋に入った人が抜けたらuserId nextUserIdをデクリメント
                                            user.userId--;
                                            if (user.nextUserId > 1) {
                                                user.nextUserId--;
                                            } else {

                                            }
                                            Log.d("nextUserID", "at 181:: " + user.nextUserId);
                                            mutableData.setValue(user.userId);
                                        } else if (removedUserId == mutableData.getValue(int.class) + 1) {
//                                                自分の次が削除のとき
//                                                そいつがケツやったらnextUserIDを1にする
                                            myRef.child("numberOfUser").runTransaction(new Transaction.Handler() {
                                                @Override
                                                public Transaction.Result doTransaction(MutableData mutableData) {
                                                    if (user.userId == mutableData.getValue(int.class)) {
                                                        user.nextUserId = 1;
                                                        Log.d("nextUserID", "at 190:: " + user.nextUserId);
                                                    }
                                                    return null;
                                                }

                                                @Override
                                                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                                                    if (b) {
                                                        String logMessage = dataSnapshot.getValue().toString();
                                                        Log.d("testRunTran1", "counter: " + logMessage);
                                                    } else {
                                                        Log.d("testRunTran1", databaseError.getMessage(), databaseError.toException());
                                                    }
                                                }
                                            });
                                        }
                                        return Transaction.success(mutableData);
                                    }

                                    @Override
                                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                        if (b) {
                                        } else {
                                            Log.d("testRunTran", databaseError.getMessage(), databaseError.toException());
                                        }
                                    }
                                });
                            }
                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.w("a", "postComments:onCancelled", databaseError.toException());
                            Toast.makeText(act, "Failed to load comments.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    };
                    if (user.joined) {
                        //すでに部屋に入っているので何もしない
                    } else {
                        user.joined = true;
                        key = myRef.push().getKey();
                        user.userKey = key;
                        registUserID(myRef, user);
                        user.userName = userName.getText().toString();
                        myRef.addChildEventListener(childEventListener);
                        roomCreateButton.setText("部屋を退出する");
                    }
                } else if (user.joined == true) {
                    //すでに部屋に入っているときの処理
                    //退出する
                    //部屋の人数 numberOfUserをデクリメントして，自分自身のremoveする．
                    myRef.child("numberOfUser").runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            int temp = mutableData.getValue(int.class) - 1;
                            if (temp == 0) {
                                myRef.removeValue();
                                return null;
                            }
                            mutableData.setValue(temp);

                            return Transaction.success(mutableData);
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        }
                    });
                    myRef.child(user.userKey).removeValue();

                    myRef.removeEventListener(childEventListener);
                    roomCreateButton.setText("JOIN ROOM");
                    user.joined = false;
                }
            }
        });

//            ██╗  ██╗ █████╗ ███╗   ██╗██████╗  █████╗ ██╗
//            ██║ ██╔╝██╔══██╗████╗  ██║██╔══██╗██╔══██╗██║
//            █████╔╝ ███████║██╔██╗ ██║██████╔╝███████║██║
//            ██╔═██╗ ██╔══██║██║╚██╗██║██╔═══╝ ██╔══██║██║
//            ██║  ██╗██║  ██║██║ ╚████║██║     ██║  ██║██║
//            ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝     ╚═╝  ╚═╝╚═╝

        //テスト
        //乾杯
        kanpaiButton.setOnClickListener(new View.OnClickListener(){
            int old_color;
            int count;
            int temp_color;

            ChildEventListener ce;
            @Override
            public void onClick(View v){
                old_color = user.now_color;
                user.kanpai_gotNewColor = false;
                count = 0;
                ce = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
//                        追加された値の取得
                        String temp = dataSnapshot.getKey();
                        count ++;
                        if(!temp.equals(user.userKey) && count < 3){
//                            自分以外のkeyの値を書き換え
                            myRef.child("now_color").child(temp).setValue(old_color);
//                                    .runTransaction(new Transaction.Handler() {
//                                @Override
//                                public Transaction.Result doTransaction(MutableData mutableData) {
//                                    mutableData.setValue(user.now_color);
//                                    return Transaction.success(mutableData);
//                                }
//
//                                @Override
//                                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
//                                    if (b){
//                                        Log.d("kanpai::495",""+dataSnapshot);
//                                    }else{
//                                        Log.d("kanpai::497",""+databaseError);
//                                    }
//                                }
//                            });
                        }

                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };

                myRef.child("now_color").child(user.userKey).setValue(user.now_color);
                myRef.child("now_color").addChildEventListener(ce);


                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        myRef.child("now_color").child(user.userKey).runTransaction(new Transaction.Handler() {
                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
//                                値取得
                                temp_color = mutableData.getValue(int.class);
                                myRef.child("now_color").removeEventListener(ce);
                                return Transaction.success(mutableData);
                            }
                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                myRef.child("now_color").child(user.userKey).removeValue();
                                user.now_color = temp_color;
                                myRef.child(user.userKey).child("now_color").setValue(user.now_color);
                                if (b){
                                    Log.d("kanpai::560",""+dataSnapshot);
                                }else{
                                    Log.d("kanpai::562",""+databaseError);
                                }
                            }
                        });
                    }
                },500);
//                TODO::秒数を調整
            }
     });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("LifeCycle", "onDestroy");
        if (btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
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
                        // EditText(mDirection)からの文字列取得
                        sendMessage = mDirection.getText().toString();
                        // マイコンへ送る
                        mmOutputStream.write(sendMessage.getBytes());

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
                    mInputTextView.setText("　　" + msgStr);
                } else if(action == VIEW_STATUS) {
                    mStatusTextView.setText(msgStr);
                }
            }
        };


    /*---       startActivityForResultで起動したアクティビティが終了した時に呼び出される関数   ---*/
        @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            // 音声認識結果の時
            if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
                // 結果文字列リストを取得
                ArrayList<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                if (results.size() > 0) {
                    // 認識結果候補で一番有力なものを表示
                    txvRec.setText(results.get(0));
                    // checkCharacterに値を渡す
                    //checkResult.resultRec = results.get(0);
                    result_voce = results.get(0);
                /*
                /*---    この下に結果処理を一応描いてみる   ---*/
                    switch (result_voce) {
                /*---   乾杯   ---*/
                        case "乾杯します":
                            Toast.makeText(this, "乾杯！！", Toast.LENGTH_LONG).show();
                            break;
                        case "乾杯":
                            Toast.makeText(this, "乾杯！！", Toast.LENGTH_LONG).show();
                            try{
                                // "L"は光らせる
                                mmOutputStream.write("L".getBytes());
                                mStatusTextView.setText("L");
                            } catch (IOException e) {
                                Message valueMsg = new Message();
                                valueMsg.what = VIEW_STATUS;
                                valueMsg.obj = "Error3:" + e;
                                mHandler.sendMessage(valueMsg);
                            }
                            break;
                /*---   ルーレット   */
                        case "ルーレットモード":
                            Toast.makeText(this, "ルーレット", Toast.LENGTH_LONG).show();
                            Random rnd = new Random();
                            myRef.child("Roulette").child("light_now").setValue(1);
                            myRef.child("Roulette").child("count").setValue(rnd.nextInt(5) + 10);
                            break;
                        case "ルーレット":
                            Toast.makeText(this, "ルーレット", Toast.LENGTH_LONG).show();
                            Random rnd1 = new Random();
                            myRef.child("Roulette").child("light_now").setValue(1);
                            myRef.child("Roulette").child("count").setValue(rnd1.nextInt(5) + 10);
                            break;
                /*--   司会者   ---*/
                        case "司会者になりました":
                            Toast.makeText(this, "司会者になりました", Toast.LENGTH_LONG).show();
                            break;
                        case "司会者":
                            Toast.makeText(this, "司会者になりました", Toast.LENGTH_LONG).show();
                            break;
                /*---   一気飲み   ---*/
                        case "一気飲み":
                            Toast.makeText(this, "一気飲み", Toast.LENGTH_LONG).show();
                            break;
                        case "一気飲みします":
                            Toast.makeText(this, "一気飲み", Toast.LENGTH_LONG).show();
                            break;
                        case "赤色":


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

                            try{
                                Thread.sleep(500); //3000ミリ秒Sleepする
                            }catch(InterruptedException e){}





                            try{
                                mmOutputStream.write("red".getBytes());
                                //mStatusTextView.setText("red");
                            } catch (IOException e) {
                                Message valueMsg1 = new Message();
                                valueMsg1.what = VIEW_STATUS;
                                valueMsg1.obj = "Error3:" + e;
                                mHandler.sendMessage(valueMsg1);
                            }
                            break;
                    }
                }
            }

    }

    private void registUserID(final DatabaseReference databaseReference, final User user){
        //部屋に入る時，部屋の人数に合わせてuserIdを決める
        databaseReference.child("numberOfUser").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    mutableData.setValue(1);
                    user.userId = 1;
                    user.now_color = user.userId;
                }else{
                    int id = mutableData.getValue(int.class)+1;
                    mutableData.setValue(id);
                    user.userId = id;
                    user.now_color = user.userId;
                }
                databaseReference.child(user.userKey).child("userId").setValue(user.userId);
                myRef.child(key).child("userName").setValue(user.userName);
                user.nextUserId = 1;
                Log.d("nextUserID","at 297:: "+user.nextUserId);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
            }
        });
    }



    /**
     * OAuth認証（厳密には認可）を開始します。
     */
    private void startAuthorize() {
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    mRequestToken = mTwitter.getOAuthRequestToken(mCallbackURL);
                    return mRequestToken.getAuthorizationURL();
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String url) {
                if (url != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } else {
                    // 失敗。。。
                }
            }
        };
        task.execute();
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (intent == null
                || intent.getData() == null
                || !intent.getData().toString().startsWith(mCallbackURL)) {
            return;
        }
        String verifier = intent.getData().getQueryParameter("oauth_verifier");

        AsyncTask<String, Void, AccessToken> task = new AsyncTask<String, Void, AccessToken>() {
            @Override
            protected AccessToken doInBackground(String... params) {
                try {
                    return mTwitter.getOAuthAccessToken(mRequestToken, params[0]);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(AccessToken accessToken) {
                if (accessToken != null) {
                    // 認証成功！
                    showToast("認証成功！");
                    successOAuth(accessToken);
                } else {
                    // 認証失敗。。。
                    showToast("認証失敗。。。");
                }
            }
        };
        task.execute(verifier);
    }

    private void successOAuth(AccessToken accessToken) {
        TwitterUtils.storeAccessToken(this, accessToken);
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        //finish();
    }

    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}

