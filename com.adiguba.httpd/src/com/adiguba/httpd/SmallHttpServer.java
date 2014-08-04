package com.adiguba.httpd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmallHttpServer {
	
	private static class TaskInfo {
		private final Pattern pattern;
		private final HttpTask task;
		
		public TaskInfo(Pattern pattern, HttpTask task) {
			super();
			this.pattern = pattern;
			this.task = task;
		}
		
		public Pattern getPattern() {
			return pattern;
		}
		
		public HttpTask getTask() {
			return task;
		}
	}

	private final Map<String,HttpTask> namedTasks = new HashMap<>();
	private final List<TaskInfo> patternTasks = new ArrayList<>();
	
	private final String hostname;
	private final int port;

	public SmallHttpServer(int port) {
		this(null, port);
	}

	public SmallHttpServer(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}

	
	public void registerStatic(String path, HttpTask task) {
		this.namedTasks.put(path, task);
	}
	
	public void registerDynamic(String regex, HttpTask task) {
		registerDynamic(Pattern.compile(regex), task);
	}
	
	public void registerDynamic(Pattern pattern, HttpTask task) {
		this.patternTasks.add(new TaskInfo(pattern, task));
	}
	
	
	private void processSocket(Socket socket) throws IOException {
		// socket.setSoTimeout(5000);

		try (InputStream input = socket.getInputStream()) {
			try (OutputStream output = socket.getOutputStream()) {

				HttpRequest request = HttpRequest.buildRequest(input, socket);
				HttpResponse response = HttpResponse.buildResponse(output, request);

				System.out.println("The Client " + request.getRemoteAddress()
						+ " is connected : " + request.getPath());
				
				HttpTask task = findHttpTask(request);
				if (task!=null) {
					task.serve(request, response);
				} else {
					response.sendError(404, null, request.getPath());
				}		
			}
		}
	}

	
	private HttpTask findHttpTask(HttpRequest request) {
		final String path = request.getPath();
		
		HttpTask task = this.namedTasks.get(path);
		if (task!=null) {
			return task;
		}
		
		for (TaskInfo info : this.patternTasks) {
			Matcher matcher = info.getPattern().matcher(path);
			if (matcher.matches()) {
				request.setMatches(matcher.toMatchResult());
				return info.getTask();
			}
		}
		
		// TODO : static files ???
		
		return null;
	}
		
	public void start() throws IOException {
		final InetSocketAddress address;
		if (this.hostname != null) {
			address = new InetSocketAddress(this.hostname, this.port);
		} else {
			address = new InetSocketAddress(this.port);
		}
		try (final ServerSocket serverSocket = new ServerSocket()) {
			serverSocket.bind(address);
			while (true) {
				processSocket(serverSocket.accept());
			}
		}

	}

}
