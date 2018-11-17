package www.kong.com.okhttp.frame.post;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
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
import www.kong.com.okhttp.frame.HttpConfig;
import www.kong.com.okhttp.frame.WrapData;
import www.kong.com.okhttp.frame.interfaces.Call;
import www.kong.com.okhttp.frame.interfaces.IBuilder;
import www.kong.com.okhttp.frame.interfaces.IHttp;
import static okhttp3.MultipartBody.FORM;

public class HttpPost<T> implements IHttp<T> {

    enum BuilderType {
        DEFAULT, FORM, MULTIPART
    }

    private OkHttpClient mOkHttpClient;
    private Map<String, String> mHeadMap;
    private String mUrl;
    private BuilderType mType = BuilderType.DEFAULT;
    private Map<String, Object> mParams;
    private Map<String, Object> mPartHeaders;
    private Object mObjectParams;

    private HttpPost(IBuilder build) {
        if(build instanceof Builder) {
            Builder builder = (Builder) build;
            mType = BuilderType.DEFAULT;
            if (builder.mClient != null) {
                mOkHttpClient = builder.mClient;
            }
            mHeadMap = builder.mHeadMap;
            mUrl = builder.mUrl;
            mObjectParams = builder.mObjectParams;
        } else if (build instanceof FromBuilder) {
            FromBuilder builder = (FromBuilder) build;
            mType = BuilderType.FORM;
            if (builder.mClient != null) {
                mOkHttpClient = builder.mClient;
                mHeadMap = builder.mHeadMap;
                mUrl = builder.mUrl;
                mParams = builder.mParams;
            }
        } else if (build instanceof MultipartBuilder){
            MultipartBuilder builder = (MultipartBuilder) build;
            mType = BuilderType.MULTIPART;
            if (builder.mClient != null) {
                mOkHttpClient = builder.mClient;
                mHeadMap = builder.mHeadMap;
                mUrl = builder.mUrl;
                mParams = builder.mParams;
                mPartHeaders = builder.mPartHeaders;
            }
        }
        if (mOkHttpClient == null) {
            mOkHttpClient = HttpConfig.get().getOkHttpClient();
        }
    }

    public Request request() {
        RequestBody requestBody;
        if (mType == BuilderType.FORM) {
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
        } else if (mType == BuilderType.MULTIPART) {
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
            String json = new Gson().toJson(mObjectParams);
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

    public static class Builder implements IBuilder{

        private Map<String, String> mHeadMap;
        private OkHttpClient mClient;
        private String mUrl;
        private Object mObjectParams;

        public Builder() {
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

        public Builder params(Object objectParams) {
            mObjectParams = objectParams;
            return this;
        }

        public HttpPost build() {
            return new HttpPost<>(this);
        }
    }

    public static class FromBuilder implements  IBuilder{

        private Map<String, String> mHeadMap;
        private OkHttpClient mClient;
        private String mUrl;
        private Map<String, Object> mParams;

        public FromBuilder() {
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


    public static class MultipartBuilder implements IBuilder{

        private Map<String, String> mHeadMap;
        private OkHttpClient mClient;
        private String mUrl;
        private Map<String, Object> mParams;
        private Map<String, Object> mPartHeaders;

        public MultipartBuilder() {
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

        public MultipartBuilder partHeaders(Map<String, Object> partHeaders) {
            mPartHeaders = partHeaders;
            return this;
        }

        public HttpPost build() {
            return new HttpPost<>(this);
        }
    }
}
