package com.mediatek.gallery3d.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import com.android.gallery3d.app.PhotoDataAdapter.MavListener;
import com.android.gallery3d.util.ThreadPool.JobContext;

import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MediatekFeature.DataBundle;
import com.mediatek.gallery3d.util.MediatekFeature.Params;

public interface IMediaRequest {

    public DataBundle request(JobContext jc, Params params, String filePath);

    public DataBundle request(JobContext jc, Params params, byte[] data, 
                              int offset,int length);

    public DataBundle request(JobContext jc, Params params,
                              ContentResolver cr, Uri uri);
    
    public void setMavListener(MavListener listener);

}
