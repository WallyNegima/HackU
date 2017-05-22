package com.example.wally_nagama.paripigrass;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.support.annotation.IdRes;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private TextView txvAction;
    private static final int REQUEST_CODE = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // へーへーボタン（同意を表す貴重なボタン）
        txvAction = (TextView)findViewById(R.id.amin_txvAction);

        //へーへーボタンリスナー
        findViewById(R.id.amin_heybutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txvAction.setText(R.string.amin_heybutton);
            }
        });

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


    }


    // startActivityForResultで起動したアクティビティが終了した時に呼び出される関数
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 音声認識結果の時
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            // 結果文字列リストを取得
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);

            // 取得した文字列を結合
            String resultsString = "";
            for(int i = 0; i < results.size(); i++) {
                resultsString += results.get(i)+";";
            }

            //トーストで結果表示
            Toast.makeText(this, resultsString, Toast.LENGTH_LONG).show();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }




}
