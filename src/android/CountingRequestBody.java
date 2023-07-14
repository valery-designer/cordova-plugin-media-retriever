package com.valerydesigner.mediaretriever;

import android.content.ContentResolver;
import android.net.Uri;

import okhttp3.MediaType;
import okhttp3.RequestBody;

import java.io.IOException;

import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;
import java.lang.IllegalStateException;

/**
 * Decorates an OkHttp request body to count the number of bytes written when writing it. Can
 * decorate any request body, but is most useful for tracking the upload progress of large
 * multipart requests.
 *
 * @author Leo Nikkilä
 */
public class CountingRequestBody extends RequestBody
{

    protected RequestBody delegate;
    protected Listener listener;

    protected CountingSink countingSink;

    public CountingRequestBody(RequestBody delegate, Listener listener)
    {
        this.delegate = delegate;
        this.listener = listener;

    }

    @Override
    public MediaType contentType()
    {
        return delegate.contentType();
    }

    @Override
    public long contentLength()
    {
        try
        {
            return delegate.contentLength();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException
    {

        countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);

        delegate.writeTo(bufferedSink);

        bufferedSink.flush();
    }

    protected final class CountingSink extends ForwardingSink
    {

        private long bytesWritten = 0;

        private long bytesWrittenAtPreviousProgressRequest = 0;
        private long bytesInOnePercent = contentLength()/100;

        public CountingSink(Sink delegate)
        {
            super(delegate);
        }

        @Override
        public void write(Buffer source, long byteCount) throws IOException
        {
            super.write(source, byteCount);

            bytesWritten += byteCount;
            if(bytesWritten - bytesWrittenAtPreviousProgressRequest > bytesInOnePercent) {
                bytesWrittenAtPreviousProgressRequest = bytesWritten;
                listener.onRequestProgress(bytesWritten, contentLength());
            }

        }

    }

    public static interface Listener
    {
        public void onRequestProgress(long bytesWritten, long contentLength);
    }

}

