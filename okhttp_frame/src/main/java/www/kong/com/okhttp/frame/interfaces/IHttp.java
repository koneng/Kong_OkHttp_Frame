package www.kong.com.okhttp.frame.interfaces;

import okhttp3.Request;

public interface IHttp<T> {

    Request request();

    void execute(Call<T> call);
}
