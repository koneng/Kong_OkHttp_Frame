package www.kong.com.okhttp.frame;

import okhttp3.OkHttpClient;

public class HttpConfig {

    private static HttpConfig mInstance;
    private OkHttpClient mOkHttpClient;

    private HttpConfig() {
        mOkHttpClient = new OkHttpClient();
    }

    public static HttpConfig get() {
        if (mInstance == null) {
            synchronized (HttpConfig.class) {
                if (mInstance == null) {
                    mInstance = new HttpConfig();
                }
            }
        }
        return mInstance;
    }

    public void initClient(OkHttpClient okHttpClient) {
        mOkHttpClient = okHttpClient;
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }
}
