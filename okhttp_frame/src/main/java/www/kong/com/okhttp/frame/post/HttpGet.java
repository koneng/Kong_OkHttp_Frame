package www.kong.com.okhttp.frame.post;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import www.kong.com.okhttp.frame.HttpConfig;
import www.kong.com.okhttp.frame.WrapData;
import www.kong.com.okhttp.frame.interfaces.Call;
import www.kong.com.okhttp.frame.interfaces.IBuilder;
import www.kong.com.okhttp.frame.interfaces.IHttp;

public class HttpGet<T> implements IHttp<T> {

    private OkHttpClient mOkHttpClient;
    private Map<String, String> mHeadMap;
    private String mUrl;

    private HttpGet(Builder builder) {
        if (builder.mClient != null) {
            mOkHttpClient = builder.mClient;
        }else {
            mOkHttpClient = HttpConfig.get().getOkHttpClient();
        }
        if (mOkHttpClient == null) {
            throw new NullPointerException("ok http client is null !");
        }
        mHeadMap = builder.mHeadMap;
        mUrl = builder.mUrl;
    }

    public Request request() {
        Request.Builder builder = new Request.Builder();
        if (mHeadMap != null) {
            Headers headers = parseHeaders(mHeadMap);
            builder.headers(headers);
        }
        return builder.url(mUrl).build();
    }

    private void newCall(Request request, final Call<T> dyResponse) {
        mOkHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                //todo
                dyResponse.onError(-1, e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) {
                String json = response.message();
                Type type = new TypeToken<WrapData<T>>(){}.getType();
                WrapData<T> wrapData = new Gson().fromJson(json, type);
                if (response.isSuccessful()) {
                    T t = wrapData.data;
                    dyResponse.onSuccess(t);
                } else {
                    //todo 统一处理异常逻辑
                    dyResponse.onError(wrapData.code, wrapData.message);
                }
            }
        });
    }

    private Headers parseHeaders(Map<String, String> map) {
        Headers.Builder builder = new Headers.Builder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }

    @Override
    public void execute(Call<T> call) {
        Request request = request();
        newCall(request, call);
    }

    public static class Builder implements IBuilder {

        private Map<String, String> mHeadMap;
        private OkHttpClient mClient;
        private String mUrl;

        public Builder okHttpClient(OkHttpClient client) {
            mClient = client;
            return this;
        }

        public Builder addHeaders(Map<String, String> headers) {
            mHeadMap = headers;
            return this;
        }

        public Builder url(String url) {
            mUrl = url;
            return this;
        }

        public HttpGet build() {
            return new HttpGet<>(this);
        }
    }
}
