package com.matejdro.pebblenotificationcenter.util;


public class TextUtil {	
	public static String prepareString(String text)
	{
		return prepareString(text, 20);
	}
	
	public static String prepareString(String text, int length)
	{
		text = fixInternationalAndTrim(text, length);
		return trimString(text, length, true);
	}

	
	public static String fixInternationalAndTrim(String text, int length)
	{
		StringBuilder builder = new StringBuilder(length);
		
		length = Math.min(length, text.length());
		
		//Thanks to Morbyd for russian table
		for (int i = 0; i < length; i++)
		{
			char ch = text.charAt(i);
			
			if (ch == '\u010C')
				builder.append('C');
			else if (ch == '\u010D')
				builder.append('c');
			
			else if (ch == '\u0401')
				builder.append("Yo");
			else if (ch == '\u0410')
				builder.append('A');
			else if (ch == '\u0411')
				builder.append('B');
			else if (ch == '\u0412')
				builder.append('V');
			else if (ch == '\u0413')
				builder.append('G');
			else if (ch == '\u0414')
				builder.append('D');
			else if (ch == '\u0415')
				builder.append('E');
			else if (ch == '\u0416')
				builder.append("Zh");
			else if (ch == '\u0417')
				builder.append('Z');
			else if (ch == '\u0418')
				builder.append('I');
			else if (ch == '\u0419')
				builder.append('Y');
			else if (ch == '\u041A')
				builder.append('K');
			else if (ch == '\u041B')
				builder.append('L');
			else if (ch == '\u041C')
				builder.append('M');
			else if (ch == '\u041D')
				builder.append('N');
			else if (ch == '\u041E')
				builder.append('O');
			else if (ch == '\u041F')
				builder.append('P');
			else if (ch == '\u0420')
				builder.append('R');
			else if (ch == '\u0421')
				builder.append('S');
			else if (ch == '\u0422')
				builder.append('T');
			else if (ch == '\u0423')
				builder.append('U');
			else if (ch == '\u0424')
				builder.append('F');
			else if (ch == '\u0425')
				builder.append("Kh");
			else if (ch == '\u0426')
				builder.append("Ts");
			else if (ch == '\u0427')
				builder.append("Ch");
			else if (ch == '\u0428')
				builder.append("Sh");
			else if (ch == '\u0429')
				builder.append("Sch");
			else if (ch == '\u042A')
				;
			else if (ch == '\u042B')
				builder.append('Y');
			else if (ch == '\u042C')
				;
			else if (ch == '\u042D')
				builder.append('E');
			else if (ch == '\u042E')
				builder.append("Yu");
			else if (ch == '\u042F')
				builder.append("Ya");
			else if (ch == '\u0430')
				builder.append('a');
			else if (ch == '\u0431')
				builder.append('b');
			else if (ch == '\u0432')
				builder.append('v');
			else if (ch == '\u0433')
				builder.append('g');
			else if (ch == '\u0434')
				builder.append('d');
			else if (ch == '\u0435')
				builder.append('e');
			else if (ch == '\u0436')
				builder.append("zh");
			else if (ch == '\u0437')
				builder.append('z');
			else if (ch == '\u0438')
				builder.append('i');
			else if (ch == '\u0439')
				builder.append('y');
			else if (ch == '\u043A')
				builder.append('k');
			else if (ch == '\u043B')
				builder.append('l');
			else if (ch == '\u043C')
				builder.append('m');
			else if (ch == '\u043D')
				builder.append('n');
			else if (ch == '\u043E')
				builder.append('o');
			else if (ch == '\u043F')
				builder.append('p');
			else if (ch == '\u0440')
				builder.append('r');
			else if (ch == '\u0441')
				builder.append('s');
			else if (ch == '\u0442')
				builder.append('t');
			else if (ch == '\u0443')
				builder.append('u');
			else if (ch == '\u0444')
				builder.append('f');
			else if (ch == '\u0445')
				builder.append("kh");
			else if (ch == '\u0446')
				builder.append("ts");
			else if (ch == '\u0447')
				builder.append("ch");
			else if (ch == '\u0448')
				builder.append("sh");
			else if (ch == '\u0449')
				builder.append("sch");
			else if (ch == '\u044A')
				;
			else if (ch == '\u044B')
				builder.append('y');
			else if (ch == '\u044C')
				;
			else if (ch == '\u044D')
				builder.append('e');
			else if (ch == '\u044E')
				builder.append("yu");
			else if (ch == '\u044F')
				builder.append("ya");
			else if (ch == '\u0451')
				builder.append("yo");

			else
				builder.append(ch);
		}
		
		return builder.toString();		
	}
	
	public static String trimString(String text)
	{
		return trimString(text, 20, true);
	}
	
	public static String trimString(String text, int length, boolean trailingElipsis)
	{
		if (text == null)
			return null;
	
		int targetLength = length;
		if (trailingElipsis)
		{
			targetLength -= 3;
		}
					
		if (text.getBytes().length > length)
		{
			if (text.length() > targetLength)
				text = text.substring(0, targetLength);
			
			while (text.getBytes().length > targetLength)
			{
				text = text.substring(0, text.length() - 1);
			}
			
			if (trailingElipsis)
				text = text + "...";

		}
		
		return text;

	}
}
