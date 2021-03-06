package com.example.zhaolexi.imageloader.detail;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.example.zhaolexi.imageloader.R;
import com.example.zhaolexi.imageloader.common.utils.AlbumConstructor;
import com.example.zhaolexi.imageloader.common.utils.DisplayUtils;
import com.example.zhaolexi.imageloader.common.utils.KeyBroadUtils;
import com.example.zhaolexi.imageloader.home.album.Photo;
import com.example.zhaolexi.imageloader.home.manager.Album;


public class PhotoDetailActivity extends DetailActivity<PhotoDetailPresenter, Photo> implements PhotoDetailViewInterface<Photo>, View.OnClickListener {

    public static final String ALBUM = "album";

    private RelativeLayout mDetailContainer, mLikeLayout;
    private LinearLayout mDescriptionContainer;
    private ImageView mEdit, mLike, mDownload;
    private EditText mDescription;
    private AlphaAnimation mAlphaAppear, mAlphaDisappear;
    private ValueAnimator mDescriptionOpen, mDescriptionClose;

    private Album mAlbumInfo;
    private int mLastPos;
    private boolean mHasThumbUp;
    private boolean mIsAnimating;
    private boolean mIsDescriptionShown = true;
    private boolean mIsInEditMode;

    private int mDescriptionHeight = DESCRIPTION_HEIGHT;
    private static final int DESCRIPTION_HEIGHT = 107;
    private static final int DESCRIPTION_EDIT = 42;
    private static final long DURATION = 150L;

    private static final int CLICK_SLOP = 10;
    private int mLastX, mLastY;

    private Runnable mLastCallback;
    private Handler mHandler;
    private static final long HIDE_THRESHOLD = 2000;
    private long mLastTouchScrollViewTime;

    @Override
    public void finish() {
        toggleThumbUp(mLastPos);
        super.finish();
    }

    public void toggleThumbUp(int pos) {
        Photo photo = mPagerAdapter.getDetailInfo(pos);
        if (photo != null && photo.hasThumbUp() != mHasThumbUp) {
            mPresenter.toggleThumbUp(photo.getPid());
            photo.setHasThumbUp(mHasThumbUp);
        }
    }

