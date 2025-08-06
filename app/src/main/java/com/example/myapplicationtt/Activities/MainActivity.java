package com.example.myapplicationtt.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.myapplicationtt.Fragments.AccountFragment;
import com.example.myapplicationtt.Fragments.ChatsFragment;
import com.example.myapplicationtt.Fragments.HomeFragment;
import com.example.myapplicationtt.Fragments.MyAdsFragment;
import com.example.myapplicationtt.R;
import com.example.myapplicationtt.Utils;
import com.example.myapplicationtt.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    private static final String TAG ="MAIN_TAG";

    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);




        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth =FirebaseAuth.getInstance();
        if(firebaseAuth.getCurrentUser()==null){
            startLoginOption();
       // }else{

           // updateFCMToken();
          //  askNotificationPermission();

        }//

        showHomeFragment();

        binding.bottomNv.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int ItemId=item.getItemId();
                if(ItemId== R.id.menu_home){

                    showHomeFragment();
                    return  true;
                }
                else if(ItemId ==R.id.menu_chats){

                    if(firebaseAuth.getCurrentUser() == null){
                        Utils.toast(MainActivity.this,"Login Required...");
                        startLoginOption();
                        return false;
                    }
                    else{
                        showChatFragment();
                        return true;

                    }



                }
                else if(ItemId==R.id.menu_my_ads){

                    if(firebaseAuth.getCurrentUser() == null){
                        Utils.toast(MainActivity.this,"Login Required...");
                        startLoginOption();
                        return false;
                    }
                    else{
                        showMyAdsFragment();
                        return  true;


                    }


                }
                else if(ItemId==R.id.menu_account){

                    if(firebaseAuth.getCurrentUser() == null){
                        Utils.toast(MainActivity.this,"Login Required...");
                        startLoginOption();
                        return  false;
                    }
                    else{
                        showAccountFragment();
                        return true;

                    }


                }
                else{
                    return  false;

                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        binding.sellFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, AdCreateActivity.class);
                intent.putExtra("isEditMode",false);
                startActivity(intent);
            }
        });

    }

    private void showHomeFragment() {
        binding.toolbarTitleTv.setText("Home");
        HomeFragment fragment=new HomeFragment();
        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(),fragment, "HomeFragment");
        fragmentTransaction.commit();


    }
    private void showChatFragment(){
        binding.toolbarTitleTv.setText("Chats");
        ChatsFragment fragment=new ChatsFragment();
        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(),fragment, "ChatsFragment");
        fragmentTransaction.commit();

    }
    private void showMyAdsFragment(){
        binding.toolbarTitleTv.setText("My Ads");
        MyAdsFragment fragment=new MyAdsFragment();
        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(),fragment, "MyAdsFragment");
        fragmentTransaction.commit();

    }
    private void showAccountFragment(){
        binding.toolbarTitleTv.setText("Account");
        AccountFragment fragment=new AccountFragment();
        FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentsFl.getId(),fragment, "AcountFragment");
        fragmentTransaction.commit();

    }
    private void startLoginOption(){

        startActivity(new Intent(this,LoginOptionsActivity.class));
    }


    }

