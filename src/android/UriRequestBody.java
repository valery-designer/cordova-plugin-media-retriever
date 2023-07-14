package com.valerydesigner.mediaretriever;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

import java.io.IOException;
import java.io.InputStream;


public class UriRequestBody extends RequestBody {

    protected Uri uri;
    protected ContentResolver contentResolver;

    public UriRequestBody(Uri uri, ContentResolver contentResolver) {
        this.uri = uri;
        this.contentResolver = contentResolver;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        String contentType = contentResolver.getType(uri);
        return contentType != null ? MediaType.parse(contentType) : null;
    }

    @Override
    public void writeTo(@NonNull BufferedSink bufferedSink) throws IOException {

        InputStream inputStream = null;

        try {
            inputStream = contentResolver.openInputStream(uri);
        } catch (IOException e) {
            Log.i("wera", "Error1: " + e.getMessage());
        }

        if(inputStream != null) {
            Source source = Okio.source(inputStream);
            bufferedSink.writeAll(source);
        }
    }

    @Override
    public long contentLength() {
        AssetFileDescriptor assetFileDescriptor = null;
        long length = -1;

        try {
            assetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r");
        } catch(IOException e) {
            Log.i("wera", "Error2: " + e.getMessage());
        }

        if(assetFileDescriptor != null){
            length = assetFileDescriptor.getLength();
            try {
                assetFileDescriptor.close();
            }
            catch (IOException e) {
                Log.i("wera", "Error3: " + e.getMessage());
            }
        }

        return length;
    }
}
