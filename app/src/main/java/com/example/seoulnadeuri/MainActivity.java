package com.example.seoulnadeuri;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
// 뷰 바인딩 클래스 import (이름이 안 맞아서 빨간 줄이 뜨면 Alt+Enter 눌러주세요)
import com.example.seoulnadeuri.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {

    // 뷰 바인딩 객체 선언 (과거의 findViewById 노가다를 없애주는 최신 무기)
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 뷰 바인딩 초기화 및 화면 세팅
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 앱 켜고 메인으로 넘어왔을 때, 처음 보여줄 화면을 HomeFragment로 지정
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }

        // 하단 탭 아이콘을 클릭했을 때의 동작 (람다식 사용)
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // 어떤 탭을 눌렀는지 아이디로 구분
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_record) {
                selectedFragment = new RecordFragment();
            } else if (itemId == R.id.nav_extra) {
                selectedFragment = new WishlistFragment();
            }

            // 고른 화면으로 싹 갈아끼우기
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true; // 클릭 성공적으로 처리됨
        });
    }
}