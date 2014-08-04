package com.adiguba.httpd;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class HttpResponse {

	private int status = 200;
	private String statusMessage = "OK";
	
	private OutputStream output;
	private final boolean autoGzip;
	private final MultiMap headers = new MultiMap();
	
	public HttpResponse(OutputStream output, boolean autoGzip) {
		this.output = output;
		this.autoGzip = autoGzip;
		this.headers.setValue("Server", "SHS");
		this.headers.setValue("Connection", "close");
	}
	
	
	
	static HttpResponse buildResponse(OutputStream output, HttpRequest request) {
		return new HttpResponse(output, request.acceptEncoding("gzip"));
	}
	
	public void setStatus(int status) {
		setStatus(status, null);
	}
	
	public void setStatus(int status, String statusMessage) {
		this.status = status;
		this.statusMessage = statusMessage != null ? statusMessage : getDefaultStatusFor(status);
	}
	
	private static String getDefaultStatusFor(int statusCode) {
		switch(statusCode) {
		case 200: return "OK";
		case 202: return "Accepted";
		case 204: return "No Content";
		case 206: return "Partial Content";
		case 300: return "Multiple Choices";
		case 301: return "Moved Permanently";
		case 302: return "Found";
		case 303: return "See Other";
		case 304: return "Not Modified";
		case 400: return "Bad Request";
		case 401: return "Unauthorized";
		case 403: return "Forbidden";
		case 404: return "Not found";
		case 405: return "Method Not Allowed";
		case 500: return "Internal Server Error";
		case 501: return "Not Implemented";
		case 502: return "Bad Gateway";
		}
		return "Status " + statusCode;
	}
	
	public PrintWriter getWriter(Charset charset, String contentType) throws IOException {
		this.headers.setValue("Content-Type", contentType + "; charset=" + charset.name());
		return new PrintWriter(new OutputStreamWriter(getOutputStream(), charset));
	}
	
	public OutputStream getOutputStream() throws IOException {
		if (output==null) {
			throw new IOException("headers already sent");
		}
		final Charset ascii = StandardCharsets.US_ASCII;
		final byte[] separ = ": ".getBytes(ascii);
		final byte[] comma = ", ".getBytes(ascii);
		final byte[] endl = "\n".getBytes(ascii);
		
		final OutputStream out = this.output;
		this.output = null;
		
		final boolean gzip = this.autoGzip && this.headers.getValue("Content-Encoding")==null;
		if (gzip) {
			this.headers.setValue("Content-Encoding", "gzip");
		}
		out.write("HTTP/1.1 ".getBytes(ascii));
		out.write((this.status + " " + this.statusMessage).getBytes(ascii));
		out.write(endl);
		
		for (Map.Entry<String, String[]> header : this.headers.values()) {
			out.write(header.getKey().getBytes(ascii));
			out.write(separ);
			boolean first = true;
			
			for (String value : header.getValue()) {
				if (first) {
					first = false;
				} else {
					out.write(comma);
				}
				out.write(value.getBytes(ascii));
			}
			out.write(endl);
		}
		out.write(endl);
		out.flush();
		
		if (gzip) {
			return new GZIPOutputStream(out);
		}
		return out;
	}
	
	public void sendError(int status, String statusMessage, String detail) throws IOException {
		setStatus(status, statusMessage);
		try (PrintWriter w = getWriter(StandardCharsets.UTF_8, "text/plain")) {
			w.write(this.status + " " + this.statusMessage);
			if (detail!=null) {
				w.write(" : ");
				w.write(detail);
			}
		}
	}
}
