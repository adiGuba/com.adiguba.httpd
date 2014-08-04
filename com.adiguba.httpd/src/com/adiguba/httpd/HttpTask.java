package com.adiguba.httpd;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;


public interface HttpTask {
	
	public static HttpTask from(String data, Charset charset, String contentType) {
		return (request,response) -> {
			try (PrintWriter writer = response.getWriter(charset, contentType)) {
				writer.write(data);
				writer.flush();
			}
		};
	}
	
	
	public void serve(HttpRequest request, HttpResponse response) throws IOException;
}
