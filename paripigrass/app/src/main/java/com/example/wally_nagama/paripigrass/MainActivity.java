package com.example.wally_nagama.paripigrass;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

public class MainActivity extends AppCompatActivity {
    User user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    Button button, roomCreateButton, kanpaiButton;
    EditText editText, userName, roomNumber;
    Context act = this;
    ChildEventListener childEventListener;
    String key;
    int color;
    TextView test_tv;
    int removedUserId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.button);
        roomCreateButton = (Button)findViewById(R.id.userCreate);
        kanpaiButton = (Button)findViewById(R.id.kanpai);
        editText = (EditText)findViewById(R.id.edittext);
        userName = (EditText)findViewById(R.id.userName);
        roomNumber = (EditText)findViewById(R.id.roomNumber);
        test_tv = (TextView)findViewById(R.id.test_tv);

        user = new User();

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
//                user.now_color = Integer.valueOf(editText.getText().toString());
                Random rnd = new Random();
                myRef.child("Roulette").child("light_now").setValue(1);
                myRef.child("Roulette").child("count").setValue(rnd.nextInt(5)+10);
            }
        });

        //roomを作成する
        roomCreateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(user.joined == false){
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
                            if(dataSnapshot.child("userId").getValue() != null && !dataSnapshot.getKey().equals(user.userKey)){
                                if (user.nextUserId == 1 && count > user.userId){
                                    user.nextUserId = user.userId + 1;
                                    Log.d("nextUserID","at 151:: "+user.nextUserId);
                                }
                            }

//                            ルーレットON
                            if(dataSnapshot.getKey().equals("Roulette")){
                                Log.d("Roulette","Added");
                                if (dataSnapshot.child("light_now").getValue(int.class) == user.userId){
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
                            if(dataSnapshot.getKey().equals("Roulette")){
                                if (dataSnapshot.child("light_now").getValue(int.class) == user.userId){
                                    final int count = dataSnapshot.child("count").getValue(int.class);
//                                    TODO::ピカピカ〜
                                    test_tv.setText("hfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhfhf");
//                                    countが0やったら終了
                                    if (count > 0){
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
                                                        myRef.child("Roulette").child("count").setValue(count-1);
                                                        test_tv.setText("");
                                                    }
                                                });
                                            }
                                        }, 100);
                                    }else{
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
//                                        Roulette削除
                                                myRef.child("Roulette").removeValue();
                                            }
                                        },2000);
                                    }
                                }
                            }

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {
                            //userが部屋からいなくなったときの処理
                            if(dataSnapshot.child("userId").getValue() != null) {
                                removedUserId = dataSnapshot.child("userId").getValue(int.class);
                                myRef.child(user.userKey).child("userId").runTransaction(new Transaction.Handler() {
                                    @Override
                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                            if(mutableData.getValue(int.class) > removedUserId ){
                                                //自分よりも先に部屋に入った人が抜けたらuserId nextUserIdをデクリメント
                                                user.userId--;
                                                if (user.nextUserId > 1){
                                                    user.nextUserId--;
                                                }else{

                                                }
                                                Log.d("nextUserID","at 181:: "+user.nextUserId);
                                                mutableData.setValue(user.userId);
                                            }else if(removedUserId == mutableData.getValue(int.class)+1 ){
//                                                自分の次が削除のとき
//                                                そいつがケツやったらnextUserIDを1にする
                                                myRef.child("numberOfUser").runTransaction(new Transaction.Handler() {
                                                    @Override
                                                    public Transaction.Result doTransaction(MutableData mutableData) {
                                                        if(user.userId == mutableData.getValue(int.class)){
                                                            user.nextUserId = 1;
                                                            Log.d("nextUserID","at 190:: "+user.nextUserId);
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
                    if(user.joined){
                        //すでに部屋に入っているので何もしない
                    }else{
                        user.joined = true;
                        key = myRef.push().getKey();
                        user.userKey = key;
                        registUserID(myRef,user);
                        user.userName = userName.getText().toString();
                        myRef.addChildEventListener(childEventListener);
                        roomCreateButton.setText("部屋を退出する");
                    }
                }else if(user.joined == true){
                    //すでに部屋に入っているときの処理
                    //退出する
                    //部屋の人数 numberOfUserをデクリメントして，自分自身のremoveする．
                    myRef.child("numberOfUser").runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            int temp = mutableData.getValue(int.class) -1;
                            if (temp == 0){
                                myRef.removeValue();
                                return null;
                            }
                            mutableData.setValue(temp);

                            return Transaction.success(mutableData);
                        }
                        @Override
                        public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {}
                    });
                    myRef.child(user.userKey).removeValue();

                    myRef.removeEventListener(childEventListener);
                    roomCreateButton.setText("JOIN ROOM");
                    user.joined = false;
                }
            }
        });

        //テスト
        //乾杯
        kanpaiButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                user.kanpai_gotNewColor = false;
                myRef.child("now_color").runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        if(mutableData.getValue() == null || mutableData.getValue(int.class) == 0){
                            mutableData.setValue(user.now_color);
                            myRef.child("now_color").addListenerForSingleValueEvent(
                                    new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {
                                            // Get user value
                                            if(dataSnapshot.getKey().equals("now_color")){
                                                if(dataSnapshot.getValue(int.class) != 0){
                                                    user.now_color = dataSnapshot.getValue(int.class);
                                                    test_tv.setText(user.now_color + "!");
                                                    user.kanpai_gotNewColor = true;
                                                }
                                            }
                                        }
                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {
                                            Log.w("kanpai", "getUser:onCancelled", databaseError.toException());
                                        }
                                    });
                            //誰も乾杯してくれなかった時
                            HandlerThread handlerThread = new HandlerThread("foo");
                            handlerThread.start();
                            new Handler(handlerThread.getLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    // ここにn秒後に実行したい処理
                                    if(!user.kanpai_gotNewColor){
                                        myRef.child("now_color").runTransaction(new Transaction.Handler() {
                                            @Override
                                            public Transaction.Result doTransaction(MutableData mutableData) {
                                                mutableData.setValue(0);
                                                test_tv.setText("誰も乾杯せず");
                                                return Transaction.success(mutableData);
                                            }
                                            @Override
                                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                            }
                                        });
                                    }

                                }
                            }, 200);

                        }else{
                            int temp = user.now_color;
                            user.now_color = mutableData.getValue(int.class);
                            myRef.child("now_color").setValue(temp);
                            test_tv.setText(user.now_color + "!");
                            Log.d("kanpai", "add now_color!");
                        }
                        return Transaction.success(mutableData);
                    }
                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    }
                });
            }
        });
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
}
