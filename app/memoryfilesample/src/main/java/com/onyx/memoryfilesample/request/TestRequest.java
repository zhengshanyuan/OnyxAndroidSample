package com.onyx.memoryfilesample.request;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.onyx.android.sdk.data.model.Metadata;
import com.onyx.android.sdk.data.model.common.AppPreference;
import com.onyx.android.sdk.data.request.data.db.BaseDBRequest;
import com.onyx.android.sdk.reader.IMetadataService;
import com.onyx.android.sdk.rx.RxCallback;
import com.onyx.android.sdk.utils.Benchmark;
import com.onyx.android.sdk.utils.Debug;
import com.onyx.android.sdk.utils.ViewDocumentUtils;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Created by suicheng on 2018/5/17.
 */
public class TestRequest extends AsyncTask<Object, Object, String> {
    private Context context;
    private String filePath;
    private List<Metadata> metadataList;
    private RxCallback<TestRequest> progressCallback;

    public TestRequest(Context context, @NonNull String filePath) {
        this.context = context;
        this.filePath = filePath;
    }

    private boolean extractMetadataByRemoteService(final String filePath) {
        final File file = new File(filePath);
        final Intent intent = ViewDocumentUtils.intentFromMimeType(file);
        intent.setAction("com.onyx.android.sdk.data.IntentFactory.ACTION_EXTRACT_METADATA");
        return extractMetadataByRemoteService(file);
    }

    private String getAppPreferPackageName(String filename) {
        AppPreference appPrefer = AppPreference.getFileAppPreferMap().get(FilenameUtils.getExtension(filename));
        if (appPrefer == null) {
            return null;
        }
        return appPrefer.packageName;
    }

    public boolean extractMetadataByRemoteService(final File file) {
        final MetadataServiceConnection connection = new MetadataServiceConnection(context, file);
        final Benchmark benchmark = new Benchmark();
        boolean ret = false;
        try {
            final Intent service = new Intent();
            service.setComponent(new ComponentName("com.onyx.kreader", "com.onyx.android.sdk.readerview.service.ReaderMetadataService"));
            context.bindService(service, connection, Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (Debug.getDebug()) {
                if (!ret) {
                    Log.w(getClass().getSimpleName(), "extract file " + file.getAbsolutePath() + " failed");
                }
                benchmark.reportError("ExtractBookService:" + file.getAbsolutePath() + " ts");
            }
        }
        return true;
    }

    @Override
    protected String doInBackground(Object... objects) {
        return extractMetadataByRemoteService(filePath) + "";
    }

    @Override
    protected void onPostExecute(String o) {
        super.onPostExecute(o);
    }

    private static class MetadataServiceConnection implements ServiceConnection {
        public static final int WAIT_SLEEP_TIME = 100;
        private volatile boolean connected = false;
        private volatile IBinder remoteService;
        private File file;
        private Context context;

        public MetadataServiceConnection(Context context, File file) {
            super();
            this.file = file;
            this.context = context;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = true;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                connected = true;
                remoteService = service;
                IMetadataService extractService = IMetadataService.Stub.asInterface(getRemoteService());
                ParcelFileDescriptor pd = null;

                pd = extractService.getTableOfContentByFilename(file.getAbsolutePath());
                FileInputStream fis = new FileInputStream(pd.getFileDescriptor());
                byte[] readbyte = new byte[1024];
                int length;
                StringBuilder stringBuilder = new StringBuilder();
                while ((length = fis.read(readbyte)) != -1) {
                    stringBuilder.append(new String(readbyte, 0, length));
                }

                context.unbindService(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public IBinder getRemoteService() {
            return remoteService;
        }

        public void waitUntilConnected(final BaseDBRequest request) throws InterruptedException {
            while (!connected && !request.isAbort()) {
                Thread.sleep(WAIT_SLEEP_TIME);
            }
        }
    }

    private void onNextCallback() {
        invokeProgressCallback(new Consumer() {
            @Override
            public void accept(Object o) throws Exception {
                RxCallback.onNext(progressCallback, TestRequest.this);
            }
        });
    }

    private void onCompleteCallback() {
        invokeProgressCallback(new Consumer() {
            @Override
            public void accept(Object o) throws Exception {
                RxCallback.onComplete(progressCallback);
            }
        });
    }

    private void invokeProgressCallback(@NonNull Consumer consumer) {
        Observable.just("").subscribeOn(AndroidSchedulers.mainThread()).subscribe(consumer);
    }

    private static String encodeByMD5(String originString) {
        try {
            // 加密对象，指定加密方式
            MessageDigest md5 = MessageDigest.getInstance("md5");
            // 准备要加密的数据
            byte[] b = originString.getBytes();
            // 加密
            byte[] digest = md5.digest(b);
            // 十六进制的字符
            char[] chars = new char[]{'0', '1', '2', '3', '4', '5',
                    '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
            StringBuffer sb = new StringBuffer();
            // 处理成十六进制的字符串(通常)
            for (byte bb : digest) {
                sb.append(chars[(bb >> 4) & 15]);
                sb.append(chars[bb & 15]);
            }
            // 打印加密后的字符串
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
