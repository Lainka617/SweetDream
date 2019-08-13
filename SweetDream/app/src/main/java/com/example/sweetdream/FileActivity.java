package com.example.sweetdream;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import com.example.sweetdream.adapter.FileAdapter;
import com.example.sweetdream.base.BaseActivity;
import com.example.sweetdream.db.BookList;
import com.example.sweetdream.util.FileUtils;
import com.example.sweetdream.util.Fileutil;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;


public class FileActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.btn_choose_all)
    Button btnChooseAll;
    @Bind(R.id.btn_delete)
    Button btnDelete;
    @Bind(R.id.btn_add_file)
    Button btnAddFile;
    @Bind(R.id.lv_file_drawer)
    ListView lvFileDrawer;

    public static final int EXTERNAL_STORAGE_REQ_CODE = 10 ;


    private File root;
    private List<File> listFile = new ArrayList<>();
    private static FileAdapter adapter;
    private SearchTextFileTask mSearchTextFileTask;
    private SaveBookToSqlLiteTask mSaveBookToSqlLiteTask;
    @Override
    public int getLayoutRes() {
        return R.layout.activity_file;
    }

    @Override
    protected void initData() {
        getWindow().setBackgroundDrawable(null);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        if(getSupportActionBar() != null) {
            getSupportActionBar().setTitle("add file");
        }

        adapter = new FileAdapter(this, listFile);
        lvFileDrawer.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(FileActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, EXTERNAL_STORAGE_REQ_CODE,"Permission needed when adding books");
        }else{
            root = Environment.getExternalStorageDirectory();
            searchFile();
        }

    }

    @Override
    protected void initListener() {
        lvFileDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                adapter.setSelectedPosition(position);
            }
        });

        lvFileDrawer.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                return true;
            }
        });

        adapter.setCheckedChangeListener(new FileAdapter.CheckedChangeListener() {

            @Override
            public void onCheckedChanged(int position, CompoundButton buttonView, boolean isChecked) {
                setAddFileText(adapter.getCheckNum());
            }
        });

        btnChooseAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.checkAll();
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.cancel();
            }
        });

        btnAddFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBookList();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSearchTextFileTask != null) {
            mSearchTextFileTask.cancel(true);
        }
        if (mSaveBookToSqlLiteTask != null){
            mSaveBookToSqlLiteTask.cancel(true);
        }
    }


    private void saveBookList(){
        List<File> files = adapter.getCheckFiles();
        if (files.size() > 0) {
            List<BookList> bookLists = new ArrayList<BookList>();
            for (File file : files) {
                BookList bookList = new BookList();
                String bookName = Fileutil.getFileNameNoEx(file.getName());
                bookList.setBookname(bookName);
                bookList.setBookpath(file.getAbsolutePath());
                bookLists.add(bookList);
            }
            mSaveBookToSqlLiteTask = new SaveBookToSqlLiteTask();
            mSaveBookToSqlLiteTask.execute(bookLists);
        }
    }

    private class SaveBookToSqlLiteTask extends AsyncTask<List<BookList>,Void,Integer>{
        private static final int FAIL = 0;
        private static final int SUCCESS = 1;
        private static final int REPEAT = 2;
        private BookList repeatBookList;

        @Override
        protected Integer doInBackground(List<BookList>... params) {
            List<BookList> bookLists = params[0];
            for (BookList bookList : bookLists){
                List<BookList> books = DataSupport.where("bookpath = ?", bookList.getBookpath()).find(BookList.class);
                if (books.size() > 0){
                    repeatBookList = bookList;
                    return REPEAT;
                }
            }

            try {
                DataSupport.saveAll(bookLists);
            } catch (Exception e){
                e.printStackTrace();
                return FAIL;
            }
            return SUCCESS;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            String msg = "";
            switch (result){
                case FAIL:
                    msg = "Fail";
                    break;
                case SUCCESS:
                    msg = "add successful";
                    setAddFileText(0);
                    adapter.cancel();
                    break;
                case REPEAT:
                    msg = "book" + repeatBookList.getBookname() + "repeated";
                    break;
            }

            Toast.makeText(FileActivity.this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    //设置添加按钮text
    protected void setAddFileText(final int num){
        btnAddFile.post(new Runnable() {
            @Override
            public void run() {
                btnAddFile.setText("add into bookshelf(" + num + ")");
            }
        });
    }
    protected void searchFile(){
//        startTime = System.currentTimeMillis();
        mSearchTextFileTask = new SearchTextFileTask();
        mSearchTextFileTask.execute();
    }

    private class SearchTextFileTask extends AsyncTask<Void,Void,Boolean>{
        @Override
        protected void onPreExecute() {
            showProgress(true,"scaning text");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            listFile = FileUtils.getSuffixFile(root.getAbsolutePath(),".txt");
            if (listFile == null || listFile.isEmpty()){
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            hideProgress();
            if (result) {
                adapter.setFiles(listFile);  //list值传到adapter
                setAddFileText(0);
//                endTime = System.currentTimeMillis();
//                Log.e("time",endTime - startTime + "");
            } else {
                Toast.makeText(FileActivity.this, "no file", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case EXTERNAL_STORAGE_REQ_CODE: {

                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    root = Environment.getExternalStorageDirectory();
                    searchFile();
                } else {

                }
                return;
            }
        }
    }

}
