package com.example.zhaolexi.imageloader.home.album;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.example.zhaolexi.imageloader.R;
import com.example.zhaolexi.imageloader.common.base.BaseApplication;
import com.example.zhaolexi.imageloader.common.base.BasePresenter;
import com.example.zhaolexi.imageloader.common.net.OnRequestFinishListener;
import com.example.zhaolexi.imageloader.common.utils.NetUtils;
import com.example.zhaolexi.imageloader.detail.DetailActivity;
import com.example.zhaolexi.imageloader.detail.PhotoDetailActivity;
import com.example.zhaolexi.imageloader.home.InteractInterface;
import com.example.zhaolexi.imageloader.home.manager.Album;
import com.example.zhaolexi.imageloader.redirect.router.RedirectCallback;
import com.example.zhaolexi.imageloader.redirect.router.Result;
import com.example.zhaolexi.imageloader.redirect.router.Router;
import com.example.zhaolexi.imageloader.upload.UpLoadPhotoActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ZHAOLEXI on 2017/10/14.
 */

public class AlbumPresenter extends BasePresenter<AlbumViewInterface, AlbumModel> {

    private int currentPage;
    private boolean hasMoreData = true;

    private static final int REFRESH_FINISH = 1;
    public static final int SELECT_PHOTO = 2;
    public static final int OPEN_PHOTO_DETAIL = 3;

