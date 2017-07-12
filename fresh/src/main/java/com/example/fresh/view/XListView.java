/**
 * @file XListView.java
 * @package com.limxing.library.XListView
 * @create Mar 18, 2012 6:28:41 PM
 * @author Limxing
 * @description An ListView support (a) Pull down to refresh, (b) Pull up to load more.
 * Implement IXListViewListener, and see stopRefresh() / stopLoadMore().
 */
package com.example.fresh.view;


import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

import com.example.fresh.R;


/**
 * 下拉刷新，上滑自动加载。foot包含为空布局。
 * <pre>
 *  startRefresh();
 *  stopLoadMore();
 *  stopLoadMore(Msg);
 *  setNoDataDefaultString();
 *  setNoDataDefaultImg();
 * </pre>
 * <p>
 * 不再使用上拉刷新，而改用scroll响应式 zhangjianqiu 2017-7-11
 * 1,updateFoot 不再 footView.setState.
 * 2,ACTION_UP 不再 startLoadMore(),而是 reset All flag state.
 *
 * @Comment zhangjianqiu 2017-7-11..perfect in 7-12.
 */
public class XListView extends ListView implements OnScrollListener {

    private final static int SCROLLBACK_HEADER = 0;
    private final static int SCROLLBACK_FOOTER = 1;
    private final static int SCROLL_DURATION = 400; // scroll back duration
    private final static int PULL_LOAD_MORE_DELTA = 50; // when pull up >= 50px
    // at bottom, trigger
    // load more.
    private final static float OFFSET_RADIO = 1.8f; // support iOS like pull
    private float mLastY = -1; // save event y
    private Scroller mScroller; // used for scroll back
    private OnScrollListener mScrollListener; // user's scroll listener
    // the interface to trigger refresh and load more.
    private IXListViewListener mListViewListener;
    // -- header view
    private XListViewHeader mHeaderView;
    private RelativeLayout mHeaderViewContent;
    private TextView mHeaderTimeView;
    private int mHeaderViewHeight; // header view's height
    private boolean mEnablePullRefresh = true;
    private boolean mPullRefreshing = false; //refreshing State
    // -- footer view
    private XListViewFooter mFooterView;
    private boolean mEnablePullLoad;
    private boolean mPullLoading;//loadingState
    private boolean mIsFooterReady = false;
    private boolean isPullLoad = false;
    private boolean isPullRefresh = true;
    // total list items, used to detect is at the bottom of listview.
    private int mTotalItemCount;
    // for mScroller, scroll back from header or footer.
    private int mScrollBack;
    private boolean mPullLoad;
    private boolean isLoadMoreFlag;
    // feature.
    private View.OnClickListener footViewClick = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!mPullLoading && !mPullRefreshing) {
                mPullLoading = true;
                isLoadMoreFlag = false;
                mFooterView.setState(XListViewFooter.STATE_LOADING);
                startLoadMore();
            }
        }
    };

    /**
     * @param context
     */
    public XListView(Context context) {
        super(context);
        initWithContext(context);
    }

    public XListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initWithContext(context);
    }

    public XListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initWithContext(context);
    }

    private void initWithContext(Context context) {
        mScroller = new Scroller(context, new DecelerateInterpolator());
        // XListView need the scroll event, and it will dispatch the event to user's listener (as a proxy).
        super.setOnScrollListener(this);

        // init header view
        mHeaderView = new XListViewHeader(context);
        mHeaderViewContent = (RelativeLayout) mHeaderView.findViewById(R.id.xlistview_header_content);
        mHeaderTimeView = (TextView) mHeaderView.findViewById(R.id.xlistview_header_time);
        addHeaderView(mHeaderView, null, false);

        // init footer view
        mFooterView = new XListViewFooter(context, this);
        // init header height
        mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        mHeaderViewHeight = mHeaderViewContent.getHeight();
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                });

        setPullLoadEnable(isPullLoad);//初始化不支持上啦加载
        mFooterView.getmHintView().setOnClickListener(footViewClick);
        mFooterView.getNoDataDefaultText().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListViewListener != null) mListViewListener.onRefresh();
                mFooterView.setNoneDataLoadingState(true);
            }
        });
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        // make sure XListViewFooter is the last footer view, and only add once.
        if (!mIsFooterReady) {
            mIsFooterReady = true;
            addFooterView(mFooterView);
        }
        mHeaderView.setRefreshStampTag(adapter.getClass().getSimpleName());
        super.setAdapter(adapter);
    }

    /**
     * enable or disable pull down refresh feature.
     */
    public void setPullRefreshEnable(boolean enable) {
        isPullRefresh = enable;
        if (!enable) { // disable, hide the content
            mHeaderViewContent.setVisibility(View.INVISIBLE);
        } else {
            mHeaderViewContent.setVisibility(View.VISIBLE);
        }
        enableRefresh(enable);
    }

    private void enableRefresh(boolean enable) {
        mEnablePullRefresh = enable;
    }

    /**
     * enable or disable pull up load more feature.
     */
    public void setPullLoadEnable(boolean enable) {
        isPullLoad = enable;
        enablePullLoad(enable);
    }

    private void enablePullLoad(boolean enable) {
        mEnablePullLoad = enable;
        if (!enable) {
            mFooterView.hide();
            //make sure "pull up" don't show a line in bottom when listview with one page
            setFooterDividersEnabled(false);
            mFooterView.getmHintView().setOnClickListener(null);
        } else {
            mFooterView.show();
            mFooterView.setState(XListViewFooter.STATE_NORMAL);
            //make sure "pull up" don't show a line in bottom when listview with one page
            setFooterDividersEnabled(true);
            // both "pull up" and "click" will invoke load more.
            mFooterView.getmHintView().setOnClickListener(footViewClick);
        }
    }

    /**
     * stop refresh, reset header view.
     */
    public void stopRefresh(boolean isSuccess) {
        if (mPullRefreshing) {
            if (isSuccess) {
                mHeaderView.setState(XListViewHeader.STATE_SUCCESS);
            } else {
                mHeaderView.setState(XListViewHeader.STATE_FRESH_FAILT);
            }
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPullRefreshing = false;
                    resetHeaderHeight();
                }
            }, 1000);
        } else {
            mFooterView.setNoneDataLoadingState(false);
        }
    }


    /**
     * stop load more, reset footer view.
     */
    public void stopLoadMore() {
        mPullLoad = false;
        mPullLoading = false;
        if (mFooterView.getCurrentState() != XListViewFooter.STATE_NORMAL) {
            mFooterView.setState(XListViewFooter.STATE_NORMAL);
            resetFooterHeight();
        }
    }

    /**
     * 添加一个停止加载后更改提示字的方法
     */
    public void stopLoadMore(String msg) {
        mPullLoad = false;
        mPullLoading = false;
        if (mFooterView.getCurrentState() != XListViewFooter.STATE_NORMAL) {
            mFooterView.setState(XListViewFooter.STATE_NORMAL);
            resetFooterHeight();
            if (msg.length() > 0) {
                mFooterView.getmHintView().setText(msg);
            }
        }
    }


    public void setNoDataDefaultText(@StringRes int stringId) {
        mFooterView.setNoDataDefaultText(stringId);
    }

    public void setNoDataDefaultImg(@DrawableRes int drawableId) {
        mFooterView.setNoDataDefaultImg(drawableId);
    }

    /**
     * set last refresh time
     *
     * @param time
     */
    public void setRefreshTime(String time) {
        mHeaderTimeView.setText(time);
    }

    private void invokeOnScrolling() {

        if (mScrollListener instanceof OnXScrollListener) {
            OnXScrollListener l = (OnXScrollListener) mScrollListener;
            l.onXScrolling(this);
        }
    }

    private void updateHeaderHeight(float delta) {
        if (mEnablePullRefresh) {
            mHeaderView.setVisiableHeight((int) delta + mHeaderView.getVisiableHeight());
        }
        if (isPullRefresh && !mPullRefreshing) { // 未处于刷新状态，更新箭头
            if (mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
                mHeaderView.setState(XListViewHeader.STATE_READY);
            } else {
                mHeaderView.setState(XListViewHeader.STATE_NORMAL);
            }
        }
        setSelection(0); // scroll to top each time
    }

    /**
     * reset header view's height.
     */
    private void resetHeaderHeight() {
        int height = mHeaderView.getVisiableHeight();
        if (height == 0) // not visible.
            return;
        // refreshing and header isn't shown fully. do nothing.
        if (mPullRefreshing && height <= mHeaderViewHeight) {
            return;
        }
        int finalHeight = 0; // default: scroll back to dismiss header.
        // is refreshing, just scroll back to show all the header.
        if (mPullRefreshing && height > mHeaderViewHeight) {
            finalHeight = mHeaderViewHeight;
        }

        mScrollBack = SCROLLBACK_HEADER;
        mScroller.startScroll(0, height, 0, finalHeight - height, SCROLL_DURATION);
        // trigger computeScroll
        invalidate();
    }

    private void updateFooterHeight(float delta) {
        int height = mFooterView.getBottomMargin() + (int) delta;
        if (mEnablePullLoad) {
            if (height > PULL_LOAD_MORE_DELTA) { // height enough to invoke load
                // more.不再使用上拉刷新
                //mFooterView.setState(XListViewFooter.STATE_READY);
                //mPullLoading = true;
            } else {
                //mFooterView.setState(XListViewFooter.STATE_NORMAL);
                //mPullLoading = false;
                //mPullLoad = false;
            }
        }
        mFooterView.setBottomMargin(height);
    }

    private void resetFooterHeight() {
        int bottomMargin = mFooterView.getBottomMargin();
        if (bottomMargin > 0) {
            mScrollBack = SCROLLBACK_FOOTER;
            mScroller.startScroll(0, bottomMargin, 0, -bottomMargin, SCROLL_DURATION);
            invalidate();
        }
    }

    private void startLoadMore() {
        if (mListViewListener != null) {
            mListViewListener.onLoadMore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mLastY == -1) {
            mLastY = ev.getRawY();
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastY = ev.getRawY();
                if (!mPullRefreshing && getFirstVisiblePosition() == 0) {
                    mHeaderView.refreshUpdatedAtValue();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                final float deltaY = ev.getRawY() - mLastY;
                mLastY = ev.getRawY();
                if (!mPullLoading && getFirstVisiblePosition() == 0 && (mHeaderView.getVisiableHeight() > 0 || deltaY > 0)) {
                    // the first item is showing, header has shown or pull down.
                    updateHeaderHeight(deltaY / OFFSET_RADIO);
                    invokeOnScrolling();
                } else if (!mPullRefreshing && !mPullLoad && getLastVisiblePosition() == mTotalItemCount - 1
                        && (mFooterView.getBottomMargin() > 0 || deltaY < 0)) {
                    // last item, already pulled up or want to pull up.
                    updateFooterHeight(-deltaY / OFFSET_RADIO);
                }
                break;
            case MotionEvent.ACTION_UP:
                mLastY = -1; // reset
                if (!mPullRefreshing && getFirstVisiblePosition() == 0) {
                    // invoke refresh
                    if (isPullRefresh && mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
                        mPullRefreshing = true;
                        mHeaderView.setState(XListViewHeader.STATE_REFRESHING);
                        if (mListViewListener != null) {
                            mListViewListener.onRefresh();
                        }
                    }
                }
                if (mPullLoading && getLastVisiblePosition() == mTotalItemCount - 1) {
                    // invoke load more.
                    if (mEnablePullLoad && mFooterView.getBottomMargin() > PULL_LOAD_MORE_DELTA) {
                        //不再使用上拉刷新，而改用scroll响应式 zhangjianqiu 2017-7-11
                        //mFooterView.setState(XListViewFooter.STATE_LOADING);
                        //startLoadMore();
                    }
                }
                resetFooterHeight();
                resetHeaderHeight();
                break;
        }
        return super.onTouchEvent(ev);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mScrollBack == SCROLLBACK_HEADER) {
                mHeaderView.setVisiableHeight(mScroller.getCurrY());
            } else {
                mFooterView.setBottomMargin(mScroller.getCurrY());
            }
            postInvalidate();
            invokeOnScrolling();
        }
        super.computeScroll();
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        mScrollListener = l;
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mScrollListener != null) {
            mScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // send to user's listener
        mTotalItemCount = totalItemCount;
        if (mScrollListener != null) {
            mScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
        //disappear footView Once.And ensure after stopLoad.
        if (view.getCount() > 2 && view.getLastVisiblePosition() == view.getCount() - 1) {
            if (isLoadMoreFlag && (mFooterView.getCurrentState() == XListViewFooter.STATE_NORMAL)) {
                mPullLoading = true;
                mFooterView.setState(XListViewFooter.STATE_LOADING);
                startLoadMore();
            }
            isLoadMoreFlag = false;
        } else {
            isLoadMoreFlag = true;
        }
    }

    public void setXListViewListener(IXListViewListener l) {
        mListViewListener = l;
    }

    /**
     * Executive refresh
     * 执行刷新
     */
    public void startRefresh() {
        if (!mPullRefreshing) {
            mPullRefreshing = true;
            setSelection(0);
            mHeaderView.setState(XListViewHeader.STATE_REFRESHING);
            if (mListViewListener != null) {
                mListViewListener.onRefresh();
            }
            mScrollBack = SCROLLBACK_HEADER;
            mScroller.startScroll(0, 0, 0, mHeaderViewHeight, SCROLL_DURATION);
            // trigger computeScroll
            invalidate();
        }
    }

    /**
     * Set hide time
     * 设置隐藏时间
     */
    public void hideTimeView() {
        mHeaderTimeView.setVisibility(View.GONE);
    }

    /**
     * 设置底部文字
     *
     * @param text
     */
    public void setFootText(String text) {
        mFooterView.getmHintView().setText(text);
    }

    /**
     * 设置头部文字
     *
     * @param header
     */
    public void setHeaderText(String header) {
        mHeaderView.getmHintTextView().setText(header);
    }

    /**
     * notification的时候调用
     */
    @Override
    public void requestLayout() {
        super.requestLayout();
        if (mFooterView == null) {
            return;
        }
        if (getCount() <= getHeaderViewsCount() + getFooterViewsCount()) {
            mFooterView.setNoneDataState(true);
            mFooterView.setNoneDataStateHeight(getMeasuredHeight());
            if (isPullLoad) {
                enablePullLoad(false);
            }
            if (isPullRefresh) {
                enableRefresh(false);
            }
        } else {
            mFooterView.setNoneDataState(false);
            if (isPullLoad) {
                mFooterView.show();
                mFooterView.getmHintView().setOnClickListener(footViewClick);
            }
            if (isPullRefresh) {
                enableRefresh(true);
            }
        }
    }

    /**
     * you can listen ListView.OnScrollListener or this one. it will invoke
     * onXScrolling when header/footer scroll back.
     */
    public interface OnXScrollListener extends OnScrollListener {
        void onXScrolling(XListView view);
    }


    /**
     * implements this interface to get refresh/load more event.
     */
    public interface IXListViewListener {
        void onRefresh();

        void onLoadMore();
    }
}
