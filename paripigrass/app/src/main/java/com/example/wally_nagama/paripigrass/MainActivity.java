package com.example.wally_nagama.paripigrass;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.button);
        roomCreateButton = (Button)findViewById(R.id.userCreate);
        editText = (EditText)findViewById(R.id.edittext);
        userName = (EditText)findViewById(R.id.userName);
        roomNumber = (EditText)findViewById(R.id.roomNumber);

        user = new User();

        myRef.child("user").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("onClick",""+dataSnapshot.getValue(int.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                myRef.child("user").setValue(1);
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
                    registUserID(key,myRef,user);
                    user.userName = userName.getText().toString();
                    user.userKey = key;
                    myRef.child(key).child("userName").setValue(user.userName);
                }


            }
        });

    }

    private void registUserID(final String key, final DatabaseReference databaseReference, final User user){
        final Query query = databaseReference.orderByChild("userID").limitToLast(1);
        query.addChildEventListener(new ChildEventListener(){
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                int id;
                if(dataSnapshot.child("userID").getValue(int.class) == null){
                    //誰もおらんとき
                    id = 0;
                }else{
                    id = dataSnapshot.child("userID").getValue(int.class)+1;
                }
                databaseReference.child(key).child("userID").setValue(id);
                user.userId = id;
                Log.d("registID", "onChildAdded:" + dataSnapshot);
                query.removeEventListener(this);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }
}
