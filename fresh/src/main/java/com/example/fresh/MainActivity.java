package com.example.fresh;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.fresh.view.XListView;


public class MainActivity extends AppCompatActivity implements XListView.IXListViewListener {

    private XListView listview;
    private boolean b;
    private int count = 0;
    BaseAdapter wo = new BaseAdapter() {
        @Override
        public int getCount() {
            return count;
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextView view1 = new TextView(MainActivity.this);
            view1.setText("leefeng.me" + i);
            view1.setHeight(100);
            return view1;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listview = (XListView) findViewById(R.id.listview);
        listview.setPullLoadEnable(true);
        listview.setPullRefreshEnable(true);
        listview.setXListViewListener(this);
        listview.setAdapter(wo);
        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                listview.stopLoadMore();
                listview.startRefresh();
            }
        });
    }

    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                listview.stopRefresh(b);
                b = !b;
                if (b) {
                    count = 0;
                } else {
                    count = 20;
                }
                wo.notifyDataSetChanged();
            }
        }, 2000);
    }

    @Override
    public void onLoadMore() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                listview.stopLoadMore("zhangjainqiu is handsome");
                if (listview.getCount() < 40) {
                    count += 20;
                    wo.notifyDataSetChanged();
                }
            }
        }, 5000);
    }
}
