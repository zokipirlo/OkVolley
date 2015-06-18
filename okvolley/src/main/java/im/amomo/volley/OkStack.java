package im.amomo.volley;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.Map;

/**
 * Created by GoogolMo on 6/4/14.
 */
public interface OkStack {
    Response performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError;
}
