package com.example.wally_nagama.paripigrass;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference();
    Button button, userCreateButton;
    EditText editText, userName, roomNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = (Button)findViewById(R.id.button);
        userCreateButton = (Button)findViewById(R.id.userCreate);
        editText = (EditText)findViewById(R.id.edittext);
        userName = (EditText)findViewById(R.id.userName);
        roomNumber = (EditText)findViewById(R.id.roomNumber);

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                //String value = dataSnapshot.getValue(String.class);
                //Log.d("a", "Value is: " + value);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("a", "Failed to read value.", error.toException());
            }
        });

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                myRef.child("user").setValue(editText.getText().toString());
            }
        });

        //user作成する
        userCreateButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                User user = new User(userName.getText().toString());
                String roomId = roomNumber.getText().toString();
                myRef.child("room" + roomId).child("user1").setValue(user.userName);
            }
        });

    }
}
