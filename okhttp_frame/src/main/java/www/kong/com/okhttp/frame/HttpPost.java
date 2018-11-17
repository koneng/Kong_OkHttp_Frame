package www.kong.com.okhttp.frame;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static okhttp3.MultipartBody.FORM;

public class HttpPost<T, K> implements IHttp<T> {

    enum Type {
        DEFAULT, FORM, MULTIPART
    }

    private OkHttpClient mOkHttpClient;
    private Map<String, String> mHeadMap;
    private String mUrl;
    private K mK;
    private Type mType = Type.DEFAULT;
    private Map<String, Object> mParams;
    private Map<String, Object> mPartHeaders;

    private HttpPost(Builder builder) {
        if (builder.mClient != null) {
            mOkHttpClient = builder.mClient;
        } else {
            mOkHttpClient = HttpConfig.get().getOkHttpClient();
        }
        if (mOkHttpClient == null) {
            throw new NullPointerException("ok http client is null !");
        }
        mHeadMap = builder.mHeadMap;
        mUrl = builder.mUrl;
        mK = builder.mK;
    }

    private HttpPost(FromBuilder builder) {
        if (builder.mClient != null) {
            mOkHttpClient = builder.mClient;
        } else {
            mOkHttpClient = HttpConfig.get().getOkHttpClient();
        }
        if (mOkHttpClient == null) {
            throw new NullPointerException("ok http client is null !");
        }
        mHeadMap = builder.mHeadMap;
        mUrl = builder.mUrl;
        mParams = builder.mParams;
    }

    private HttpPost(MultipartBuilder builder) {
        if (builder.mClient != null) {
            mOkHttpClient = builder.mClient;
        } else {
            mOkHttpClient = HttpConfig.get().getOkHttpClient();
        }
        if (mOkHttpClient == null) {
            throw new NullPointerException("ok http client is null !");
        }
        mHeadMap = builder.mHeadMap;
        mUrl = builder.mUrl;
        mParams = builder.mParams;
        mPartHeaders = builder.mPartHeaders;
    }

    public Request request() {
        RequestBody requestBody;
        if (mType == Type.FORM) {
            FormBody.Builder builder = new FormBody.Builder();
            if (mParams != null) {
                for (Map.Entry<String, Object> entry : mParams.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    String str = (String) value;
                    builder.add(key, str);
                }
            }
            requestBody = builder.build();
        } else if (mType == Type.MULTIPART) {
            MultipartBody.Builder builder = new MultipartBody.Builder();
            if (mPartHeaders != null) {
                builder.setType(FORM);
                for (Map.Entry<String, Object> headerEntry : mPartHeaders.entrySet()) {
                    String key = headerEntry.getKey();
                    Object value = headerEntry.getValue();
                    if(value instanceof File) {
                        File file = (File)value;
                        RequestBody body = RequestBody.create(MediaType.parse("file/*"), file);
                        builder.addPart(
                                Headers.of("Content-Disposition", "form-data; name=\"" + key + "\""),
                                body);
                    }else {
                        String str = (String)value;
                        builder.addPart(
                                Headers.of("Content-Disposition", "form-data; name=\"" + key + "\""),
                                RequestBody.create(null, str));
                    }
                }
            }

            if (mParams != null) {
                for (Map.Entry<String, Object> entry : mParams.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value instanceof File) {
                        File file = (File) value;
                        builder.addFormDataPart(key, file.getName(), RequestBody.create(MediaType.parse("file/*"), file));//添加文件
                    } else {
                        String str = (String) value;
                        builder.addFormDataPart(key, str);
                    }
                }
            }
            requestBody = builder.build();
        } else {
            String json = new Gson().toJson(mK);
            requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        }

        Request.Builder builder = new Request.Builder();
        if (mHeadMap != null) {
            Headers headers = parseHeaders(mHeadMap);
            builder.headers(headers);
        }
        return builder.url(mUrl).post(requestBody).build();
    }

    private void newCall(Request request, final Call<T> dyResponse) {
        mOkHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                dyResponse.onError(-1, e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) {
                String json = response.message();
                java.lang.reflect.Type type = new TypeToken<WrapData<T>>(){}.getType();
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

    public class Builder {

        private Map<String, String> mHeadMap;
        private OkHttpClient mClient;
        private String mUrl;
        private K mK;

        public Builder() {
            mType = Type.DEFAULT;
        }

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

        public Builder params(K k) {
            mK = k;
            return this;
        }

        public HttpPost build() {
            return new HttpPost<>(this);
        }
    }


    public class FromBuilder {

        private Map<String, String> mHeadMap;
        private OkHttpClient mClient;
        private String mUrl;
        private Map<String, Object> mParams;

        public FromBuilder() {
            mType = Type.FORM;
        }

        public FromBuilder okHttpClient(OkHttpClient client) {
            mClient = client;
            return this;
        }

        public FromBuilder addHeaders(Map<String, String> headers) {
            mHeadMap = headers;
            return this;
        }

        public FromBuilder url(String url) {
            mUrl = url;
            return this;
        }

        public FromBuilder params(Map<String, Object> params) {
            mParams = params;
            return this;
        }

        public HttpPost build() {
            return new HttpPost<>(this);
        }
    }


    public class MultipartBuilder {

        private Map<String, String> mHeadMap;
        private OkHttpClient mClient;
        private String mUrl;
        private Map<String, Object> mParams;
        private Map<String, Object> mPartHeaders;

        public MultipartBuilder() {
            mType = Type.MULTIPART;
        }

        public MultipartBuilder okHttpClient(OkHttpClient client) {
            mClient = client;
            return this;
        }

        public MultipartBuilder addHeaders(Map<String, String> headers) {
            mHeadMap = headers;
            return this;
        }

        public MultipartBuilder url(String url) {
            mUrl = url;
            return this;
        }

        public MultipartBuilder params(Map<String, Object> params) {
            mParams = params;
            return this;
        }

        public MultipartBuilder postHeaders(Map<String, Object> partHeaders) {
            mPartHeaders = partHeaders;
            return this;
        }

        public HttpPost build() {
            return new HttpPost<>(this);
        }
    }
}