    @Override
    protected void initView() {
        super.initView();

        FrameLayout content = (FrameLayout) getWindow().getDecorView().findViewById(android.R.id.content);
        RelativeLayout root = (RelativeLayout) content.getChildAt(0);
        mDetailContainer = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.layout_detail, null);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DisplayUtils.dp2px(this, 150));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        root.addView(mDetailContainer, layoutParams);

        ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_description);
        scrollView.setOnTouchListener(this);
        mDescriptionContainer = (LinearLayout) findViewById(R.id.ll_description);
        mDescription = (EditText) findViewById(R.id.et_description);
        mDescription.setText(mPagerAdapter.getDetailInfo(mCurIndex).getDescription());
        mEdit = (ImageView) findViewById(R.id.iv_edit);
        mEdit.setVisibility(mAlbumInfo.isAccessible() ? View.VISIBLE : View.GONE);
        mEdit.setOnClickListener(this);
        mLike = (ImageView) findViewById(R.id.iv_like);
        mLike.setImageResource(mHasThumbUp ? R.mipmap.ic_thumb_up_red : R.mipmap.ic_thumb_up_grey);
        mLikeLayout = (RelativeLayout) findViewById(R.id.rl_like);
        mLikeLayout.setVisibility(new AlbumConstructor().isThird(mAlbumInfo) ? View.GONE : View.VISIBLE);
        mLikeLayout.setOnClickListener(this);
        mDownload = (ImageView) findViewById(R.id.iv_download);
        mDownload.setOnClickListener(this);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                toggleThumbUp(mLastPos);
                Photo photo = mPagerAdapter.getDetailInfo(position);
                mHasThumbUp = photo.hasThumbUp();
                mLike.setImageResource(mHasThumbUp ? R.mipmap.ic_thumb_up_red : R.mipmap.ic_thumb_up_grey);
                mDescription.setText(photo.getDescription());
                if (!mIsDescriptionShown) {
                    showDescription();
                }
                exitEditMode();
                mLastPos = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mViewPager.setOnTouchListener(this);
        mViewPager.setOnClickListener(this);
        hideDescriptionDelay();
        initAnimation();
    }

    @Override
    protected void setResultIntent(Intent intent) {

    }

    @Override
    protected void initData() {
        super.initData();
        mAlbumInfo = (Album) getIntent().getSerializableExtra(ALBUM);
        mPresenter.setAid(mAlbumInfo.getAid());
        if (!mAlbumInfo.isAccessible()) {
            mDescriptionHeight = DESCRIPTION_HEIGHT - DESCRIPTION_EDIT;
        }
        mHasThumbUp = mPagerAdapter.getDetailInfo(mCurIndex).hasThumbUp();
        mHandler = new Handler(getMainLooper());
    }

    @Override
    protected PhotoDetailPresenter createPresenter() {
        return new PhotoDetailPresenter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_detail, menu);
        if (!mAlbumInfo.isAccessible()) {
            menu.getItem(0).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_photo:
                mPresenter.deletePhoto(mPagerAdapter.getDetailInfo(mViewPager.getCurrentItem()).getPid());
        }
        return true;
    }

    @Override
    public void deletePhoto() {
        mPagerAdapter.removeDetail(mViewPager.getCurrentItem());
        if (mPagerAdapter.getCount() == 0) {
            finish();
        }
    }

    @Override
    public Album getAlbumInfo() {
        return mAlbumInfo;
    }

    @Override
    public void showHint(String hint) {
        Toast.makeText(this, hint, Toast.LENGTH_SHORT).show();
    }

    private void initAnimation() {
        final int height = DisplayUtils.dp2px(this, mDescriptionHeight);
        Interpolator linear = new LinearInterpolator();

        Animation.AnimationListener appearAnimationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsAnimating = false;
                mIsDescriptionShown = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        Animation.AnimationListener disappearAnimationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsAnimating = false;
                mIsDescriptionShown = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };

        mAlphaAppear = new AlphaAnimation(0, 1);
        mAlphaAppear.setDuration(DURATION);
        mAlphaAppear.setFillAfter(true);
        mAlphaAppear.setInterpolator(linear);
        mAlphaAppear.setAnimationListener(appearAnimationListener);

        mAlphaDisappear = new AlphaAnimation(1, 0);
        mAlphaDisappear.setDuration(DURATION);
        mAlphaDisappear.setFillAfter(true);
        mAlphaDisappear.setInterpolator(linear);
        mAlphaDisappear.setAnimationListener(disappearAnimationListener);

        mDescriptionOpen = ValueAnimator.ofInt(0, height).setDuration(DURATION);
        mDescriptionOpen.setTarget(mDescriptionContainer);
        mDescriptionOpen.setInterpolator(linear);
        mDescriptionOpen.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mDescriptionContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                mDescriptionContainer.requestLayout();
            }
        });
        mDescriptionOpen.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimating = false;
                mIsDescriptionShown = true;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsAnimating = false;
                mIsDescriptionShown = true;
                mDescriptionContainer.getLayoutParams().height = height;
                mDescriptionContainer.requestLayout();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        mDescriptionClose = ValueAnimator.ofInt(height, 0).setDuration(DURATION);
        mDescriptionClose.setTarget(mDescriptionContainer);
        mDescriptionClose.setInterpolator(linear);
        mDescriptionClose.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mDescriptionContainer.getLayoutParams().height = (int) animation.getAnimatedValue();
                mDescriptionContainer.requestLayout();
            }
        });
        mDescriptionClose.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsAnimating = false;
                mIsDescriptionShown = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mIsAnimating = false;
                mIsDescriptionShown = false;
                mDescriptionContainer.getLayoutParams().height = 0;
                mDescriptionContainer.requestLayout();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

    }


    @Override
    public void showDescription() {
        if (!mIsAnimating) {
            mDescriptionContainer.startAnimation(mAlphaAppear);
            mDescriptionOpen.start();
            hideDescriptionDelay();
        }
    }

    private void hideDescriptionDelay() {
        mHandler.removeCallbacks(mLastCallback);

        mLastCallback = new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - mLastTouchScrollViewTime > HIDE_THRESHOLD && !mIsInEditMode && mIsDescriptionShown) {
                    hideDescription();
                }
            }
        };
        mHandler.postDelayed(mLastCallback, HIDE_THRESHOLD);
    }

    @Override
    public void hideDescription() {
        if (!mIsAnimating) {
            mDescriptionContainer.startAnimation(mAlphaDisappear);
            mDescriptionClose.start();
        }
    }

    @Override
    public void enterEditMode() {
        mDescription.setEnabled(true);
        mDescription.requestFocus();
        KeyBroadUtils.openKeybord(this, mDescription);
        mEdit.setImageResource(R.mipmap.ic_done);
        mIsInEditMode = true;
    }

    @Override
    public void exitEditMode() {
        mDescription.setEnabled(false);
        mDescription.clearFocus();
        KeyBroadUtils.closeKeybord(this, mDescription);
        mEdit.setImageResource(R.mipmap.ic_edit);
        mIsInEditMode = false;
        hideDescriptionDelay();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_edit:
                if (!mIsInEditMode) {
                    enterEditMode();
                } else {
                    Photo photo = mPagerAdapter.getDetailInfo(mViewPager.getCurrentItem());
                    if (mDescription.getText().toString().equals(photo.getDescription())) {
                        exitEditMode();
                    } else {
                        mPresenter.modifyDescription(photo.getPid(), mDescription.getText().toString());
                    }
                }
                break;
            case R.id.vp_detail:
                if (mIsInEditMode)
                    return;
                if (mIsDescriptionShown) {
                    hideDescription();
                } else {
                    showDescription();
                }
                break;
            case R.id.rl_like:
                mHasThumbUp = !mHasThumbUp;
                mLike.setImageResource(mHasThumbUp ? R.mipmap.ic_thumb_up_red : R.mipmap.ic_thumb_up_grey);
                break;
            case R.id.iv_download:
                Photo photo = mPagerAdapter.getDetailInfo(mViewPager.getCurrentItem());
                mPresenter.downloadToLocal(photo.getDetailUrl(), mAlbumInfo.getTitle() + "_" + System.currentTimeMillis());
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.vp_detail:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mLastX = (int) event.getX();
                        mLastY = (int) event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        int x = (int) event.getX();
                        int y = (int) event.getY();
                        if (Math.abs(x - mLastX) <= CLICK_SLOP && Math.abs(y - mLastY) <= CLICK_SLOP) {
                            v.performClick();
                        }
                        break;
                }
                break;

            case R.id.scroll_description:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        mLastTouchScrollViewTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_UP:
                        hideDescriptionDelay();
                        break;
                }
                break;
        }
        return super.onTouch(v, event);
    }

    @Override
    public Activity getContactActivity() {
        return this;
    }
}
