package com.onyx.memoryfilesample;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.onyx.android.sdk.data.request.data.db.BaseDBRequest;
import com.onyx.android.sdk.reader.IMetadataService;
import com.onyx.android.sdk.utils.Benchmark;
import com.onyx.android.sdk.utils.Debug;
import com.onyx.memoryfilesample.request.TestRequest;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.Bind;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String packageName = "com.onyx.android.sdk.readerview";
    public static final String name = "com.onyx.android.sdk.readerview.service.ReaderMetadataService";
    @Bind(R.id.button_get_toc)
    Button buttonGetToc;
    @Bind(R.id.file_name)
    EditText fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_file_demo);

        ButterKnife.bind(this);
        buttonGetToc.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.equals(buttonGetToc)) {
            String file = fileName.getText().toString();
            /*if (!TextUtils.isEmpty(fileName.getText().toString())) {
                ExecutorService s = Executors.newCachedThreadPool();
                for (int m = 0; m < 10000; m++) {
                    final int finalM = m;
                    s.execute(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("zsy","第"+ finalM +"次");
                            TestRequest request = new TestRequest(MainActivity.this,fileName.getText().toString());
                            request.execute();
                        }
                    });
                }
            }*/
            TestRequest request = new TestRequest(MainActivity.this,fileName.getText().toString());
            request.execute();
        }
    }

    public boolean extractMetadataByRemoteService(final File file) {
        final MetadataServiceConnection connection = new MetadataServiceConnection(this,file);
        final Benchmark benchmark = new Benchmark();
        boolean ret = false;
        try {
            final Intent service = new Intent();
            service.setComponent(new ComponentName("com.onyx.kreader", "com.onyx.android.sdk.readerview.service.ReaderMetadataService"));
            bindService(service, connection, Context.BIND_AUTO_CREATE | Context.BIND_NOT_FOREGROUND);
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

    private static class MetadataServiceConnection implements ServiceConnection {
        public static final int WAIT_SLEEP_TIME = 100;
        private volatile boolean connected = false;
        private volatile IBinder remoteService;
        private File file;
        private Context context;
        public MetadataServiceConnection(Context context,File file) {
            super();
            this.file = file;
            this.context =context;
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

                Log.i("zsy", "filesize" + file.length());
                context.unbindService(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public IBinder getRemoteService() {
            return remoteService;
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
}
