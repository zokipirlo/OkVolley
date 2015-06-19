#OkVolley
A volley library using OkHttp based on [googolmo](https://github.com/googolmo/OkVolley) version.

##Differences
* OkVolley **must** be started with `init` method.
* OkRequest have timeout methods `connectionTimeout, writeTimeout, readTimeout`
* OkHttpStack handles OkRequest timeouts
* OkHttpStack based on [@bryanstern](https://gist.github.com/bryanstern/4e8f1cb5a8e14c202750) version
* OkVolley and OkHttpStack with certificate pinning
* OkVolley default headers (user agent and accept charset) added to every OkRequest

##Usage
It's recommended to initialize OkVolley in your Application class.

	public class BaseApplication extends Application
	{
	    @Override
	    public void onCreate()
	    {
	        super.onCreate();
				
			OkVolley.init(this)
	        	.setUserAgent("My user agent")
	            .trustAllCerts();
	    }
	}

###Request
It's recommended to create your own request classes, which extends OkRequest<T>.

	ProtoRequest request = new ProtoRequest("Url", body, future, future)
	{
		@Override
		public Map<String, String> getHeaders() throws AuthFailureError
		{
			Map<String, String> headers = super.getHeaders(); //this will set User-Agent and Accept-Charset headers
			headers.put("Key", "Value");	// add custom if you need
			return headers;
		}
	};
	request.fast(); //fast, longRead and longWrite are methods to set predefined connection, read and write timeout values. Override in OkRequest if you want.
	request.setTag("requestTag");
	OkVolley.getInstance().getRequestQueue().add(request);

##Include project
First copy `okvolley` folder in your project directory.

Then add in `build.gradle`
```
dependencies {
	...
    compile project(':okvolley')
}
```

and in `settings.gradle`
```
include ':okvolley'
```

##[Pay Attention](https://github.com/googolmo/OkVolley#pay-attention)
