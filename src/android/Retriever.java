package com.valerydesigner.mediaretriever;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

// from camera
import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import androidx.core.content.FileProvider;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.cordova.BuildHelper;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
// from camera

public class Retriever extends CordovaPlugin {

    private CallbackContext theCallbackContext;

    private int mQuality;                   // Compression quality hint (0-100: 0=low quality & high compression, 100=compress of max quality)
    private int targetWidth;                // target width of the image
    private int targetHeight;               // target height of the image
    private Uri imageUri;                   // Uri of captured image
    private String imageFilePath;           // File where the image is stored
    private int encodingType;               // Type of encoding to use
    private int mediaType;                  // What type of media to retrieve
    private int destType;                   // Source type (needs to be saved for the permission handling)
    private int srcType;                    // Destination type (needs to be saved for permission handling)
    private boolean saveToPhotoAlbum;       // Should the picture be saved to the device's photo album
    private boolean correctOrientation;     // Should the pictures orientation be corrected
    private boolean orientationCorrected;   // Has the picture's orientation been corrected
    private boolean allowEdit;              // Should we allow the user to crop the image.
    private int rotate;

    private class BitmapAndInfo {
        public Bitmap bitmap;
        public int width;
        public int height;
        public BitmapAndInfo() {
            bitmap = null;
            width = 0;
            height = 0;
        }
    }


    private int actionsInProcess;

    private static final String TIME_FORMAT = "yyyyMMdd_HHmmss";
    private static final String LOG_TAG = "wera";
    private static final String JPEG_EXTENSION = ".jpg";
    private static final String PNG_EXTENSION = ".png";
    private static final int JPEG = 0;                  
    private static final int PNG = 1;                  
    private static final String PNG_MIME_TYPE = "image/png";
    private static final String JPEG_MIME_TYPE = "image/jpeg";

    private ExifHelper exifData;            // Exif data from source

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.targetWidth = 200;
        this.targetHeight = 200;
        this.mQuality = 80;
        this.correctOrientation = true;
        this.rotate = 0;
        this.actionsInProcess = 0;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        theCallbackContext = callbackContext;
        if(action.equals("uploadMedia")) {
            int itemId = args.getInt(0);
            String mediaToUploadUri = args.getString(1);
            String mediaType = args.getString(2);
            String queryUrl = args.getString(3);
            String token = args.getString(4);
            String chatId = args.getString(5);
            String fromUser = args.getString(6);
            String extraPack = args.getString(7);
//            String thumbnail = args.getString(3);
//            String chatId = args.getString(4);
//            String fromUser = args.getString(5);
//            int mediaWidth = args.getInt(6);
//            int mediaHeight = args.getInt(7);
//                    [ holyPack.itemId, holyPack.theUri, holyPack.mediaType, holyPack.queryUrl, holyPack.token,
//                    holyPack.chatId, holyPack.fromUser, ep ]

            this.actionsInProcess++;
            this.uploadMedia(itemId, mediaToUploadUri, mediaType, queryUrl,token, chatId, fromUser, extraPack);
            return true;
        }

//        if(action.equals("uploadMedia")) {
//            int itemId = args.getInt(1);
//            String mediaToUploadUri = args.getString(0);
//            String mediaType = args.getString(2);
//            String thumbnail = args.getString(3);
//            String chatId = args.getString(4);
//            String fromUser = args.getString(5);
//            int mediaWidth = args.getInt(6);
//            int mediaHeight = args.getInt(7);
//            String token = args.getString(8);
//            this.actionsInProcess++;
//            this.uploadMedia(itemId, mediaToUploadUri, mediaType, thumbnail, chatId, fromUser, mediaWidth, mediaHeight, token);
//            return true;
//        }
        if(action.equals("getPreview")) {
            this.actionsInProcess++;
            String uri = args.getString(0);
            int itemId = args.getInt(1);
            int maxSideSize = args.getInt(2);
            int maxPreviewSideSize = args.getInt(3);
            int jpegQuality = args.getInt(4);
            this.scaleAndGetPreview( uri, itemId, maxSideSize, maxPreviewSideSize, jpegQuality, theCallbackContext );
            return true;
        }
        if(action.equals("getVideoPreview")) {
            this.actionsInProcess++;
            int itemId = args.getInt(1);
            String uri = args.getString(0);
            this.getVideoPreview( uri, itemId );
            return true;
        }
        if(action.equals("getAudioInfo")) {
            this.actionsInProcess++;
            int itemId = args.getInt(1);
            String uri = args.getString(0);
            this.getAudioInfo( uri, itemId );
            return true;
        }
        if(action.equals("getFilesFromPicker")) {  
            this.selectFiles();
            return true;
        }


