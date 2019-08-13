package com.example.sweetdream;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import android.widget.AbsoluteLayout;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.sweetdream.adapter.ShelfAdapter;

import com.example.sweetdream.base.BaseActivity;
import com.example.sweetdream.db.BookList;
import com.example.sweetdream.filechooser.FileChooserActivity;
import com.example.sweetdream.view.DragGridView;

import org.litepal.crud.DataSupport;

import java.io.File;
import java.util.List;

import butterknife.Bind;

public class MainActivity extends BaseActivity {

    @Bind(R.id.toolbar)
    Toolbar toolbar;
    @Bind(R.id.fab)
    FloatingActionButton fab;
    @Bind(R.id.bookShelf)
    DragGridView bookShelf;


    private WindowManager mWindowManager;
    private AbsoluteLayout wmRootView;
    private View rootView;
    private Typeface typeface;

    private List<BookList> bookLists;
    private ShelfAdapter adapter;
    //the position of clicked book
    private int itemPosition;
    private TextView itemTextView;
    //the x,y of clicked book
    private int[] location = new int[2];

    private static TextView cover;
    private static ImageView content;


    private Config config;
    @Override
    public int getLayoutRes() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        setSupportActionBar(toolbar);

        config = Config.getInstance();
        getWindow().setBackgroundDrawable(null);
        mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wmRootView = new AbsoluteLayout(this);
        rootView = getWindow().getDecorView();

        typeface = config.getTypeface();
        bookLists = DataSupport.findAll(BookList.class);
        adapter = new ShelfAdapter(MainActivity.this,bookLists);
        bookShelf.setAdapter(adapter);
    }

    @Override
    protected void initListener() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MainActivity.this, FileChooserActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        bookShelf.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bookLists.size() > position) {
                    itemPosition = position;
                    String bookname = bookLists.get(itemPosition).getBookname();

                    adapter.setItemToFirst(itemPosition);
                    final BookList bookList = bookLists.get(itemPosition);
                    bookList.setId(bookLists.get(0).getId());
                    final String path = bookList.getBookpath();
                    File file = new File(path);
                    if (!file.exists()){
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(MainActivity.this.getString(R.string.app_name))
                                .setMessage(path + "file not exists, do you want to delete？")
                                .setPositiveButton("delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DataSupport.deleteAll(BookList.class, "bookpath = ?", path);
                                        bookLists = DataSupport.findAll(BookList.class);
                                        adapter.setBookList(bookLists);
                                    }
                                }).setCancelable(true).show();
                        return;
                    }

                    ReadActivity.openBook(bookList,MainActivity.this);

                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_select_file){
            Intent intent = new Intent(MainActivity.this, ThirdActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onRestart(){
        super.onRestart();
        DragGridView.setIsShowDeleteButton(false);
        bookLists = DataSupport.findAll(BookList.class);
        adapter.setBookList(bookLists);
//        closeBookAnimation();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onStop() {
        DragGridView.setIsShowDeleteButton(false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        DragGridView.setIsShowDeleteButton(false);
        super.onDestroy();
    }


}

