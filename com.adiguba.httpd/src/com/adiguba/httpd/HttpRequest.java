package com.adiguba.httpd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.MatchResult;

public class HttpRequest {
	
	private final InetAddress remoteAddress;
	private final String method;
	private final String path;
	private final String queryString;
	private final String protocol;
	
	private final MultiMap args;
	private final MultiMap headers;
	private MatchResult matches;
	
	
	private HttpRequest(InetAddress remoteAddress, String method, String path, String queryString, String protocol, MultiMap args, MultiMap headers) {
		this.remoteAddress = remoteAddress;
		this.method = method;
		this.path = path;
		this.queryString = queryString;
		this.protocol = protocol;
		this.args = args;
		this.headers = headers;
	}
	
	static HttpRequest buildRequest(InputStream input, Socket socket) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII));
		String line = br.readLine();
		if (line==null) {
			throw new IOException("no protocol header");
		}
		
		final InetAddress remoteAddress = socket.getInetAddress();
		final String method;
		final String path;
		final String queryString;
		final String protocol;
		final MultiMap args = new MultiMap();
		final MultiMap headers = new MultiMap();
		
		String[] r = line.split(" ");
		method = r[0];
		int markIndex = r[1].indexOf('?');
		if (markIndex<0) {
			path = r[1];
			queryString = null;
		} else {
			path = r[1].substring(0, markIndex);
			queryString = r[1].substring(markIndex+1);
		}
		protocol = r[2];
		
		// Lectures des paramètres en GET :
		if (queryString!=null) {
			for (String part : queryString.split("&")) {
				int index = part.indexOf('=');
				if (index<0)
					index = part.length();
				String name = URLDecoder.decode(part.substring(0, index), "utf8");
				String value = URLDecoder.decode(part.substring(index), "utf8");
				args.addValue(name, value);
			}
		}
		
		// Lecture des headers :
		while ( (line=br.readLine()) != null && !line.isEmpty()) {
			int index = line.indexOf(':');
			if (index<0) {
				throw new IOException("bad header format");
			}
			headers.addValue(line.substring(0, index).trim(), line.substring(index+1).trim());
		}
		
		if (!"GET".equals(method)) {
			throw new IllegalStateException("Unsupported Method");
		}
		
		return new HttpRequest(remoteAddress, method, path, queryString, protocol, args, headers);
	}
	
	void setMatches(MatchResult matches) {
		this.matches = matches;
	}
	
	public MatchResult getMatches() {
		return matches;
	}
	
	boolean acceptEncoding(String name) {
		String acceptEncoding = this.getHeader("Accept-Encoding");
		if (acceptEncoding==null) {
			return false;
		}
		for (String value : acceptEncoding.split(",")) {
			if (value.trim().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public InetAddress getRemoteAddress() {
		return remoteAddress;
	}
	
	public String getMethod() {
		return method;
	}

	public String getPath() {
		return path;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getProtocol() {
		return protocol;
	}

	public Iterable<String> getArgNames() {
		return args.keys();
	}

	public Iterable<String> getHeaderNames() {
		return headers.keys();
	}

	public String getHeader(String name) {
		return this.headers.getValue(name);
	}
	
	public String getArg(String name) {
		return this.args.getValue(name);
	}
	
	public String getArg(String name, String defaultValue) {
		String arg = getArg(name);
		if (arg==null) {
			arg = defaultValue;
		}
		return arg;
	}
}