    @SuppressLint("HandlerLeak")
    private Handler mCollectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SUCCESS:
                    if (isViewAttached()) {
                        getView().collectSuccess((String) msg.obj);
                    }
                    break;
                case MSG_FAIL:
                    if (isViewAttached()) {
                        getView().collectFail((String) msg.obj);
                    }
                    break;
            }
        }
    };

    @Override
    protected AlbumModel newModel() {
        return new AlbumModelImpl();
    }

    @Override
    public void attachView(AlbumViewInterface view) {
        super.attachView(view);
    }

    @Override
    protected void onMessageSuccess(Message msg) {
        if(isViewAttached()) {
            AlbumViewInterface mView = getView();
            mView.setRefreshing(false);
            //刷新成功，更新适配器中的数据
            if (msg.arg1 == REFRESH_FINISH) {
                mView.onRefreshFinish();
            }
            List<Photo> newData = (List<Photo>) msg.obj;
            mView.showNewData(hasMoreData, newData);
            currentPage++;
        }
    }

    @Override
    protected void onMessageFail(Message msg) {
        if(isViewAttached()) {
            AlbumViewInterface mView = getView();
            mView.setRefreshing(false);
            //刷新失败，保留原始数据，并显示错误信息
            mView.showError();
        }
    }

    public void setUrl(String url) {
        mModel.setUrl(url);
    }

    public void setCurrentPage(int page) {
        currentPage = page;
    }

    public void loadMore() {
        if (isViewAttached()) {
            AlbumViewInterface albumView = getView();
            if (hasMoreData) {
                if (currentPage == 0) {
                    //加载失败的情况
                    albumView.setRefreshing(true);
                } else {
                    albumView.showLoading();
                }
                mModel.loadImage(currentPage + 1, new OnRequestFinishListener<List<Photo>>() {
                    @Override
                    public void onSuccess(List<Photo> newData) {
                        hasMoreData = newData.size() >= 10;
                        Message message = Message.obtain(mHandler, MSG_SUCCESS, newData);
                        message.sendToTarget();
                    }

                    @Override
                    public void onFail(String reason, Result result) {
                        mHandler.sendEmptyMessage(MSG_FAIL);

                        if (result != null && isViewAttached()) {
                            final Activity activity = getView().getContactActivity();
                            new Router.Builder(activity)
                                    .setOriginAlbum(getView().getAlbumInfo())
                                    .build().route(result);
                        }
                    }
                });
            }
        }
    }

    public void refresh() {

        if (isViewAttached()) {

            AlbumFragment fragment = (AlbumFragment) getView();
            InteractInterface interact = (InteractInterface) fragment.getActivity();
            //如果wifi可用或者已同意使用流量，更新图片，否则将会弹出警告
            if (!interact.canLoadWithoutWifi() && !NetUtils.isWifiAvailable(BaseApplication.getContext())) {
                getView().showAlertDialog();
                return;
            }

            getView().setRefreshing(true);
            hasMoreData = true;
            currentPage = 0;

            mModel.loadImage(currentPage + 1, new OnRequestFinishListener<List<Photo>>() {
                @Override
                public void onSuccess(List<Photo> newData) {
                    hasMoreData = newData.size() >= 10;
                    //OkHttp是在子线程中执行回调方法的，所以要通过handler切换到主线程
                    Message message = Message.obtain(mHandler, MSG_SUCCESS, newData);
                    message.arg1 = REFRESH_FINISH;
                    message.sendToTarget();
                }

                @Override
                public void onFail(String reason, Result result) {
                    Message message = Message.obtain(mHandler, MSG_FAIL);
                    message.arg1 = REFRESH_FINISH;
                    message.sendToTarget();

                    if (result != null && isViewAttached()) {
                        final Activity activity = getView().getContactActivity();
                        new Router.Builder(activity)
                                .setOriginAlbum(getView().getAlbumInfo())
                                .setLoginCallback(new RedirectCallback() {
                                    @Override
                                    protected void onCallback(boolean success) {
                                        if (success) {
                                            refresh();
                                        }
                                    }
                                })
                                .build().route(result);
                    }
                }
            });
        }
    }

    public void collectAlbum(final Album album) {
        mModel.collectAlbum(album.getAid(), new OnRequestFinishListener() {
            @Override
            public void onSuccess(Object data) {
                if (!album.isFavorite()) {
                    Message.obtain(mCollectHandler, MSG_SUCCESS, "收藏成功").sendToTarget();
                } else {
                    Message.obtain(mCollectHandler, MSG_SUCCESS, "取消收藏").sendToTarget();
                }
            }

            @Override
            public void onFail(String reason, Result result) {
                if (result != null && isViewAttached()) {
                    final Activity activity = getView().getContactActivity();
                    Router router = new Router.Builder(activity)
                            .setOriginAlbum(getView().getAlbumInfo())
                            .setLoginCallback(new RedirectCallback() {
                                @Override
                                protected void onCallback(boolean success) {
                                    if (success) {
                                        collectAlbum(album);
                                    }
                                }
                            })
                            .build();
                    if (router.route(result)) {
                        Message.obtain(mCollectHandler, MSG_FAIL, reason).sendToTarget();
                    }
                } else {
                    Message.obtain(mCollectHandler, MSG_FAIL, reason).sendToTarget();
                }
            }
        });
    }

    public void openDetail(ArrayList<Photo> details, int index, Album album) {
        if (isViewAttached()) {
            AlbumFragment fragment = (AlbumFragment) getView();
            Intent intent = new Intent(fragment.getContext(), PhotoDetailActivity.class);
            intent.putExtra(DetailActivity.DETAILS_KEY, details);
            intent.putExtra(DetailActivity.ALBUM_URL, mModel.getUrl());
            intent.putExtra(DetailActivity.CURRENT_PAGE, currentPage);
            intent.putExtra(DetailActivity.CURRENT_INDEX, index);
            intent.putExtra(PhotoDetailActivity.ALBUM, album);
            fragment.startActivityForResult(intent, OPEN_PHOTO_DETAIL);
            fragment.getActivity().overridePendingTransition(R.anim.enter_anim, R.anim.exit_anim);
        }
    }

    public void openSelectPhotoPage() {
        if (isViewAttached()) {
            AlbumFragment fragment = (AlbumFragment) getView();
            Album albumInfo = fragment.getAlbumInfo();
            if (albumInfo.isAccessible() && !TextUtils.isEmpty(albumInfo.getAid())) {
                Intent intent = new Intent(fragment.getContext(), UpLoadPhotoActivity.class);
                intent.putExtra(UpLoadPhotoActivity.ALBUM, albumInfo);
                fragment.startActivityForResult(intent, SELECT_PHOTO);
            }
        }
    }

}
