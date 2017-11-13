package com.example.zhaolexi.imageloader.model;

import android.graphics.Bitmap;
import android.os.SystemClock;

import com.example.zhaolexi.imageloader.base.MyApplication;
import com.example.zhaolexi.imageloader.presenter.ImageDetailPresenter;
import com.example.zhaolexi.imageloader.utils.loader.ImageLoader;

import java.io.IOException;

/**
 * Created by ZHAOLEXI on 2017/11/14.
 */

public class ImageDetailModelImpl implements ImageDetailModel {

    private ImageDetailPresenter mPresenter;
    private ImageLoader mImageLoader;

    public ImageDetailModelImpl(ImageDetailPresenter presenter) {
        mPresenter = presenter;
        mImageLoader = ImageLoader.getInstance(MyApplication.getContext());
    }

    @Override
    public void loadBitmapFromDisk(final String url, final ImageDetailPresenter.onLoadFinishListener listener) {
        new Thread(new Runnable() {
            Bitmap bitmap = null;
            long current=SystemClock.currentThreadTimeMillis();
            @Override
            public void run() {
                while(bitmap==null&&SystemClock.currentThreadTimeMillis()-current<10000){
                    try {
                        bitmap=mImageLoader.loadRawBitmap(url);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (bitmap != null) {
                    listener.onSuccess(bitmap);
                }else{
                    listener.onFail("加载超时");
                }
            }
        }).start();
    }
}