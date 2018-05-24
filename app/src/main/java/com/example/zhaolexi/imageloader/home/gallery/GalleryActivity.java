package com.example.zhaolexi.imageloader.home.gallery;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.graphics.drawable.ArgbEvaluator;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zhaolexi.imageloader.R;
import com.example.zhaolexi.imageloader.common.base.BaseActivity;
import com.example.zhaolexi.imageloader.common.net.OnRequestFinishListener;
import com.example.zhaolexi.imageloader.common.global.Result;
import com.example.zhaolexi.imageloader.home.album.AlbumFragment;
import com.example.zhaolexi.imageloader.home.manager.Album;
import com.example.zhaolexi.imageloader.home.manager.AlbumItemTouchHelperCallback;
import com.example.zhaolexi.imageloader.home.manager.ManagedAlbumAdapter;
import com.example.zhaolexi.imageloader.redirect.access.AlbumPasswordDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GalleryActivity extends BaseActivity<GalleryPresenter> implements GalleryViewInterface, View.OnClickListener, ManagedAlbumAdapter.OnItemClickListener, ManagedAlbumAdapter.OnItemAddListener, OnRequestFinishListener<Album> {

    public static final String ORIGIN_ALBUM = "album";

    private LinearLayout mContainer;
    private TabLayout mTabLayout;
    private ImageView mManage;
    private ViewPager mViewPager;
    private FloatingActionButton mFab;
    private ViewStub mStubGuide, mStubFinish, mStubManagedAlbum;
    private TextView mGuide, mFinish;
    private RecyclerView mManagedAlbumList;
    private AlertDialog.Builder mAlertBuilder;

    private AlbumPagerAdapter mPageAdapter;
    private ManagedAlbumAdapter mManagedAlbumAdapter;
    private List<Album> mAlbumList;
    private List<Album> mRandomList;
    private AlphaAnimation mAlphaAppear, mAlphaDisappear;
    private ValueAnimator mColorAppear, mColorDisappear, mRotateOpen, mRotateClose;

    public RecyclerView.RecycledViewPool mRecycledViewPool;
    public boolean mCanLoadWithoutWifi, mIsManagePageAnimating, mIsInManagePage;
    public final long DURATION = 150L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mAlbumList.isEmpty()) {
            showManagePage(false);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showManagePage(false);
        Album origin = (Album) intent.getSerializableExtra(ORIGIN_ALBUM);
        if (origin != null) {
            mPresenter.removeAlbum(origin);
            new AlbumPasswordDialog.Builder(this)
                    .setTitle(getString(R.string.add_album))
                    .setAccountDef(String.valueOf(origin.getAccount()))
                    .setCallback(this)
                    .build().show();
        }
    }

    @Override
    protected void initData() {
        mAlbumList = new ArrayList<>();
        mRandomList = new ArrayList<>();
        mPageAdapter = new AlbumPagerAdapter(getSupportFragmentManager(), mAlbumList);
        //从数据库中获取历史记录
        mAlbumList.addAll(mPresenter.getLocalHistory());
        preLoad();
        initAnimation();
    }

    private void initAnimation() {
        Interpolator linear = new LinearInterpolator();
        ValueAnimator.AnimatorUpdateListener colorListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mContainer.setBackgroundColor((int) animation.getAnimatedValue());
            }
        };
        ValueAnimator.AnimatorUpdateListener rotateListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mManage.setRotation((float) animation.getAnimatedValue());
            }
        };
        Animation.AnimationListener appearAnimationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mIsManagePageAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsManagePageAnimating = false;
                mIsInManagePage = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        Animation.AnimationListener disappearAnimationListener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                mIsManagePageAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mIsManagePageAnimating = false;
                mIsInManagePage = false;
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

        mRotateOpen = ValueAnimator.ofFloat(0, 45f);
        mRotateOpen.setDuration(DURATION);
        mRotateOpen.setInterpolator(linear);
        mRotateOpen.addUpdateListener(rotateListener);

        mRotateClose = ValueAnimator.ofFloat(45f, 0);
        mRotateClose.setDuration(DURATION);
        mRotateClose.setInterpolator(linear);
        mRotateClose.addUpdateListener(rotateListener);

        mColorAppear = ValueAnimator.ofInt(getResources().getColor(R.color.colorPrimary), getResources().getColor(R.color.windowBackground));
        mColorAppear.setEvaluator(ArgbEvaluator.getInstance());
        mColorAppear.setInterpolator(linear);
        mColorAppear.setDuration(DURATION);
        mColorAppear.addUpdateListener(colorListener);
        mColorAppear.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                mIsManagePageAnimating = false;
                mIsInManagePage = true;
                mContainer.setBackgroundColor(getResources().getColor(R.color.windowBackground));
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsManagePageAnimating = false;
                mIsInManagePage = true;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                mIsManagePageAnimating = true;
            }
        });

        mColorDisappear = ValueAnimator.ofInt(getResources().getColor(R.color.windowBackground), getResources().getColor(R.color.colorPrimary));
        mColorDisappear.setEvaluator(ArgbEvaluator.getInstance());
        mColorDisappear.setInterpolator(linear);
        mColorDisappear.setDuration(DURATION);
        mColorDisappear.addUpdateListener(colorListener);
        mColorDisappear.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                mIsManagePageAnimating = false;
                mIsInManagePage = false;
                mContainer.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mIsManagePageAnimating = false;
                mIsInManagePage = false;
            }

            @Override
            public void onAnimationStart(Animator animation) {
                mIsManagePageAnimating = true;
            }
        });
    }

    @Override
    protected void initView() {
        setContentView(R.layout.activity_gallery);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        mContainer = (LinearLayout) findViewById(R.id.ll_container);
        mManage = (ImageView) findViewById(R.id.iv_manager);
        mManage.setOnClickListener(this);
        mStubGuide = (ViewStub) findViewById(R.id.stub_guide);
        mStubFinish = (ViewStub) findViewById(R.id.stub_finish);
        mStubManagedAlbum = (ViewStub) findViewById(R.id.stub_managed_album);

        mViewPager = (ViewPager) findViewById(R.id.vp_album);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            //ViewPager在初始化数据时不会回调onPageSelected方法
            //并且就算调用setCurrentItem(0)，也会因为currentItem位置没有变化而不回调onPageSelected
            //所以也要在初始化时进行设置

            //会在setCurrentItem或者scrollToItem之后回调
            //只有当前位置发生改变才会被调用
            //如果删除当前item然后调用notifyDataSetChanged，也会调用setCurrentItem从而出发onPageSelected
            @Override
            public void onPageSelected(final int position) {
                if (!mIsInManagePage) {
                    //FAB动画
                    FabBehavior behavior = (FabBehavior) ((CoordinatorLayout.LayoutParams) (mFab.getLayoutParams())).getBehavior();
                    if (isCurrentAlbumAccessible(position)) {
                        behavior.startOpening(mFab);
                    } else {
                        behavior.startClosing(mFab);
                    }
                }

                initFabAndDialog(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mViewPager.setAdapter(mPageAdapter);
        mTabLayout = (TabLayout) findViewById(R.id.tabLayout);
        //与ViewPager进行绑定（初始化监听器、同步适配器数据等,最终在populateFromPagerAdapter中addTab）
        mTabLayout.setupWithViewPager(mViewPager);

        mAlertBuilder = new AlertDialog.Builder(this);
        mAlertBuilder.setCancelable(false);
        mAlertBuilder.setMessage("您当前为非Wifi环境，是否继续加载图片");
        mAlertBuilder.setTitle("注意");
        mAlertBuilder.setNegativeButton("否", null);

        mFab = (FloatingActionButton) findViewById(R.id.add_photos);
        mFab.setVisibility(isCurrentAlbumAccessible(0) ? View.VISIBLE : View.GONE);

        initFabAndDialog(0);
    }

    private void initFabAndDialog(final int position) {
        //设置FAB回调
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlbumFragment fragment = mPageAdapter.getAlbumFragmentAt(position);
                if (fragment != null) {
                    fragment.getPresenter().uploadPhoto();
                }
            }
        });

        //设置AlertDialog监听事件
        mAlertBuilder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCanLoadWithoutWifi = true;
                AlbumFragment fragment = mPageAdapter.getAlbumFragmentAt(position);
                if (fragment != null) {
                    fragment.getPresenter().refresh();
                }
            }
        });
    }

    @Override
    protected GalleryPresenter createPresenter() {
        return new GalleryPresenter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.collect_album:
                break;
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mManagedAlbumAdapter != null && mManagedAlbumAdapter.isEditable() && !mAlbumList.isEmpty()) {
            mManagedAlbumAdapter.setEditable(false, true);
        } else if (mIsInManagePage && !mAlbumList.isEmpty()) {
            dismissManagePage();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public void showAlertDialog() {
        mAlertBuilder.show();
    }

    @Override
    public void showManagePage(boolean animated) {
        if (mViewPager.getCurrentItem() != mPresenter.getCurrentPage()) {
            mPresenter.setCurrentPage(mViewPager.getCurrentItem());     //记录当前位置
            if (mManagedAlbumAdapter != null) {
                mManagedAlbumAdapter.notifyDataSetChanged();
            }
        }
        initManagePage();
        mTabLayout.setVisibility(View.GONE);
        mFab.clearAnimation();
        mFab.setVisibility(View.GONE);
        if (!animated) {
            mManage.setRotation(45f);
            mContainer.setBackgroundColor(getResources().getColor(R.color.windowBackground));
            mIsInManagePage = true;
        } else {
            mColorAppear.start();
            mRotateOpen.start();
            mGuide.startAnimation(mAlphaAppear);
            mManagedAlbumList.startAnimation(mAlphaAppear);
        }
    }

    @Override
    public void dismissManagePage() {
        mColorDisappear.start();
        mRotateClose.start();
        mGuide.startAnimation(mAlphaDisappear);
        mManagedAlbumList.startAnimation(mAlphaDisappear);
        mGuide.clearAnimation();
        mGuide.setVisibility(View.GONE);
        mManagedAlbumList.clearAnimation();
        mManagedAlbumList.setVisibility(View.GONE);

        int currentPos = mPresenter.getCurrentPage();
        if (mViewPager.getCurrentItem() != currentPos) {
            mViewPager.setCurrentItem(mPresenter.getCurrentPage());   //变更到新的位置
        }
        mFab.setVisibility(isCurrentAlbumAccessible(currentPos) ? View.VISIBLE : View.GONE);
        initFabAndDialog(currentPos);
        mTabLayout.setVisibility(View.VISIBLE);
        preLoad();
    }

    @Override
    public void showRandom(List<Album> albums) {
        mRandomList.clear();
        mRandomList.addAll(albums);
        if (mManagedAlbumAdapter != null) {
            int positionStart = mManagedAlbumAdapter.getLocalAlbumCount() + 2;
            mManagedAlbumAdapter.notifyItemRangeChanged(positionStart, albums.size());
        }
    }

    @Override
    public void showError(String reason) {
        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAlbumListStateChanged(boolean isEmpty, boolean editable) {
        if (mFinish == null) {
            mFinish = (TextView) mStubFinish.inflate();
            mFinish.setOnClickListener(this);
        }

        if (isEmpty) {
            mGuide.setText(R.string.manage_guide_empty);
            mFinish.setVisibility(View.GONE);
            mManage.setVisibility(View.GONE);
            mManagedAlbumAdapter.setEditable(false, false);
        } else if (editable) {
            mGuide.setText(R.string.manage_guide_editable);
            mFinish.setVisibility(View.VISIBLE);
            mManage.setVisibility(View.GONE);
        } else {
            mGuide.setText(R.string.manage_guide_uneditable);
            mFinish.setVisibility(View.GONE);
            mManage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public ManagedAlbumAdapter getAlbumAdapter() {
        return mManagedAlbumAdapter;
    }

    @Override
    public AlbumPagerAdapter getPagerAdapter() {
        return mPageAdapter;
    }

    private boolean isCurrentAlbumAccessible(int position) {
        if (!mAlbumList.isEmpty()) {
            Album album = mAlbumList.get(position);
            if (album.isAccessible() && !TextUtils.isEmpty(album.getAid())) {
                return true;
            }
        }
        return false;
    }

    private void preLoad() {
        mPresenter.getRandom();
    }

    private void initManagePage() {
        if (mGuide == null) {
            mGuide = (TextView) mStubGuide.inflate();
        }
        if (mManagedAlbumList == null) {
            mManagedAlbumList = (RecyclerView) mStubManagedAlbum.inflate();
            mManagedAlbumAdapter = new ManagedAlbumAdapter(this, mAlbumList, mRandomList);
            mManagedAlbumAdapter.setOnItemClickListener(this);
            mManagedAlbumAdapter.setOnItemAddListener(this);
            mManagedAlbumList.setAdapter(mManagedAlbumAdapter);
            GridLayoutManager manager = new GridLayoutManager(this, 3);
            manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (mManagedAlbumAdapter.getItemViewType(position) == ManagedAlbumAdapter.TYPE_HINT) {
                        return 3;
                    } else {
                        return 1;
                    }
                }
            });
            mManagedAlbumList.setLayoutManager(manager);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new AlbumItemTouchHelperCallback(mManagedAlbumAdapter));
            itemTouchHelper.attachToRecyclerView(mManagedAlbumList);
            mManagedAlbumAdapter.setItemTouchHelper(itemTouchHelper);
        }
        mGuide.setVisibility(View.VISIBLE);
        mManagedAlbumList.setVisibility(View.VISIBLE);
        if (mAlbumList.isEmpty()) {
            mGuide.setText(R.string.manage_guide_empty);
            mManage.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_manager:
                if (!mIsManagePageAnimating) {
                    if (!mIsInManagePage) {
                        showManagePage(true);
                    } else {
                        dismissManagePage();
                    }
                }
                break;
            case R.id.tv_finish:
                mManagedAlbumAdapter.setEditable(false, true);
                if (mPresenter.shouldUpdateState()) {
                    mPageAdapter.notifyDataSetChanged();
                    mPresenter.clearUpdateState();
                }
                break;
        }
    }


    @Override
    public void onItemClick(boolean isFromRandom, int position, boolean editable) {
        if (!editable) {
            //非编辑状态下点击相册跳转到相册页面，如果该相册在本地相册中不存在（即随机相册），则将该相册添加到本地相册中
            Album album = mManagedAlbumAdapter.getAlbum(position);
            mPresenter.addAlbum(album);
            dismissManagePage();
        } else if (!isFromRandom) {
            //编辑状态下点击本地相册则移除相册
            mPresenter.removeAlbum(position);
        } else {
            //编辑状态下点击随机相册则添加到本地相册中（可增加移动动画）
            mPresenter.addAlbum(mManagedAlbumAdapter.removeAlbum(position));
        }
    }

    @Override
    public void onClickItemAdd() {
        new AlbumPasswordDialog.Builder(this)
                .setTitle(getString(R.string.add_album))
                .setCallback(this)
                .build().show();
    }

    @Override
    public void onSuccess(Album album) {
        if (album != null) {
            mPresenter.addAlbum(album);
        }
    }

    @Override
    public void onFail(String reason, Result result) {
    }

    @Override
    public Activity getContactActivity() {
        return this;
    }
}