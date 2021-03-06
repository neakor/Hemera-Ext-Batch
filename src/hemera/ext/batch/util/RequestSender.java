package hemera.ext.batch.util;

import hemera.core.structure.enumn.EHttpMethod;
import hemera.core.utility.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <code>RequestSender</code> defines the singleton
 * testing utility that provides the functionality to
 * send various types of HTTP requests.
 *
 * @author Yi Wang (Neakor)
 * @version 1.0.2
 */
public class RequestSender {
	/**
	 * The <code>String</code> API base URL.
	 */
	private String baseURL;

	/**
	 * Constructor of <code>RequestSender</code>.
	 * @param baseURL The <code>String</code> base API
	 * URL.
	 */
	public RequestSender(final String baseURL) {
		this.baseURL = this.getLettersAndDigitsPortion(baseURL.trim());
	}

	/**
	 * Send the given request and wait for the response.
	 * @param request The <code>Request</code> to send.
	 * @return The <code>JSONObject</code> response.
	 * @throws JSONException If parsing response failed.
	 * @throws IOException If sending request failed.
	 */
	public JSONObject sendRequest(final Request request) throws IOException, JSONException {
		if (request.method == EHttpMethod.Post || request.method == EHttpMethod.Put || request.method == EHttpMethod.Connect) {
			// There's a bug in Apache that DELETE does not support output.
			return this.sendOutputRequest(request.uri, request.method, request.args);
		} else {
			return this.sendURIRequest(request.uri, request.method, request.args);
		}
	}

	/**
	 * Send a URI request to the given URI with given
	 * arguments appended to the URI itself.
	 * @param uri The <code>String</code> URI to send to.
	 * @param args The <code>Map</code> of all request
	 * <code>String</code> arguments. <code>null</code>
	 * if there are no arguments.
	 * @return The <code>JSONObject</code> response.
	 * @throws JSONException If parsing response failed.
	 * @throws IOException If sending request failed.
	 */
	private JSONObject sendURIRequest(final String uri, final EHttpMethod method, final Map<String, String> args) throws IOException, JSONException {
		final URL url = this.buildURL(uri, args);
		final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod(method.value);
		return this.getResponse(connection);
	}

	/**
	 * Create a valid get URL based on given URI and
	 * arguments.
	 * @param uri The <code>String</code> URI.
	 * @param args The <code>Map</code> of all request
	 * <code>String</code> arguments. <code>null</code>
	 * if there are no arguments.
	 * @return The <code>URL</code> instance.
	 * @throws MalformedURLException If creating URL failed.
	 * @throws UnsupportedEncodingException If UTF-8
	 * encoding is not supported.
	 */
	private URL buildURL(final String uri, final Map<String, String> args) throws MalformedURLException, UnsupportedEncodingException {
		final StringBuilder builder = new StringBuilder();
		builder.append(this.baseURL).append(this.getValidURI(uri));
		if (args == null || args.isEmpty()) return new URL(builder.toString());
		builder.append("?");
		final int last = args.size() - 1;
		final Iterable<String> keys = args.keySet();
		int index = 0;
		for (final String key : keys) {
			final String value = args.get(key);
			final String encodedKey = URLEncoder.encode(key, "UTF-8");
			final String encodedValue = URLEncoder.encode(value, "UTF-8");
			builder.append(encodedKey).append("=").append(encodedValue);
			if (index != last) builder.append("&");
			index++;
		}
		return new URL(builder.toString());
	}

	/**
	 * Send a output request to the given URI with given
	 * arguments in the request body.
	 * @param uri The <code>String</code> URI to send to.
	 * @param args The <code>Map</code> of all request
	 * <code>String</code> arguments. <code>null</code>
	 * if there are no arguments.
	 * @return The <code>JSONObject</code> response.
	 * @throws JSONException If parsing response failed.
	 * @throws IOException If sending request failed.
	 */
	private JSONObject sendOutputRequest(final String uri, final EHttpMethod method, final Map<String, String> args) throws IOException, JSONException {
		// Create a new connection with output.
		final String endPoint = this.baseURL + this.getValidURI(uri);
		final URL url = new URL(endPoint);
		final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod(method.value);
		connection.setDoOutput(true);
		// Write arguments data.
		final String data = this.buildOutput(args);
		final OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
		writer.write(data);
		writer.flush();
		writer.close();
		return this.getResponse(connection);
	}

	/**
	 * Create the output data based on given arguments
	 * for post requests.
	 * @param args The <code>Map</code> of all request
	 * <code>String</code> arguments. <code>null</code>
	 * if there are no arguments. 
	 * @return The <code>String</code> output data.
	 * @throws UnsupportedEncodingException If UTF-8
	 * encoding is not supported.
	 */
	private String buildOutput(final Map<String, String> args) throws UnsupportedEncodingException {
		final StringBuilder builder = new StringBuilder();
		if (args == null || args.isEmpty()) return builder.toString();
		final int last = args.size() - 1;
		final Iterable<String> keys = args.keySet();
		int index = 0;
		for (final String key : keys) {
			final String value = args.get(key);
			final String encodedKey = URLEncoder.encode(key, "UTF-8");
			final String encodedValue = URLEncoder.encode(value, "UTF-8");
			builder.append(encodedKey).append("=").append(encodedValue);
			if (index != last) builder.append("&");
			index++;
		}
		return builder.toString();
	}

	/**
	 * Retrieve the value URI based on given value.
	 * @param value The <code>String</code> value to
	 * parse.
	 * @return The valid <code>String</code> URI in
	 * the format of <code>/uri_value</code>.
	 */
	private String getValidURI(final String value) {
		if (value.trim().isEmpty()) return value.trim();
		final String portion = this.getLettersAndDigitsPortion(value.trim());
		// Compose URI.
		final StringBuilder builder = new StringBuilder();
		return builder.append("/").append(portion).toString();
	}

	/**
	 * Retrieve the letters and digits portion of the
	 * given string value.
	 * @param value The <code>String</code> value.
	 * @return The <code>String</code> portion that
	 * trims the preceding and trailing non-letter or
	 * digit characters.
	 */
	private String getLettersAndDigitsPortion(final String value) {
		// Find the first letter or digit.
		final int length = value.length();
		int beginIndex = -1;
		for (int i = 0; i < length; i++) {
			if (Character.isLetterOrDigit(value.charAt(i))) {
				beginIndex = i;
				break;
			}
		}
		if (beginIndex < 0) throw new IllegalArgumentException("Invalid URI " + value);
		// Find last letter or digit.
		int endIndex = -1;
		for (int i = length-1; i >= 0; i--) {
			if (Character.isLetterOrDigit(value.charAt(i))) {
				endIndex = i;
				break;
			}
		}
		if (endIndex < 0) throw new IllegalArgumentException("Invalid URI " + value);
		return value.substring(beginIndex, endIndex+1);
	}

	/**
	 * Retrieve the response from the given connection.
	 * @param connection The <code>HttpURLConnection</code>
	 * instance.
	 * @return The <code>JSONObject</code> response.
	 * @throws IOException If making connection failed.
	 * @throws JSONException If parsing response failed.
	 */
	private JSONObject getResponse(final HttpURLConnection connection) throws IOException, JSONException {
		String response = null;
		try {
			final InputStream input = connection.getInputStream();
			response = FileUtils.instance.readAsString(input);
		} catch (final IOException e) {
			final InputStream error = connection.getErrorStream();
			response = FileUtils.instance.readAsString(error);
		}
		return new JSONObject(response);
	}
}
