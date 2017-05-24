package com.example.wally_nagama.paripigrass;

import android.content.Context;
import android.os.Handler;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Comment;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    User user;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    Button button, roomCreateButton;
    EditText editText, userName, roomNumber;
    Context act = this;
    ChildEventListener childEventListener;
    String key;
    int color;
    TextView test_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.button);
        roomCreateButton = (Button)findViewById(R.id.userCreate);
        editText = (EditText)findViewById(R.id.edittext);
        userName = (EditText)findViewById(R.id.userName);
        roomNumber = (EditText)findViewById(R.id.roomNumber);
        test_tv = (TextView)findViewById(R.id.test_tv);

        user = new User();

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                user.now_color = Integer.valueOf(editText.getText().toString());
                myRef.child("prost_now").child(key).child("now_color").setValue(user.now_color);
                myRef.child("prost_now").child(key).child("next_color").setValue(user.now_color);
                final ChildEventListener ev =new ChildEventListener() {
                    int count = 0;
                    boolean is_first = false;
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Log.d("prost", "onChildAdded:All:" + dataSnapshot);
//                        自分が一番乗りのとき
                        if (count == 0 && dataSnapshot.getKey() == key){
                            is_first = true;
                        }
                        if(dataSnapshot.getKey() == key){
                            return;
                        }
                        Log.d("prost", "onChildAdded:" + dataSnapshot);
                        if (dataSnapshot.child("now_color").getValue() != null) {
//                      自身のnext_colorをdataSnap.Child("now_color")に変更
                            color = dataSnapshot.child("now_color").getValue(int.class);
                            myRef.child("prost_now").child(key).child("next_color").setValue(color);
                            if(is_first){
//                              next_colorは一回だけ変える
                                myRef.child("prost_now").child(key).removeValue();
                                Log.d("prost","removeValue_Iam_first");
                            }else{
                                count++;
                            }
                        }
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
//                        Lisnerの解除
                        if (dataSnapshot.getKey() == key){
                            Log.d("prost","removeListner");
                            myRef.child("prost_now").removeEventListener(this);
                        }else{
                            Log.d("prost","removeChild:"+dataSnapshot);
                            if(!is_first){
//                                自分のNext_colorの持ち主が消えたことを確認する
                                myRef.child("prost_now").child(key).child("next_color").setValue(user.now_color);
                                color = user.now_color;
                            }
                        }
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                };
                myRef.child("prost_now").addChildEventListener(ev);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
//                        next_colorを書き換え
//                        dbの削除
                        myRef.child("prost_now").child(key).removeValue();
                        Log.d("prost","removeValue");
                        user.now_color = color;
                        Log.d("prost","onChildtest:"+color);
                        test_tv.setText(""+color);
                    }
                }, 400);

            }
        });

        //roomを作成する
        roomCreateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                String roomId = roomNumber.getText().toString();
                Room room = new Room(roomId);
                myRef = database.getReference("room" + roomId);

                //ユーザーのリストなどを見張る
                childEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildAdded:" + dataSnapshot);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildChanged:" + dataSnapshot);

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                        Log.d("a", "onChildRemoved:" + dataSnapshot);

                        if(dataSnapshot.child("userID").getValue() == null){
                            return;
                        }

                        int id = dataSnapshot.child("userID").getValue(int.class);
                        if(id < user.userId){
                            user.userId --;
                            Map<String, Object> childUpdates = new HashMap<>();
                            childUpdates.put("/"+ key + "/userID", user.userId );
                            myRef.updateChildren(childUpdates);
                        }
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                        Log.d("a", "onChildMoved:" + dataSnapshot.getKey());
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
                    user.userKey = key;
                    registUserID(myRef,user);
                    user.userName = userName.getText().toString();
                    myRef.child(key).child("userName").setValue(user.userName);
                }


            }
        });
    }

    private void registUserID(final DatabaseReference databaseReference, final User user){
        databaseReference.child("test").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if(mutableData.getValue() == null){
                    mutableData.setValue(1);
                    user.userId = 1;
                }else{
                    int id = mutableData.getValue(int.class)+1;
                    mutableData.setValue(id);
                    user.userId = id;
                }
                databaseReference.child(user.userKey).child("userId").setValue(user.userId);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                if (b) {
                    String logMessage = dataSnapshot.getValue().toString();
                    Log.d("testRunTran", "counter: " + logMessage);
                } else {
                    Log.d("testRunTran", databaseError.getMessage(), databaseError.toException());
                }
            }
        });
    }
}
