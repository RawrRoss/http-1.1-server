package com.rawrross.server.exception;

import java.io.IOException;

/**
 * Signals that an HTTP request is malformed and cannot be parsed.
 * 
 * @author Randy Ross
 */
public class BadRequestException extends IOException {

	public BadRequestException() {
		super();
	}

	public BadRequestException(String message) {
		super(message);
	}

}
