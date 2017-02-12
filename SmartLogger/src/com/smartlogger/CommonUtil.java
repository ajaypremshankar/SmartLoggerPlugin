package com.smartlogger;

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.text.edits.TextEdit;

public class CommonUtil {

	public static final String LOGGER_TYPE = "Logger";
	public static final String LOGGER_NAME = "logger";
	public static final String SLF4J_LOGGER_IMPORT = "org.slf4j.Logger";
	public static final String LOG4J_LOGGER_IMPORT = "org.apache.log4j.Logger";
	public static final String IMAGE_PATH = "icons/SmartLogger.gif";

	private static final String[] excludedFiles = { "SCLogger.java" };

	public static boolean isNotEmpty(Collection<?> collection) {
		return !isNullOrEmpty(collection);
	}

	public static boolean isNullOrEmpty(Collection<?> collection) {
		return collection == null || collection.isEmpty();
	}

	public static boolean isNotNull(Object obj) {
		return !((obj == null));
	}

	public static TextEdit createEditOrAddChild(TextEdit edits, TextEdit rewrite) {
		if (CommonUtil.isNotNull(edits)) {
			edits.addChild(rewrite);
		} else {
			edits = rewrite;
		}
		return edits;
	}

	/***
	 * Constants/Test files need not to be logged
	 * 
	 * @param unit
	 * @return true - eligible for Logging
	 */
	public static boolean isLoggingRequiredFor(ICompilationUnit unit) {
		String fileName = unit.getElementName();
		return !(isConstantsFile(fileName) || isTestFile(fileName) || isExcludedFile(fileName));
	}

	private static boolean isConstantsFile(String fileName) {
		return fileName.endsWith("Constants.java");
	}

	private static boolean isTestFile(String fileName) {
		return fileName.startsWith("Test");
	}

	private static boolean isExcludedFile(String fileName) {
		return Arrays.asList(excludedFiles).contains(fileName);
	}
}
