package com.mycorp.constants;

import java.util.regex.Pattern;

/**
 * Clase que contiene la declaración de constantes necesarias para el funcionamiento de la GUI
 * @author cgi
 *
 */
public final class MyCorrpConstants {
	
	/**
	 * Se define un constructor privado al ser una clase de utilidades - Utility classes should not have a public constructor
	 */
  	private MyCorrpConstants(){	
  	}
  	
    public static final String ESCAPED_LINE_SEPARATOR = "\\n";
    public static final String ESCAPE_ER = "\\";
    public static final String HTML_BR = "<br/>";
    public static final Pattern RESTRICTED_PATTERN = Pattern.compile("%2B", Pattern.LITERAL);
    public static final String JSON = "application/json; charset=UTF-8";
}
