package com.rawrross.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rawrross.server.HTTPResponse.HttpStatusCode;
import com.rawrross.server.exception.BadRequestException;

/**
 * A simple HTTP 1.1 server which supports accepting requests and returning a
 * response based on a defined {@link com.rawrross.server.RequestHandler
 * RequestHandler}.
 * 
 * @author Randy Ross
 */
public class HTTPServer {

	private static final Logger logger = LogManager.getLogger("Server");

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	public static int DEFAULT_KEEP_ALIVE_TIMEOUT = 7000;
	public static double CORE_THREAD_RATIO = 2;

	public static final String DATE_TIME_FORMAT = "EEE, dd LLL yyyy HH:mm:ss zzz";
	public static final String TIME_ZONE = "GMT";
	/** Standard HTTP date-time formatter */
	public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)
			.withZone(ZoneId.of(TIME_ZONE));

	/**
	 * Get the current date in standard HTTP date-time format.
	 * 
	 * @return The current date as a String.
	 */
	public static String getDate() {
		return DATE_FORMATTER.format(Instant.now());
	}

	private static String ERROR_PAGE;

	static {
		try {
			ERROR_PAGE = new String(ClassLoader.getSystemResourceAsStream("error.html").readAllBytes(),
					DEFAULT_CHARSET);
		} catch (Exception e) {
			logger.fatal("Unable to read error.html", e);
			System.exit(1);
		}
	}

	/**
	 * Create a standard error page with the given title and subtitle.
	 * 
	 * @param title    Used as the webpage's title and displayed in large text on
	 *                 the webpage.
	 * @param subtitle Displayed in smaller text on the webpage. Pass
	 *                 <code>null</code> to omit the subtitle.
	 * @return An HTML document as a String.
	 */
	public static String errDoc(String title, String subtitle) {
		String page = new String(ERROR_PAGE);
		page = page.replaceAll("%TITLE%", title);
		page = page.replaceAll("%SUBTITLE%", (subtitle != null) ? subtitle : "");
		page = page.replaceAll("%DATE%", HTTPServer.getDate());
		return page;
	}

	private ExecutorService threadPool;
	private ServerSocket server;
	private boolean running;
	private RequestHandler requestHandler;
	private int keepAliveTimeout;

	/**
	 * Start an HTTP server on a new thread, listening on the given port number.
	 * 
	 * @param port The desired port number, or <code>0</code> to use an
	 *             automatically allocated port.
	 * @throws IOException If there is an issue starting the server socket.
	 */
	public HTTPServer(int port) throws IOException {
		if (CORE_THREAD_RATIO <= 0) {
			threadPool = Executors.newCachedThreadPool();
		} else {
			int numThreads = (int) Math.ceil(Runtime.getRuntime().availableProcessors() * CORE_THREAD_RATIO);
			threadPool = Executors.newFixedThreadPool(numThreads);
		}
		// threadPool = Executors.newVirtualThreadPerTaskExecutor();

		server = new ServerSocket(port);
		requestHandler = this::defaultRequestHandler;
		keepAliveTimeout = HTTPServer.DEFAULT_KEEP_ALIVE_TIMEOUT;

		logger.info("Listening on port {}", getPort());

		startServerThread(port);
	}

	public void setRequestHandler(RequestHandler handler) {
		requestHandler = handler;
	}

	/**
	 * The port number this server is listening on.
	 * 
	 * @return The port number of this server.
	 */
	public int getPort() {
		return server.getLocalPort();
	}

	/**
	 * Closes this server to new connections.
	 * <p>
	 * Due to blocking socket reads, each thread may not terminate until its
	 * keep-alive timeout is reached.
	 */
	public void stop() {
		running = false;

		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		threadPool.shutdownNow();
	}

	private void startServerThread(int port) {
		new Thread(() -> {
			running = true;

			while (running) {
				try {
					Socket socket = server.accept();
					threadPool.execute(() -> gotConnection(socket));
				} catch (SocketException e) {
					// Server is closing, ignore SocketException
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * Handle an accepted connection with the given socket. This method with block
	 * until the connection is terminated, and the given socket will be closed after
	 * this method returns.
	 * 
	 * @param socket This socket is closed after this method returns.
	 */
	private void gotConnection(Socket socket) {
		HTTPRequest request = null;
		HTTPResponse response;

		try (socket) {
			socket.setSoTimeout(keepAliveTimeout);

			do {
				request = null;

				try {
					request = new HTTPRequest(socket.getInputStream());
					request.checkParseException();

					response = new HTTPResponse();
					response.addHeader("Connection", "keep-alive");
					response.addHeader("Keep-Alive", "timeout=" + (keepAliveTimeout / 1000));
					requestHandler.handleRequest(request, response);
				} catch (SocketTimeoutException e) {
					// Keep alive timeout reached, so close the idle connection
					return;
				} catch (BadRequestException e) {
					response = new HTTPResponse();
					response.setErrorStatus(HttpStatusCode.BAD_REQUEST, e.getMessage());
				} catch (Exception e) {
					printException(e, request);
					response = new HTTPResponse();
					response.setErrorStatus(HttpStatusCode.INTERNAL_SERVER_ERROR, null);
				}

				response.write(socket.getOutputStream());
			} while (running && request != null && request.isConnectionKeepAlive());
		} catch (IOException e) {
			printException(e, request);
		}
	}

	private void printException(Exception e, HTTPRequest request) {
		if (request != null && !request.getLines().isEmpty()) {
			logger.error("Exception handling request \"{}\"", request.getLines().get(0), e);
		} else {
			logger.error("Exception handling request (null)", e);
		}
	}

	private void defaultRequestHandler(HTTPRequest request, HTTPResponse response) {
		response.setStatusCode(HttpStatusCode.NOT_IMPLEMENTED)
				.setContentType(MimeType.TEXT_HTML.MIME)
				.setBody("<p>Not Implemented</p>");
	}

}

/*
 * Resources
 * 
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Messages
 * https://developer.mozilla.org/en-US/docs/Glossary/HTTP_header
 * https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
 */