        return false;
    }

    private void selectFiles() {
        try {
            // JSONObject args = new JSONObject();
            // Long ts = System.currentTimeMillis();
            // args.put("status", "OK");
                 //args.put("itemId", itemId);
            // args.put("timeStamp", ts);
            // args.put("items", "items_list");
                //args.put("thumbnail", "data:image/jpg;base64,"+ previewDataUri );
            this.getFiles("*/*");
            // this.getFiles("audio/mpeg,audio/x-wav,audio/wav,audio/ogg,audio/webm,audio/3gpp");
        } catch(Exception e) {
            sendErrorToJS("Method sendVideoPreviewSuccess throws: ", 315, e);
            e.printStackTrace();
        }
    }


    public void getFiles (String accept) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        if (!accept.equals("*/*")) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, accept.split(","));
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        //this.includeData = includeData;

        Intent chooser = Intent.createChooser(intent, "Select File");
        cordova.startActivityForResult(this, chooser, 1);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        theCallbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        ArrayList<String> fileUris = new ArrayList<>();
        JSONArray items = new JSONArray();

        Long ts = System.currentTimeMillis();

        try {
            if (requestCode == 1 && theCallbackContext != null) {
                if (resultCode == Activity.RESULT_OK) {

                    if (data != null) {
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                Uri uri = clipData.getItemAt(i).getUri();
                                JSONObject item = new JSONObject();
                                item.put("uri", uri.toString());
                                item.put("mimeType", getMimeType(uri));
                                item.put("displayName", getDisplayName(uri));
                                item.put("size", getSize(uri));
                                items.put(item);
                                fileUris.add(uri.toString());
                            }
                        }
                        else {
                            Uri uri = data.getData();
                            JSONObject item = new JSONObject();
                            item.put("uri", uri.toString());
                            item.put("mimeType", getMimeType(uri));
                            item.put("displayName", getDisplayName(uri));
                            item.put("size", getSize(uri));
                            items.put(item);
                            fileUris.add(uri.toString());
                            Log.i("wera", "SELECTED Item DISPLAY NAME from getDisplayName: " + getDisplayName(uri));
                        }

                        for(int i=0; i<fileUris.size(); i++){
                            Log.i("wera", "SELECTED Item "+ i + ": "+fileUris.get(i));
                        }

                    }


                    if (fileUris != null && fileUris.size() >0) {
//                        ContentResolver contentResolver = this.cordova.getActivity().getContentResolver();

//                        String name = Chooser.getDisplayName(contentResolver, uri);

//                        String mediaType = contentResolver.getType(uri);
//                        if (mediaType == null || mediaType.isEmpty()) {
//                            mediaType = "application/octet-stream";
//                        }

//                        String base64 = "";
//
//                        if (this.includeData) {
//                            byte[] bytes = Chooser.getBytesFromInputStream(
//                                    contentResolver.openInputStream(uri)
//                            );
//
//                            base64 = Base64.encodeToString(bytes, Base64.DEFAULT);
//                        }

                        JSONObject args = new JSONObject();
//                        args.put("data", base64);
//                        args.put("mediaType", mediaType);
//                        args.put("name", name);
//                        args.put("uri", uri.toString());
                        args.put("status", "ok");
                        //args.put("itemId", itemId);
                        args.put("timeStamp", ts);
                        args.put("items", items);
                        args.put("message", "-------->SELECT FILES RESULT: OK");
                        PluginResult result = new PluginResult( PluginResult.Status.OK, args );
                        theCallbackContext.sendPluginResult(result);
                    }
                    else {
//                        this.callback.error("File URI was null.");
                        sendErrorToJS("Null URI has been received: ",0);
                    }
                }
                else if (resultCode == Activity.RESULT_CANCELED) {
//                    this.callback.success("RESULT_CANCELED");
                    JSONObject args = new JSONObject();

//                        args.put("data", base64);
//                        args.put("mediaType", mediaType);
//                        args.put("name", name);
                    args.put("uri", "");
                    args.put("status", "CANCELLED");
                    //args.put("itemId", itemId);
                    args.put("timeStamp", ts);
                    args.put("items", "items_list");
                    args.put("message", "-------->SELECT FILES RESULT CANCELLED");

                    PluginResult result = new PluginResult( PluginResult.Status.OK, args );
                    theCallbackContext.sendPluginResult(result);
                }
                else {
//                    this.callback.error(resultCode);
                    sendErrorToJS("Unknown error with code ", resultCode);
                }
            }
        }
        catch (Exception err) {
//            this.callback.error("Failed to read file: " + err.toString());
            sendErrorToJS("File reading error: ",0, err);
        }
    }

    private String getDisplayName (Uri uri) {
        ContentResolver contentResolver = this.cordova.getActivity().getContentResolver();
        String[] pr = {MediaStore.MediaColumns.DISPLAY_NAME};
        Cursor cursor = contentResolver.query(uri, pr, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return "";
    }
    
    private String getMimeType(Uri uri){
        ContentResolver contentResolver = this.cordova.getActivity().getContentResolver();
        String mt = contentResolver.getType(uri);
        if (mt == null || mt.isEmpty()) {
            mt = "application/octet-stream";
        }
        return mt;
    }

    private String getSize (Uri uri) {
        ContentResolver contentResolver = this.cordova.getActivity().getContentResolver();
        String[] pr = {MediaStore.MediaColumns.SIZE};
        Cursor cursor = contentResolver.query(uri, pr, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return "";
    }

    @Nullable
    private Uri buildMediaStoreUriOutOfPickerUri (Uri rawUri) {
        String[] segments = rawUri.getLastPathSegment().split(":");
        if (segments.length < 2) return null;
        if(segments[0].equals("video") && isInteger(segments[1])){
            Uri newUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,Integer.parseInt(segments[1]));
            return newUri;
        }
        if(segments[0].equals("audio") && isInteger(segments[1])){
            Uri newUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,Integer.parseInt(segments[1]));
            return newUri;
        }
        return null;
    }

    private boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    public void dumpVideos() {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Video.VideoColumns.DATA };
        Cursor c = this.cordova.getActivity().getContentResolver().query(uri, projection, null, null, null);
        int vidsCount = 0;
        if (c != null) {
            vidsCount = c.getCount();
            while (c.moveToNext()) {
                Log.i("wera", c.getString(0));
            }
            c.close();
        }
        Log.i("wera", "Total count of videos: " + vidsCount);
    }

    public void getVideoPreview( String stringUri, int itemId ) {

        Uri mediaUri = Uri.parse(stringUri);
	
        if(mediaUri.getAuthority().equals("com.android.providers.media.documents")) {
            Uri newUri = buildMediaStoreUriOutOfPickerUri(mediaUri);
            if (newUri!=null) mediaUri = newUri;
        }

        int id = 0;
        JSONObject info = new JSONObject();
        Cursor c;
        String[] columns = {
                MediaStore.Video.VideoColumns._ID,
                MediaStore.Video.VideoColumns.WIDTH,
                MediaStore.Video.VideoColumns.HEIGHT,
                MediaStore.Video.VideoColumns.DATA,
                MediaStore.Video.VideoColumns.DURATION,
                MediaStore.Video.VideoColumns.MIME_TYPE,
                MediaStore.Video.VideoColumns.SIZE,
                MediaStore.Video.VideoColumns.DISPLAY_NAME,
                MediaStore.Video.VideoColumns.DATE_TAKEN,
                MediaStore.Video.VideoColumns.RESOLUTION,
                MediaStore.Video.VideoColumns.TITLE,
        };

        c = this.cordova.getActivity().getContentResolver().query(mediaUri, columns, null, null, null);
        if (c!=null) {

            try {
                if (c.moveToFirst()) {
                    id = Integer.parseInt( c.getString(0) );
                    int width = Integer.parseInt( c.getString(1) );
                    int height = Integer.parseInt( c.getString(2) );
                    String path = c.getString(3) ;
                    int duration = Integer.parseInt( c.getString(4) );
                    String mimeType = c.getString(5);
                    int size = Integer.parseInt( c.getString(6) );
                    String displayName = c.getString(7);
                    String dateTaken = c.getString(8);
                    String resolution = c.getString(9);
                    String title = c.getString(10);

                    
                    info.put("width", width);
                    info.put("height", height);
                    info.put("size", size);
                    info.put("duration", duration);
                    info.put("mime_type", mimeType);
                    info.put("resolution", resolution);
                    info.put("date_taken", dateTaken);
                    info.put("path", path);
                }
            } catch(Exception e) {
                this.sendErrorToJS("forming video info object has a dramatic outcome: ", itemId, e);
            } finally {
                c.close();
            }
        

            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize = 1;
            Bitmap bitmap;
                        
            ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();
            CompressFormat compressFormat = CompressFormat.JPEG;
    
            try {
                bitmap =  MediaStore.Video.Thumbnails.getThumbnail(this.cordova.getActivity().getContentResolver(), id, MediaStore.Video.Thumbnails.MINI_KIND, options);
                if (bitmap.compress(compressFormat, mQuality, jpeg_data)) {
                    byte[] code = jpeg_data.toByteArray();
                    byte[] output = Base64.encode(code, Base64.NO_WRAP);
                    String js_out = new String(output);
    
                    this.sendVideoPreviewSuccess(itemId, js_out, info);
                    
                    js_out = null;
                    output = null;
                    code = null;
                }
            } catch (Exception e) { 
                this.sendErrorToJS("Error retrieving preview image for video ", itemId);
            }
            jpeg_data = null;
        }
    }

    public void getAudioInfo( String stringUri, int itemId ) {

        Uri mediaUri = Uri.parse(stringUri);
	
        if(mediaUri.getAuthority().equals("com.android.providers.media.documents")) {
            Uri newUri = buildMediaStoreUriOutOfPickerUri(mediaUri);
            if (newUri!=null) mediaUri = newUri;
        }

        int id = 0;
        JSONObject info = new JSONObject();
        Cursor c = null;
        String[] columns = {
                MediaStore.Audio.AudioColumns._ID,
                // MediaStore.Audio.AudioColumns.WIDTH,
                // MediaStore.Audio.AudioColumns.HEIGHT,
                // MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.MIME_TYPE,
                MediaStore.Audio.AudioColumns.SIZE,
                MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                MediaStore.Audio.AudioColumns.DATE_TAKEN,
                // MediaStore.Audio.AudioColumns.RESOLUTION,
                MediaStore.Audio.AudioColumns.TITLE,
        };

        try {
            c = this.cordova.getActivity().getContentResolver().query(mediaUri, columns, null, null, null);
        } catch(Exception e){
            Log.i("wera", "query for a cursor has a dramatic outcome: " + e.getMessage());
        }
        
        if (c!=null) {
            try {
                if (c.moveToFirst()) {
                    id = Integer.parseInt( c.getString(0) ); Log.i("wera", "===> Audio info: ID: " + id);
                    // int width = Integer.parseInt( c.getString(1) ); Log.i("wera", "===> Audio info: width: " + width);
                    // int height = Integer.parseInt( c.getString(2) ); Log.i("wera", "===> Audio info: height: " + height);
                    // String path = c.getString(3); Log.i("wera", "===> Audio info: path: " + path);
                    int duration = Integer.parseInt( c.getString(1) ); Log.i("wera", "===> Audio info: duration: " + duration);
                    String mimeType = c.getString(2); Log.i("wera", "===> Audio info: mimeType: " + mimeType);
                    int size = Integer.parseInt( c.getString(3) ); Log.i("wera", "===> Audio info: size: " + size);
                    String displayName = c.getString(4); Log.i("wera", "===> Audio info: displayName: " + displayName);
                    String dateTaken = c.getString(5); Log.i("wera", "===> Audio info: dateTaken: " + dateTaken);
                    //String resolution = c.getString(9);
                    String title = c.getString(6); Log.i("wera", "===> Audio info: title: " + title);

                    
                    info.put("width", 0);
                    info.put("height", 0);
                    info.put("size", size);
                    info.put("duration", duration);
                    info.put("mime_type", mimeType);
                    info.put("resolution", 0);
                    info.put("date_taken", 0);
                    info.put("display_name", displayName);
                    info.put("title", title);
                    info.put("path", "");

                    this.sendAudioInfoSuccess(itemId, info);

                }
            } catch(Exception e) {
                this.sendErrorToJS("forming Audio info object has a dramatic outcome: ", itemId, e);
            } finally {
                c.close();
            }      

        }

    }


    //    public void getPreview(String uri, int itemId, CallbackContext callbackContext) {
    //
    //        String sourcePath = uri;
    //
    //        Bitmap bitmap = null;
    //        try {
    //            bitmap = getScaledAndRotatedBitmap(sourcePath);
    //        } catch (IOException e) {
    //            this.sendErrorToJS("getScaledAndRotatedBitmap has a dramatic outcome: ", itemId, e);
    //            e.printStackTrace();
    //        }
    //
    //        if (bitmap == null) {
    //            LOG.d(LOG_TAG, "I either have a null image path or bitmap");
    //            this.sendErrorToJS("Unable to create bitmap!" , itemId);
    //            return;
    //        }
    //
    //
    //        ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();
    //        CompressFormat compressFormat = CompressFormat.JPEG;
    //
    //        try {
    //            if (bitmap.compress(compressFormat, mQuality, jpeg_data)) {
    //                byte[] code = jpeg_data.toByteArray();
    //                byte[] output = Base64.encode(code, Base64.NO_WRAP);
    //                String js_out = new String(output);
    //
    //                this.sendScaleAndPreviewSuccess(itemId, Uri.parse(sourcePath), js_out);
    //
    ////                this.theCallbackContext.success(js_out);
    //
    //
    //                js_out = null;
    //                output = null;
    //                code = null;
    //            }
    //        } catch (Exception e) {
    //            this.sendErrorToJS("Error compressing image: ", itemId);
    //        }
    //        jpeg_data = null;
    //
    //    }

    public void scaleAndGetPreview(String url, int itemId, CallbackContext callbackContext) {
        scaleAndGetPreview(url, itemId, 2000, 300, 80, callbackContext);
    }

    public void scaleAndGetPreview(String url, int itemId, int maxSideSize, CallbackContext callbackContext) {
        scaleAndGetPreview(url, itemId, maxSideSize, 300, 80, callbackContext);
    }

    public void scaleAndGetPreview(String url, int itemId, int maxSideSize, int maxPreviewSideSize, int jpegQuality, CallbackContext callbackContext) {

        //// SCALED VERSION

        String sourcePath = url;
        Bitmap bitmap = null;
        int bitmapWidth = 0;
        int bitmapHeight = 0;
        String bigImageUri = null;


        // Uri uri = Uri.fromFile(createCaptureFile(this.encodingType, System.currentTimeMillis() + ""));

        String fileName = String.valueOf(itemId) + System.currentTimeMillis() + JPEG_EXTENSION;
        Uri uri = Uri.fromFile(new File(getTempDirectoryPath(), fileName));
        try {
            BitmapAndInfo bni = getScaledAndRotatedBitmapAndInfo(sourcePath, maxSideSize);
            bitmap = bni.bitmap;
            bitmapWidth = bni.width;
            bitmapHeight = bni.height;
        }
        catch (IOException e) {
            this.sendErrorToJS("getScaledAndRotatedBitmap for big bitmap has a dramatic outcome: ", itemId, e);
            e.printStackTrace();
        }

        // Double-check the bitmap.
        if (bitmap == null) {
            LOG.d(LOG_TAG, "I either have a null image path or bitmap");
            this.sendErrorToJS("Unable to create big bitmap!" , itemId);
            return;
        }

        // Add compressed version of captured image to returned media store Uri
        OutputStream os;
        try {
            os = this.cordova.getActivity().getContentResolver().openOutputStream(uri);
            bitmap.compress(CompressFormat.JPEG, jpegQuality, os);
            os.close();
        } catch (IOException e) {
            this.sendErrorToJS("Unable write big bitmap to file!" , itemId, e);
        }

        // Restore exif data to file
        //        if (this.encodingType == JPEG) {
        //            String exifPath;
        //            exifPath = uri.getPath();
        //            //We just finished rotating it by an arbitrary orientation, just make sure it's normal
        //            if(this.rotate != ExifInterface.ORIENTATION_NORMAL)
        //                exif.resetOrientation();
        //                exif.createOutFile(exifPath);
        //                exif.writeExifData();
        //        }

        // Send Uri back to JavaScript for viewing image
        // this.callbackContext.success(uri.toString());

        bigImageUri = uri.toString();

        //// PREVIEW

        sourcePath = url;

        Bitmap previewBitmap = null;
        try {
            previewBitmap = getScaledAndRotatedBitmapAndInfo(sourcePath, maxPreviewSideSize).bitmap;
        } catch (IOException e) {
            this.sendErrorToJS("getScaledAndRotatedBitmap for a preview has a dramatic outcome: ", itemId, e);
            e.printStackTrace();
        }

        if (previewBitmap == null) {
            LOG.d(LOG_TAG, "I either have a null image path or bitmap");
            this.sendErrorToJS("Unable to create preview bitmap!" , itemId);
            return;
        }


        ByteArrayOutputStream jpeg_data = new ByteArrayOutputStream();

        try {
            if (previewBitmap.compress(CompressFormat.JPEG, jpegQuality, jpeg_data)) {
                byte[] code = jpeg_data.toByteArray();
                byte[] output = Base64.encode(code, Base64.NO_WRAP);
                String js_out = new String(output);

                this.sendScaleAndPreviewSuccess(itemId, Uri.parse(bigImageUri), bitmapWidth, bitmapHeight, js_out);

                // this.theCallbackContext.success(js_out);


                js_out = null;
                output = null;
                code = null;
            }
        } catch (Exception e) {
            this.sendErrorToJS("Error compressing image: ", itemId);
        }
        jpeg_data = null;
        bitmap = null;
        previewBitmap = null;
    }


    private BitmapAndInfo getScaledAndRotatedBitmapAndInfo(String imageUrl, int maxSideSize) throws IOException {

        int targetImageWidth = maxSideSize;
        int targetImageHeight = maxSideSize;

        BitmapAndInfo toReturn = new BitmapAndInfo();

        // If no new width or height were specified, and orientation is not needed return the original bitmap
        if (targetImageWidth <= 0 && targetImageHeight <= 0 && !(this.correctOrientation)) {

            BitmapFactory.Options bmoptions = new BitmapFactory.Options();
            bmoptions.inJustDecodeBounds = true;

            InputStream fileStream = null;
            Bitmap image = null;
            try {
                fileStream = FileHelper.getInputStreamFromUriString(imageUrl, cordova);
                image = BitmapFactory.decodeStream(fileStream, null, bmoptions);
            }  catch (OutOfMemoryError e) {
                theCallbackContext.error(e.getLocalizedMessage());
            } catch (Exception e){
                theCallbackContext.error(e.getLocalizedMessage());
            }
            finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException e) {
                        LOG.d(LOG_TAG, "Exception while closing file input stream.");
                    }
                }
            }
            toReturn.bitmap = image;
            toReturn.width = bmoptions.outWidth;
            toReturn.height = bmoptions.outHeight;
            return toReturn;
        }


        File localFile = null;
        Uri galleryUri = null;
        //  int rotate = 0;
        try {
            InputStream fileStream = FileHelper.getInputStreamFromUriString(imageUrl, cordova);
            if (fileStream != null) {
                // Generate a temporary file
                String timeStamp = new SimpleDateFormat(TIME_FORMAT).format(new Date());
                String fileName = "IMG_" + timeStamp + (this.encodingType == JPEG ? JPEG_EXTENSION : PNG_EXTENSION);
                localFile = new File(getTempDirectoryPath() + fileName);
                galleryUri = Uri.fromFile(localFile);
                writeUncompressedImage(fileStream, galleryUri);
                try {
                    String mimeType = FileHelper.getMimeType(imageUrl.toString(), cordova);
                    if (JPEG_MIME_TYPE.equalsIgnoreCase(mimeType)) {
                        //  ExifInterface doesn't like the file:// prefix
                        String filePath = galleryUri.toString().replace("file://", "");
                        // read exifData of source
                        exifData = new ExifHelper();
                        exifData.createInFile(filePath);
                        exifData.readExifData();
                        // Use ExifInterface to pull rotation information
                        if (this.correctOrientation) {
                            ExifInterface exif = new ExifInterface(filePath);
                            this.rotate = exifToDegrees(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED));
                        }
                    }
                } catch (Exception oe) {
                    LOG.w(LOG_TAG,"Unable to read Exif data: "+ oe.toString());
                    this.rotate = 0;
                }
            }
        }
        catch (Exception e)
        {
            LOG.e(LOG_TAG,"Exception while getting input stream: "+ e.toString());
            return null;
        }



        try {
            // figure out the original width and height of the image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream fileStream = null;
            try {
                fileStream = FileHelper.getInputStreamFromUriString(galleryUri.toString(), cordova);
                BitmapFactory.decodeStream(fileStream, null, options);
            } finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException e) {
                        LOG.d(LOG_TAG, "Exception while closing file input stream.");
                    }
                }
            }

            if (options.outWidth == 0 || options.outHeight == 0) {
                return null;
            }

            // User didn't specify output dimensions, but they need orientation
            if (targetImageWidth <= 0 && targetImageHeight <= 0) {
                targetImageWidth = options.outWidth;
                targetImageHeight = options.outHeight;
            }

            // Setup target width/height based on orientation
            int rotatedWidth, rotatedHeight;
            boolean rotated= false;
            if (this.rotate == 90 || this.rotate == 270) {
                rotatedWidth = options.outHeight;
                rotatedHeight = options.outWidth;
                rotated = true;
            } else {
                rotatedWidth = options.outWidth;
                rotatedHeight = options.outHeight;
            }

            // determine the correct aspect ratio
            int[] widthHeight = calculateAspectRatio(rotatedWidth, rotatedHeight, targetImageWidth, targetImageHeight);

            // Load in the smallest bitmap possible that is closest to the size we want
            options.inJustDecodeBounds = false;
            options.inSampleSize = calculateSampleSize(rotatedWidth, rotatedHeight,  widthHeight[0], widthHeight[1]);
            Bitmap unscaledBitmap = null;
            try {
                fileStream = FileHelper.getInputStreamFromUriString(galleryUri.toString(), cordova);
                unscaledBitmap = BitmapFactory.decodeStream(fileStream, null, options);
            } finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException e) {
                        LOG.d(LOG_TAG, "Exception while closing file input stream.");
                    }
                }
            }
            if (unscaledBitmap == null) {
                return null;
            }

            int scaledWidth = (!rotated) ? widthHeight[0] : widthHeight[1];
            int scaledHeight = (!rotated) ? widthHeight[1] : widthHeight[0];

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(unscaledBitmap, scaledWidth, scaledHeight, true);
            if (scaledBitmap != unscaledBitmap) {
                unscaledBitmap.recycle();
                unscaledBitmap = null;
            }
            if (this.correctOrientation && (rotate != 0)) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotate);
                try {
                    scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
                    this.orientationCorrected = true;
                } catch (OutOfMemoryError oom) {
                    this.orientationCorrected = false;
                }
            }
            toReturn.bitmap = scaledBitmap;
            toReturn.width = scaledBitmap.getWidth();
            toReturn.height = scaledBitmap.getHeight();
            return toReturn;
        }
        finally {
            // delete the temporary copy
            if (localFile != null) {
                localFile.delete();
            }
        }

    }


    private Bitmap getScaledAndRotatedBitmap(String imageUrl) throws IOException {
        // If no new width or height were specified, and orientation is not needed return the original bitmap
        if (this.targetWidth <= 0 && this.targetHeight <= 0 && !(this.correctOrientation)) {
            InputStream fileStream = null;
            Bitmap image = null;
            try {
                fileStream = FileHelper.getInputStreamFromUriString(imageUrl, cordova);
                image = BitmapFactory.decodeStream(fileStream);
            }  catch (OutOfMemoryError e) {
                theCallbackContext.error(e.getLocalizedMessage());
            } catch (Exception e){
                theCallbackContext.error(e.getLocalizedMessage());
            }
            finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException e) {
                        LOG.d(LOG_TAG, "Exception while closing file input stream.");
                    }
                }
            }
            return image;
        }

        File localFile = null;
        Uri galleryUri = null;
        //   int rotate = 0;
        try {
            InputStream fileStream = FileHelper.getInputStreamFromUriString(imageUrl, cordova);
            if (fileStream != null) {
                // Generate a temporary file
                String timeStamp = new SimpleDateFormat(TIME_FORMAT).format(new Date());
                String fileName = "IMG_" + timeStamp + (this.encodingType == JPEG ? JPEG_EXTENSION : PNG_EXTENSION);
                localFile = new File(getTempDirectoryPath() + fileName);
                galleryUri = Uri.fromFile(localFile);
                writeUncompressedImage(fileStream, galleryUri);
                try {
                    String mimeType = FileHelper.getMimeType(imageUrl.toString(), cordova);
                    if (JPEG_MIME_TYPE.equalsIgnoreCase(mimeType)) {
                        //  ExifInterface doesn't like the file:// prefix
                        String filePath = galleryUri.toString().replace("file://", "");
                        // read exifData of source
                        exifData = new ExifHelper();
                        exifData.createInFile(filePath);
                        exifData.readExifData();
                        // Use ExifInterface to pull rotation information
                        if (this.correctOrientation) {
                            ExifInterface exif = new ExifInterface(filePath);
                            this.rotate = exifToDegrees(exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED));
                        }
                    }
                } catch (Exception oe) {
                    LOG.w(LOG_TAG,"Unable to read Exif data: "+ oe.toString());
                    this.rotate = 0;
                }
            }
        }
        catch (Exception e)
        {
            LOG.e(LOG_TAG,"Exception while getting input stream: "+ e.toString());
            return null;
        }



        try {
            // figure out the original width and height of the image
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            InputStream fileStream = null;
            try {
                fileStream = FileHelper.getInputStreamFromUriString(galleryUri.toString(), cordova);
                BitmapFactory.decodeStream(fileStream, null, options);
            } finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException e) {
                        LOG.d(LOG_TAG, "Exception while closing file input stream.");
                    }
                }
            }


            //CB-2292: WTF? Why is the width null?
            if (options.outWidth == 0 || options.outHeight == 0) {
                return null;
            }

            // User didn't specify output dimensions, but they need orientation
            if (this.targetWidth <= 0 && this.targetHeight <= 0) {
                this.targetWidth = options.outWidth;
                this.targetHeight = options.outHeight;
            }

            // Setup target width/height based on orientation
            int rotatedWidth, rotatedHeight;
            boolean rotated= false;
            if (this.rotate == 90 || this.rotate == 270) {
                rotatedWidth = options.outHeight;
                rotatedHeight = options.outWidth;
                rotated = true;
            } else {
                rotatedWidth = options.outWidth;
                rotatedHeight = options.outHeight;
            }

            // determine the correct aspect ratio
            int[] widthHeight = calculateAspectRatio(rotatedWidth, rotatedHeight);


            // Load in the smallest bitmap possible that is closest to the size we want
            options.inJustDecodeBounds = false;
            options.inSampleSize = calculateSampleSize(rotatedWidth, rotatedHeight,  widthHeight[0], widthHeight[1]);
            Bitmap unscaledBitmap = null;
            try {
                fileStream = FileHelper.getInputStreamFromUriString(galleryUri.toString(), cordova);
                unscaledBitmap = BitmapFactory.decodeStream(fileStream, null, options);
            } finally {
                if (fileStream != null) {
                    try {
                        fileStream.close();
                    } catch (IOException e) {
                        LOG.d(LOG_TAG, "Exception while closing file input stream.");
                    }
                }
            }
            if (unscaledBitmap == null) {
                return null;
            }

            int scaledWidth = (!rotated) ? widthHeight[0] : widthHeight[1];
            int scaledHeight = (!rotated) ? widthHeight[1] : widthHeight[0];

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(unscaledBitmap, scaledWidth, scaledHeight, true);
            if (scaledBitmap != unscaledBitmap) {
                unscaledBitmap.recycle();
                unscaledBitmap = null;
            }
            if (this.correctOrientation && (rotate != 0)) {
                Matrix matrix = new Matrix();
                matrix.setRotate(rotate);
                try {
                    scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
                    this.orientationCorrected = true;
                } catch (OutOfMemoryError oom) {
                    this.orientationCorrected = false;
                }
            }
            return scaledBitmap;
        }
        finally {
            // delete the temporary copy
            if (localFile != null) {
                localFile.delete();
            }
        }

    }

    /* ====================================================================
        FUNCTIONS FROM CAMERA   >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
     ==================================================================== */
    private int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        } else {
            return 0;
        }
    }

    public int[] calculateAspectRatio(int origWidth, int origHeight) {
        return calculateAspectRatio(origWidth, origHeight, this.targetWidth, this.targetHeight);
    }
    
    public int[] calculateAspectRatio(int origWidth, int origHeight, int targetImageWidth, int targetImageHeight) {
        int newWidth = targetImageWidth;
        int newHeight = targetImageHeight;

        // If no new width or height were specified return the original bitmap
        if (newWidth <= 0 && newHeight <= 0) {
            newWidth = origWidth;
            newHeight = origHeight;
        }
        // Only the width was specified
        else if (newWidth > 0 && newHeight <= 0) {
            newHeight = (int)((double)(newWidth / (double)origWidth) * origHeight);
        }
        // only the height was specified
        else if (newWidth <= 0 && newHeight > 0) {
            newWidth = (int)((double)(newHeight / (double)origHeight) * origWidth);
        }
        else {
            double newRatio = newWidth / (double) newHeight;
            double origRatio = origWidth / (double) origHeight;

            if (origRatio > newRatio) {
                newHeight = (newWidth * origHeight) / origWidth;
            } else if (origRatio < newRatio) {
                newWidth = (newHeight * origWidth) / origHeight;
            }
        }

        int[] retval = new int[2];
        retval[0] = newWidth;
        retval[1] = newHeight;
        return retval;
    }

    public static int calculateSampleSize(int srcWidth, int srcHeight, int dstWidth, int dstHeight) {
        final float srcAspect = (float) srcWidth / (float) srcHeight;
        final float dstAspect = (float) dstWidth / (float) dstHeight;

        if (srcAspect > dstAspect) {
            return srcWidth / dstWidth;
        } else {
            return srcHeight / dstHeight;
        }
    }

    private String getTempDirectoryPath() {
        File cache = cordova.getActivity().getCacheDir();
        // Create the cache directory if it doesn't exist
        cache.mkdirs();
        return cache.getAbsolutePath();
    }

    private void writeUncompressedImage(InputStream fis, Uri dest) throws FileNotFoundException,
            IOException {
        OutputStream os = null;
        try {
            os = this.cordova.getActivity().getContentResolver().openOutputStream(dest);
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    LOG.d(LOG_TAG, "Exception while closing output stream.");
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    LOG.d(LOG_TAG, "Exception while closing file input stream.");
                }
            }
        }
    }

    /* ====================================================================
        <<<<<<<<<<<<<<<<   FUNCTIONS FROM CAMERA
     ==================================================================== */


    public void uploadMedia_(int itemId, String mediaToUploadUri, CallbackContext callbackContext){
        if(mediaToUploadUri != null && mediaToUploadUri.length() > 0) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    int timeSeconds = 0;

                    try {
                        sendUploadBegin(itemId);
                    } catch (Exception e) {
                        sendErrorToJS("Method getResult throws: ", itemId, e);
                    }

                    while(timeSeconds < 10) {
                        try {
                            Thread.sleep(1000);
                        }
                        catch (Exception e) {
                            sendErrorToJS("Method getResult from while loop throws: ", itemId, e);
                            // e.printStackTrace();
                        }
                        timeSeconds++;
                        sendUploadProgress(itemId, timeSeconds);
                    }

                    try {
                        sendUploadSuccess(itemId,"http://");
                    } catch (Exception e) {
                        sendErrorToJS("Method getResult throws: ", itemId, e);
                    }

                }
            });
        } else {
            callbackContext.error("Expected non-empty string");
        }
    }

    private void sendUploadProgress(int itemId, int progress) {
        final String type = "uploadProgress";
        try {
            JSONObject args = new JSONObject();
//            JSONArray values = new JSONArray();
//            for (int i = 0; i < event.values.length; i++) {
//                values.put(event.values[i]);
//            }
//            eventArg.put("values", values);
            Long ts = System.currentTimeMillis();
            args.put("type", type);
            args.put("itemId", itemId);
            args.put("timeStamp", ts);
            args.put("progress", progress);
            sendToJS(args);
        } catch(Exception e) {
            sendErrorToJS("Method sendUploadProgress throws: ", itemId, e);
            e.printStackTrace();
        }
    }

    private void sendScaleAndPreviewSuccess(int itemId, Uri uri, int imageWidth, int imageHeight, String previewDataUri) {
        final String type = "scaleAndPreviewSuccess";
        try {
            JSONObject args = new JSONObject();
            Long ts = System.currentTimeMillis();
            args.put("type", type);
            args.put("itemId", itemId);
            args.put("timeStamp", ts);
            args.put("scaledImageUri", uri);
            args.put("imageWidth", imageWidth);
            args.put("imageHeight", imageHeight);
//            args.put("thumbnail", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==");
            args.put("thumbnail", "data:image/jpg;base64,"+ previewDataUri );
            sendSuccessToJS(args);
        } catch(Exception e) {
            sendErrorToJS("Method sendUploadProgress throws: ", itemId, e);
            e.printStackTrace();
        }
    }

    private void sendVideoPreviewSuccess(int itemId, String previewDataUri, JSONObject info) {
        final String type = "videoPreviewSuccess";
        try {
            JSONObject args = new JSONObject();
            Long ts = System.currentTimeMillis();
            args.put("type", type);
            args.put("itemId", itemId);
            args.put("timeStamp", ts);
            args.put("info", info);
            args.put("thumbnail", "data:image/jpg;base64,"+ previewDataUri );
            sendSuccessToJS(args);
        } catch(Exception e) {
            sendErrorToJS("Method sendVideoPreviewSuccess throws: ", itemId, e);
            e.printStackTrace();
        }
    }

    private void sendAudioInfoSuccess(int itemId, JSONObject info) {
        final String type = "audioInfoSuccess";
        try {
            JSONObject args = new JSONObject();
            Long ts = System.currentTimeMillis();
            args.put("type", type);
            args.put("itemId", itemId);
            args.put("timeStamp", ts);
            args.put("info", info);
            args.put("thumbnail", "");
            sendSuccessToJS(args);
        } catch(Exception e) {
            sendErrorToJS("Method audioInfoSuccess throws: ", itemId, e);
            e.printStackTrace();
        }
    }

    private void sendUploadBegin(int itemId) {
        final String type = "uploadBegin";
        try {
            JSONObject args = new JSONObject();
            Long ts = System.currentTimeMillis();
            args.put("type", type);
            args.put("itemId", itemId);
            args.put("timeStamp", ts);
            args.put("url", "");
            sendToJS(args);
        } catch(Exception e) {
            sendErrorToJS("Method sendUploadBegin throws: ", itemId, e);
        }
    }

    private void sendUploadSuccess(int itemId, String serverResponse) {
        final String type = "uploadSuccess";
        try {
            JSONObject args = new JSONObject();
            Long ts = System.currentTimeMillis();
            args.put("type", type);
            args.put("itemId", itemId);
            args.put("timeStamp", ts);
            args.put("serverResponse", serverResponse);
            sendSuccessToJS(args);
        } catch(Exception e) {
            sendErrorToJS("Method sendUploadSuccess throws: ", itemId, e);
            e.printStackTrace();
        }
    }

    private void sendUploadFailure(int itemId) {
        final String type = "uploadFailure";
        try {
            JSONObject args = new JSONObject();
            Long ts = System.currentTimeMillis();
            args.put("type", type);
            args.put("itemId", itemId);
            args.put("timeStamp", ts);
            sendErrorToJS(args);
        } catch(Exception e) {
            sendErrorToJS("Method sendUploadFailure itself failure: ", itemId, e);
            e.printStackTrace();
        }
    }

    private void sendToJS(JSONObject args) {
        try {
            // Send the result.
            PluginResult result = new PluginResult( PluginResult.Status.OK, args );
            result.setKeepCallback(true);
            theCallbackContext.sendPluginResult(result);
        } catch (Exception e) {
            sendErrorToJS("Method sendToJS throws: ", 0, e);
        }
    }

    private void sendSuccessToJS(JSONObject args) {
        try {
            PluginResult result = new PluginResult( PluginResult.Status.OK, args );
            if(this.actionsInProcess > 1) {
                result.setKeepCallback(true);
                this.actionsInProcess--;
            }
            theCallbackContext.sendPluginResult(result);
        } catch (Exception e) {
            sendErrorToJS("Method sendToJS throws: ", 0, e);
            // e.printStackTrace();
        }
    }

    private void sendErrorToJS(String note, int itemId, Exception error) {
        final String type = "error";
        try {
            JSONObject args = new JSONObject();
            Long ts = System.currentTimeMillis();
            args.put("type", type);
            args.put("note", note);
            args.put("itemId", itemId);
            args.put("message", error.getMessage());
            args.put("timeStamp", ts);
            PluginResult result = new PluginResult( PluginResult.Status.ERROR, args);
            if(this.actionsInProcess > 1) { 
                result.setKeepCallback(true);
                this.actionsInProcess--;
            }
            theCallbackContext.sendPluginResult(result);
        } catch (Exception e) {
            PluginResult result = new PluginResult(
                    PluginResult.Status.ERROR,
                    "Method sendErrorToJS itself failure on item: "+ String.valueOf(itemId)+ " exception: " + e.getMessage()
            );
            //result.setKeepCallback(true);
            theCallbackContext.sendPluginResult(result);
            e.printStackTrace();
        }
    }

    private void sendErrorToJS(String note, int itemId) {
        final String type = "error";
        try {
            JSONObject args = new JSONObject();
            Long ts = System.currentTimeMillis();
            args.put("type", type);
            args.put("note", note);
            args.put("itemId", itemId);
            args.put("message", "");
            args.put("timeStamp", ts);
            PluginResult result = new PluginResult( PluginResult.Status.ERROR, args);
            //result.setKeepCallback(true);
            theCallbackContext.sendPluginResult(result);
        } catch (Exception e) {
            PluginResult result = new PluginResult(
                    PluginResult.Status.ERROR,
                    "Method sendErrorToJS itself failure on item: "+ String.valueOf(itemId)+ " exception: " + e.getMessage()
            );
            //result.setKeepCallback(true);
            theCallbackContext.sendPluginResult(result);
            e.printStackTrace();
        }
    }

    private void sendErrorToJS(JSONObject args) {
        PluginResult result = new PluginResult( PluginResult.Status.ERROR, args);
        theCallbackContext.sendPluginResult(result);
    }

    //=================================================================
    //=====================  UPLOAD IMAGE =============================
    //=================================================================

    private void uploadMedia(int itemId, String mediaToUploadUri, String mediaType, String queryUrl, String token,
        String chatId, String fromUser, String extraPack) {

        Log.i("wera", "UPLOAD MEDIA itemId: " + itemId);
        Log.i("wera", "UPLOAD MEDIA mediaToUploadUri: " + mediaToUploadUri);
        Log.i("wera", "UPLOAD MEDIA mediaType: " + mediaType);
        Log.i("wera", "UPLOAD MEDIA queryUrl: " + queryUrl);
        Log.i("wera", "UPLOAD MEDIA token: " + token);
        Log.i("wera", "UPLOAD MEDIA chatId: " + chatId);
        Log.i("wera", "UPLOAD MEDIA fromUser: " + fromUser);
        Log.i("wera", "UPLOAD MEDIA extraPack: " + extraPack);

        JSONObject exPack = null;
        try { 
            exPack = new JSONObject(extraPack);
        } catch (JSONException e) {
            sendErrorToJS("Error unpacking extraPack", itemId, e);
        }
        

        CountingRequestBody.Listener progressListener = new CountingRequestBody.Listener() {
            @Override
            public void onRequestProgress(long bytesRead, long contentLength) {
                if (bytesRead >= contentLength) {
                    //                if (txtInfo != null)
                    //                    MainActivity.this.runOnUiThread(new Runnable() {
                    //                        public void run() {
                    ////                                progressBar.setVisibility(View.GONE);
                    //                            return;
                    //                        }
                    //                    });
                } else {
                    if (contentLength > 0) {
                        final int progress = (int) (((double) bytesRead / contentLength) * 100);
                        sendUploadProgress(itemId, progress);
                        //                    if (txtInfo != null)
                        //                        MainActivity.this.runOnUiThread(new Runnable() {
                        //                            public void run() {
                        ////                                    progressBar.setVisibility(View.VISIBLE);
                        ////                                    progressBar.setProgress(progress);
                        //                                txtInfo.setText(String.valueOf(progress));
                        //                                Log.i("wera", "Uploaded " + String.valueOf(progress) + "%");
                        //                                return;
                        //                            }
                        //                        });

                        if(progress >= 100){
                        //                            progressBar.setVisibility(View.GONE);
                        }
                        //                        Log.e("uploadProgress called", progress+" ");
                    }
                }
            }
        };


        OkHttpClient imageUploadClient = new OkHttpClient.Builder()
            // .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT))
            .addNetworkInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request originalRequest = chain.request();

                    if (originalRequest.body() == null) {
                        return chain.proceed(originalRequest);
                    }
                    Request progressRequest = originalRequest.newBuilder()
                            .method(originalRequest.method(),
                                    new CountingRequestBody(originalRequest.body(), progressListener))
                            .build();

                    return chain.proceed(progressRequest);

                }
            })
            .build();





            //    private void uploadMedia(int itemId, String uri, CallbackContext callbackContext) { 
                
            //    private void uploadMedia(int itemId, String mediaToUploadUri, String mediaType, String thumbnail) {

        Uri photoURI = Uri.parse(mediaToUploadUri);
        if(photoURI == null){
            return;
        }

            //        final File imageFile = new File(uriToFilename(photoURI));
            //        Uri tmpUri = Uri.fromFile(imageFile);
            //        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(tmpUri.toString());
            //        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase());
            //        String imageName = imageFile.getName();




            //        Log.i("wera", "Filename: " + imageFile.getName()+" type: "+mime+" uriToFilename: " + uriToFilename(photoURI));

            //        RequestBody requestBody = new MultipartBody.Builder()
            //                .setType(MultipartBody.FORM)
            //                .addFormDataPart("file", imageName,
            //                        RequestBody.create(imageFile, MediaType.parse(mime)))
            //                .build();
            //

        MultipartBody.Builder requestBodyBuilder = new MultipartBody.Builder();
        requestBodyBuilder
            .setType(MultipartBody.FORM)
            // .addFormDataPart("file", imageName, new UriRequestBody(photoURI, MainActivity.this.getContentResolver()));
            .addFormDataPart("type", mediaType)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("from_user", fromUser)
            .addFormDataPart("file", "imageFile", new UriRequestBody(photoURI, this.cordova.getActivity().getContentResolver()));

        try {
            requestBodyBuilder.addFormDataPart("thumbnail", exPack.getString("thumbnail"));
            requestBodyBuilder.addFormDataPart("media_width", exPack.getString("mediaWidth"));
            requestBodyBuilder.addFormDataPart("media_height", exPack.getString("mediaHeight"));
            requestBodyBuilder.addFormDataPart("mime_type", exPack.getString("mimeType"));
            requestBodyBuilder.addFormDataPart("size", exPack.getString("size"));
            requestBodyBuilder.addFormDataPart("date_taken", exPack.getString("dateTaken"));
            requestBodyBuilder.addFormDataPart("duration", exPack.getString("duration"));
            requestBodyBuilder.addFormDataPart("display_name", exPack.getString("displayName"));
            requestBodyBuilder.addFormDataPart("title", exPack.getString("title"));
        } catch (JSONException e) {
            sendErrorToJS("Error unpacking extraPack", itemId, e);
        }


        MultipartBody requestBody = requestBodyBuilder.build();

        Request request = new Request.Builder()
                .url(queryUrl)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .post(requestBody)
                .build();

                //        Call call = okClient.newCall(request);

        sendUploadBegin(itemId);

        imageUploadClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.i("wera", "Request failure: " + e.getMessage());
                sendUploadFailure(itemId);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String r = response.body().string();
                Log.i("wera", "FROM SERVER: " + r);
                sendUploadSuccess(itemId,r);
                //                MainActivity.this.runOnUiThread(new Runnable() {
                //                    final String mess = "Response: " + response.body().string();
                //                    @Override
                //                    public void run() {
                //                        Log.i("wera", mess);
                //                        txtInfo.setText("Completed!");
                //                        return;
                //                    }
                //                });
            }
        });
    }

}