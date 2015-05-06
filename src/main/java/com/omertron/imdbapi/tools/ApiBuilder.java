package com.omertron.imdbapi.tools;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.omertron.imdbapi.ImdbException;
import com.omertron.imdbapi.model.AbstractJsonMapping;
import com.omertron.imdbapi.model.ImdbError;
import com.omertron.imdbapi.model.ImdbMovieDetails;
import com.omertron.imdbapi.model.ImdbPerson;
import com.omertron.imdbapi.search.SearchDeserializer;
import com.omertron.imdbapi.search.SearchObject;
import com.omertron.imdbapi.wrapper.ResponseDetail;
import com.omertron.imdbapi.wrapper.WrapperResponse;
import com.omertron.imdbapi.wrapper.WrapperSearch;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yamj.api.common.exception.ApiExceptionType;
import org.yamj.api.common.http.DigestedResponse;
import org.yamj.api.common.http.DigestedResponseReader;
import org.yamj.api.common.http.UserAgentSelector;

public final class ApiBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(ApiBuilder.class);
    private static final int MILLIS_PER_SECOND = 1000;
    private static final String DEFAULT_CHARSET = "UTF-8";
    private static final Charset CHARSET = Charset.forName(DEFAULT_CHARSET);
    private static HttpClient httpClient;
    private static final String BASE_URL = "http://app.imdb.com/";
    private static final String API_VERSION = "v1";
    private static final String APP_ID = "iphone1";
    private static final String SIG = "app1";
    private static Locale imdbLocale = Locale.getDefault();
    /*
     * Jackson JSON configuration
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        SearchDeserializer deserializer = new SearchDeserializer();
        deserializer.registerSearchObject("tconst", ImdbMovieDetails.class);
        deserializer.registerSearchObject("nconst", ImdbPerson.class);

        Version ver = new Version(1, 0, 0, null, "com.omertron.imdbapi.tools", "SearchDeserializer");
        SimpleModule module = new SimpleModule("PolymorphicSearchDeserializerModule", ver);
        module.addDeserializer(SearchObject.class, deserializer);

        MAPPER.registerModule(module);
    }

    private ApiBuilder() {
        throw new UnsupportedOperationException("Class cannot be instantiate");
    }

    public static void setHttpClient(HttpClient httpClient) {
        ApiBuilder.httpClient = httpClient;
    }

    public static void setLocale(Locale locale) {
        ApiBuilder.imdbLocale = locale;
        LOG.trace("Setting locale to {}", imdbLocale.toString());
    }

    public static URL buildUrl(String function, Map<String, String> arguments) {
        StringBuilder sbURL = new StringBuilder(BASE_URL);

        sbURL.append(function);
        sbURL.append("?api=").append(API_VERSION);
        sbURL.append("&appid=").append(APP_ID);
        sbURL.append("&locale=").append(imdbLocale);
        sbURL.append("&timestamp=").append(System.currentTimeMillis() / MILLIS_PER_SECOND);

        for (Map.Entry<String, String> argEntry : arguments.entrySet()) {
            sbURL.append("&").append(argEntry.getKey());
            sbURL.append("=").append(argEntry.getValue());
        }

        sbURL.append("&sig=").append(SIG);

        LOG.trace("URL = {}", sbURL.toString());
        try {
            return new URL(sbURL.toString());
        } catch (MalformedURLException ex) {
            LOG.trace("Failed to convert string to URL: {}", ex.getMessage());
            return null;
        }
    }

    public static <T extends AbstractJsonMapping> T getWrapper(Class<T> clazz, String function, Map<String, String> args) {
        T result;

        // Make sure we have a "blank" object to return
        try {
            result = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            LOG.debug("Failed to instantiate class {}", clazz.getSimpleName(), ex);
            return null;
        }

        try {
            String webPage = requestWebPage(buildUrl(function, args));
            Object response = MAPPER.readValue(webPage, clazz);
            result = clazz.cast(response);
        } catch (JsonParseException ex) {
            LOG.warn("JsonParseException: {}", ex.getMessage(), ex);
            result.setStatusMessage("JsonParseException: " + ex.getMessage(), ex);
        } catch (JsonMappingException ex) {
            LOG.warn("JsonMappingException: {}", ex.getMessage(), ex);
            result.setStatusMessage("JsonMappingException: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            LOG.warn("IOException: {}", ex.getMessage(), ex);
            result.setStatusMessage("IOException: " + ex.getMessage(), ex);
        } catch (ImdbException ex) {
            LOG.warn("ImbdException: {}", ex.getMessage(), ex);
            result.setStatusMessage("ImdbExceptio2n: " + ex.getResponse(), ex);
        }

        return result;
    }
    
    public static String getTheActorIdFromTheWebPage(String function, Map<String, String> args) {
    	String result = null ;

        try 
        {
            String webPage = requestWebPage(buildUrl(function, args));
            result = getActorIdFromString( webPage ) ;

        }
        catch ( Exception e)
        {
        	LOG.error( "Exception in new code", e);
        }

        return result;
    }
    
    private static String getActorIdFromString( String s )
    {
    	String id = null ;
    	int pos, pos2 = 0 ;
    	String SEARCH = "\"nconst\"" ;

    	pos = s.indexOf(SEARCH) ;
    	if ( pos > 0 )
    	{
    		// find the next quote
    		pos = s.indexOf("\"", pos+SEARCH.length() + 1) ;
    	}
    	if ( pos > 0 )
    	{
    		// aha .. pos = first " of "nm000015"
    		pos2 = s.indexOf("\"", pos+1) ;
    	}
    	if ( pos2 > 0 )
    	{
    		// return string without quotes
    		id = s.substring(pos+1, pos2) ;
    	}
    	
    	return id ;
    }


    public static ResponseDetail getResponse(String function, Map<String, String> args) {
        WrapperResponse wr = getWrapper(WrapperResponse.class, function, args);
        return wr.getResult();
    }

    public static ResponseDetail getResponse(String function) {
        return getResponse(function, Collections.<String, String>emptyMap());
    }

    public static WrapperSearch getSearchWrapper(String function, Map<String, String> args) {
        WrapperSearch wrapper = getWrapper(WrapperSearch.class, function, args);

        if (wrapper == null) {
            return null;
        }

        return wrapper.getSearchData();
    }

    /**
     * Download the URL into a String
     *
     * @param url
     * @return
     * @throws AllocineException
     */
    private static String requestWebPage(URL url) throws ImdbException {
        try {
            HttpGet httpGet = new HttpGet(url.toURI());
            httpGet.addHeader("accept", "application/json");
            httpGet.addHeader(HTTP.USER_AGENT, UserAgentSelector.randomUserAgent());

            final DigestedResponse response = DigestedResponseReader.requestContent(httpClient, httpGet, CHARSET);

            if (response.getStatusCode() >= 500) {
                ImdbError error = MAPPER.readValue(response.getContent(), ImdbError.class);
                throw new ImdbException(ApiExceptionType.HTTP_503_ERROR, error.getStatusMessage().getMessage(), url);
            } else if (response.getStatusCode() >= 300) {
                ImdbError error = MAPPER.readValue(response.getContent(), ImdbError.class);
                throw new ImdbException(ApiExceptionType.HTTP_404_ERROR, error.getStatusMessage().getMessage(), url);
            }

            return response.getContent();
        } catch (URISyntaxException ex) {
            throw new ImdbException(ApiExceptionType.INVALID_URL, "Invalid URL", url, ex);
        } catch (IOException ex) {
            throw new ImdbException(ApiExceptionType.CONNECTION_ERROR, "Error retrieving URL", url, ex);
        }
    }
}
