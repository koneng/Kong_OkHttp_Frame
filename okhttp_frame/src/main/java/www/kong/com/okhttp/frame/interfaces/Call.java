package www.kong.com.okhttp.frame.interfaces;

public interface Call<T> {

    void onSuccess(T t);

    void onError(int code, String error);
}
