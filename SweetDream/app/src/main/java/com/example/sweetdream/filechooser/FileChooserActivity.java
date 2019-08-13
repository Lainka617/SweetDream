package com.example.sweetdream.filechooser;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.sweetdream.FileActivity;
import com.example.sweetdream.R;

import java.util.ArrayList;

public class FileChooserActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private FragmentManager fragmentManager = null;
    private FragmentTransaction fragmentTransaction = null;
    private DirectoryFragment mDirectoryFragment;

    public static final int EXTERNAL_STORAGE_REQ_CODE = 10 ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filechooser);


        toolbar = (Toolbar) findViewById(R.id.tool_bar);
        toolbar.setTitle("Catalogue");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();

        mDirectoryFragment = new DirectoryFragment();
        mDirectoryFragment.setDelegate(new DirectoryFragment.DocumentSelectActivityDelegate() {

            @Override
            public void startDocumentSelectActivity() {

            }

            @Override
            public void didSelectFiles(DirectoryFragment activity,
                                       ArrayList<String> files) {
                mDirectoryFragment.showReadBox(files.get(0).toString());
            }

            @Override
            public void updateToolBarName(String name) {
                toolbar.setTitle(name);

            }
        });
        fragmentTransaction.add(R.id.fragment_container, mDirectoryFragment, "" + mDirectoryFragment.toString());
        fragmentTransaction.commit();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(FileChooserActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, EXTERNAL_STORAGE_REQ_CODE,"Permission needed when adding new book");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mDirectoryFragment.onFragmentDestroy();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mDirectoryFragment.onBackPressed_()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.file, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_select_file){
            Intent intent = new Intent(FileChooserActivity.this, FileActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     *
     * @param thisActivity
     * @param permission
     * @param requestCode
     * @param errorText
     */
    protected void checkPermission (Activity thisActivity, String permission, int requestCode, String errorText) {

        if(ContextCompat.checkSelfPermission(thisActivity,permission) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                    permission)) {
                Toast.makeText(this,errorText,Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(thisActivity,
                        new String[]{permission},
                        requestCode);
            } else {
                ActivityCompat.requestPermissions(thisActivity,
                        new String[]{permission},
                        requestCode);
            }
        } else {

        }
    }
}
