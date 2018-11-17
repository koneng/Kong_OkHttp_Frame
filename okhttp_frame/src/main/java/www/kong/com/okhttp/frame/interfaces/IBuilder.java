package www.kong.com.okhttp.frame.interfaces;

import java.util.Map;

import okhttp3.OkHttpClient;

public interface IBuilder {

    IBuilder okHttpClient(OkHttpClient client);

    IBuilder addHeaders(Map<String, String> headers);

    IBuilder url(String url);

    IHttp build();
}
