package com.tdunning.common;

public class Preconditions {

	/**
	 * Ensures the truth of an expression involving one or more parameters to the calling method.
	 * 
	 * @param expression
	 *            a boolean expression
	 * @param errorMessage
	 *            the exception message to use if the check fails; will be converted to a string using
	 *            {@link String#valueOf(Object)}
	 * @throws IllegalArgumentException
	 *             if {@code expression} is false
	 */
	public static void checkArgument(boolean expression, Object errorMessage) {
		if (!expression) {
			throw new IllegalArgumentException(String.valueOf(errorMessage));
		}
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters
	 * to the calling method.
	 * 
	 * @param expression
	 *            a boolean expression
	 * @throws IllegalStateException
	 *             if {@code expression} is false
	 */
	public static void checkState(boolean expression) {
		if (!expression) {
			throw new IllegalStateException();
		}
	}

	/**
	 * Ensures the truth of an expression involving the state of the calling instance, but not involving any parameters
	 * to the calling method.
	 * 
	 * @param expression
	 *            a boolean expression
	 * @param errorMessage
	 *            the exception message to use if the check fails; will be converted to a string using
	 *            {@link String#valueOf(Object)}
	 * @throws IllegalStateException
	 *             if {@code expression} is false
	 */
	public static void checkState(boolean expression, Object errorMessage) {
		if (!expression) {
			throw new IllegalStateException(String.valueOf(errorMessage));
		}
	}
}
