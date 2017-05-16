package com.phone.test;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;


public class MainActivity extends ListActivity {

    private String titles[]={"依赖关系优先级演示","依赖链传递演示"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getListView().setAdapter(new ArrayAdapter<String>(this,android.R.layout
                .simple_list_item_1,titles));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent intent=null;
        switch (position){
            case 0:
                intent=new Intent(this, Test0.class);
                break;
            case 1:
                intent=new Intent(this, Test1.class);
                break;
        }
        if(intent!=null){
            startActivity(intent);
        }
    }
}
