package com.example.zhaolexi.imageloader.upload;

import android.graphics.Bitmap;

import java.util.Set;
import java.util.TreeSet;

/**
 * Created by ZHAOLEXI on 2017/11/3.
 */

public class PhotoBucket {

    private int count;
    private String name;
    private Set<LocalPhoto> photoSet;
    private Bitmap cover;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Bitmap getCover() {
        return cover;
    }

    public void setCover(Bitmap cover) {
        this.cover = cover;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<LocalPhoto> getPhotoSet() {
        if (photoSet == null) {
            photoSet = new TreeSet<>();
        }
        return photoSet;
    }

    public void setPhotoSet(Set<LocalPhoto> photoSet) {
        this.photoSet = photoSet;
    }
}
