/**
 * @file XFooterView.java
 * @author Limxing
 * @description XListView's footer
 */
package com.example.fresh.view;


import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.fresh.R;


public class XListViewFooter extends RelativeLayout {

    public final static int STATE_NORMAL = 0;
    public final static int STATE_READY = 1;
    public final static int STATE_LOADING = 2;
    private Drawable selector;
    private View mContentView;
    private View mProgressBar;
    private TextView mHintView;
    private TextView mNodataString;
    private LinearLayout xlistview_footer_state;
    private RelativeLayout xlistview_footer_nodata;
    private LinearLayout llnodateloading;
    private LoadView lvNodata;
    private LoadView xlistview_footer_loadview;
    private int currentState = 100;
    private XListView parentListView;

    public XListViewFooter(Context context, XListView xListView) {
        super(context);
        initView(context);
        parentListView = xListView;
        selector = xListView.getSelector();
    }

    public XListViewFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public synchronized void setState(int state) {
        currentState = state;
        if (state == STATE_READY) {
            mProgressBar.setVisibility(View.INVISIBLE);
            mHintView.setVisibility(View.VISIBLE);
            mHintView.setText(R.string.xlistview_footer_hint_ready);
        } else if (state == STATE_LOADING) {
            mHintView.setVisibility(View.INVISIBLE);
            mProgressBar.setVisibility(View.VISIBLE);
            xlistview_footer_loadview.startLoad();
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
            mHintView.setVisibility(View.VISIBLE);
            xlistview_footer_loadview.stopLoad();
            mHintView.setText(R.string.xlistview_footer_hint_normal);
        }
    }

    public int getBottomMargin() {
        LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
        return lp.bottomMargin;
    }

    public void setBottomMargin(int height) {
        if (height < 0) return;
        LayoutParams lp = (LayoutParams) mContentView.getLayoutParams();
        lp.bottomMargin = height;
        mContentView.setLayoutParams(lp);
    }

    /**
     * hide footer when disable pull load more
     */
    public void hide() {
        mContentView.setVisibility(View.GONE);
    }

    /**
     * show footer
     */
    public void show() {
        mContentView.setVisibility(View.VISIBLE);
    }

    private void initView(Context context) {
        RelativeLayout moreView = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.xlistview_footer, null);
        addView(moreView);
        moreView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        xlistview_footer_nodata = moreView.findViewById(R.id.xlistview_footer_nodata);
        xlistview_footer_nodata.setVisibility(View.GONE);
        xlistview_footer_state = moreView.findViewById(R.id.xlistview_footer_state);

        mContentView = moreView.findViewById(R.id.xlistview_footer_content);
        mProgressBar = moreView.findViewById(R.id.xlistview_footer_progressbar);
        lvNodata = moreView.findViewById(R.id.xlistview_footer_nodataprogressbar);
        mHintView = moreView.findViewById(R.id.xlistview_footer_hint_textview);
        llnodateloading = moreView.findViewById(R.id.xlistview_footer_nodateloading);
        xlistview_footer_loadview = moreView.findViewById(R.id.xlistview_footer_loadview);
        mNodataString = xlistview_footer_state.findViewById(R.id.xlistview_footer_nodatatext);
    }

    public TextView getmHintView() {
        return mHintView;
    }

    void setNoneDataLoadingState(boolean isLoading) {
        if (isLoading) {
            xlistview_footer_state.setVisibility(View.GONE);
            llnodateloading.setVisibility(View.VISIBLE);
            lvNodata.startLoad();
        } else {
            xlistview_footer_state.setVisibility(View.VISIBLE);
            llnodateloading.setVisibility(View.GONE);
            lvNodata.stopLoad();
        }
    }

    TextView getNoDataDefaultText() {
        return mNodataString;
    }

    void setNoDataDefaultText(@StringRes int stringId) {
        TextView nodatatext = xlistview_footer_state.findViewById(R.id.xlistview_footer_nodatatext);
        nodatatext.setText(stringId);
    }

    void setNoDataDefaultImg(@DrawableRes int drawableId) {
        ImageView iv = xlistview_footer_state.findViewById(R.id.xlistview_footer_nodataimg);
        iv.setImageResource(drawableId);
    }

    int getCurrentState() {
        return currentState;
    }

    /**
     * 设置是否显示有无数据有好提示
     */
    void setNoneDataState(boolean isNone) {
        if (isNone) {
            xlistview_footer_nodata.setVisibility(View.VISIBLE);
            parentListView.setSelector(new ColorDrawable(Color.TRANSPARENT));
        } else {
            xlistview_footer_nodata.setVisibility(View.GONE);
            parentListView.setSelector(selector);
        }
    }

    /**
     * 设置为空布局的高度，因为lv中所有的都是wrap，所以需要单独设置。
     */
    void setNoneDataStateHeight(int height) {
        xlistview_footer_nodata.setMinimumHeight(height);
    }
}
