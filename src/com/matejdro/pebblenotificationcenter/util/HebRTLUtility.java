package com.matejdro.pebblenotificationcenter.util;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class HebRTLUtility {
	/**
	 * If string contains Hebrew characters, method will format it in a way
	 * that will be readable. 
	 * @param text text to format
	 * @param max  maximum characters per line 
	 * @return
	 */
	public static String format(String text, int max){
        Log.d("Reverting string", text);
        
        Pattern heb_re = Pattern.compile("([\u05D0-\u05EA][\u05D0-\u05EA\\W]+)");
        Matcher matcher = heb_re.matcher(text);
        String str = text;
        int current = 0;
        int start;
        int end=0;
        int first_dir = 0;

        ArrayList<String> fragments = new ArrayList<String>();
        
        while (matcher.find()) {
        	start = matcher.start();
        	end   = matcher.end();
        	if (start > current) {
        		fragments.add(text.substring(current, start));
        		if (current == 0) first_dir = 1;
        	}
        	fragments.add(reverseSubString(text, matcher.start(),matcher.end()));
            current = end;
        }

        if (end == 0) return text;
        
        if (end != text.length()-1) {
        	fragments.add(text.substring(end));
        }
        
        str = reorgRTLString(fragments, max, first_dir);

        Log.d("String reverted", str);
        return str;
    }
    
    protected static String reorgRTLString(ArrayList<String> frags, int max, int first_dir){
    	StringBuilder result = new StringBuilder();
    	String line;
    	
    	for (int i=0; i < frags.size(); i++){
    		if (first_dir % 2 == 0){
        		line = formatLines(frags.get(i), max);    			
    		}else {
    			line = frags.get(i);
    		}
    		first_dir++;
    		result.append(line);    		
    	}
    	return result.toString();
    }

    protected static String reverseSubString(String text, int start, int end){
        String str = new StringBuilder(text.substring(start, end)).reverse().toString();
        if (str.charAt(0) == ' ') {
            str = str.substring(1) + " ";
        }
        return str;
    }

    protected static String formatLines(String text, int max) {
        ArrayList<String> lines = new ArrayList<String>();

        if (max >= text.length() -1) return text;
 
        if (text.charAt(text.length()-1)==' '){
        	text = text.substring(0, text.length()-1);
        }
        int end = text.length();
        int start = end - max;
        char current;
        String line;
        
        while (start > -1 && end > 0) {
            current = text.charAt(start++);
        	while (start < end && current!=' '){
                current = text.charAt(start++);
        	}
        	if (start == end) {
        		start = end - max < 0 ? 0 : end - max;
        	}
        	lines.add(text.substring(start, end));
        	end = start;
        	start = end - max < 0 ? 0 : end - max;
        }

        StringBuilder res = new StringBuilder();
        for ( int i = 0; i < lines.size() ; i++){
        	line = lines.get(i);
        	if (line.charAt(line.length()-1)!='\n'){
        		line = line+'\n';
        	}
            res.append(line);
        }
        return res.toString();
    }


}
