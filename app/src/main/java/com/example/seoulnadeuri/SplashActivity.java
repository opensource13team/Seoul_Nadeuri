package com.example.seoulnadeuri;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // 방금 만든 XML 껍데기 연결

        // 3초(3000 밀리초) 대기 후 실행되는 타이머
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // SplashActivity에서 MainActivity로 이동하라는 '인텐트(Intent)' 택배 생성
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent); // 이동 시작!

                // 스플래시 화면은 뒤로 가기 눌러도 다시 안 나오게 완전 종료
                finish();
            }
        }, 3000);
    }
}