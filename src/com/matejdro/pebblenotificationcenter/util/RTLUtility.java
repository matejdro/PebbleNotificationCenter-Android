package com.matejdro.pebblenotificationcenter.util;

import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;
import timber.log.Timber;

/**
 * This class takes care of preparing rtl+non-rtl texts to be shown on the pebble
 * 
 * Since pebble OS doesn't support RTL, displaying RTL texts in it is quite problematic, and we are
 * required to use a somewhat hacky solution, involving manual insertion of line breaks and reordering of 
 * RTL portions of the text.
 * This utility will reformat the text in a way that might not look 1-to-1 like it does on the phone, it will be
 * readable on 90% of the cases.
 * 
 * @author arieh
 *
 */
public class RTLUtility {
	private static RTLUtility ref;
	protected Pattern rtl_rx;
	protected String heb_range = "\u05D0-\u05EA";
	protected String arabic_range = "\u0600-\u06FF"; 
	protected String rx_str;
	
	private RTLUtility(){
		rx_str = generateRegExp();
		rtl_rx = Pattern.compile(rx_str);
	}
	
	public static RTLUtility getInstance(){
		if (ref == null){
			ref = new RTLUtility();
		}
		return ref;
	}
	
	public boolean isRTL(String text){
		Matcher m = rtl_rx.matcher(text);
		if (m.find()){
			return true;
		}
		return false;
	}
	
	/**
	 * Will reverse RTL sections of a string, but will not enter any new lines into text
	 * @param text
	 * @return String
	 */
	public String format(String text){
		if (false == isRTL(text)) return text;

        ArrayList<String> fragments = getFragments(text);
		
        return reorgRTLString(fragments, 0, false);
	}
	
	/**
	 * If string contains rtl characters, method will format it in a way
	 * that will be readable, including separating the text into lines. 
	 * @param text text to format
	 * @param max  maximum characters per line 
	 * @return
	 */
	public String format(String text, int max){
		if (false == isRTL(text)) return text;

        Timber.d("Reverting string %s", text);
       
        ArrayList<String> fragments = getFragments(text);
        
        String str = reorgRTLString(fragments, max, true);

        Timber.d("String reverted %s", str);
        return str;
    }
	
	protected String generateRegExp(){
		return "(["+heb_range+arabic_range+"]["+heb_range+arabic_range+"\\W]+)";
	}
	
	protected ArrayList<String> getFragments(String text){
		Matcher matcher = rtl_rx.matcher(text);
        int current = 0;
        int start;
        int end=0;

        ArrayList<String> fragments = new ArrayList<String>();
        
        //Separate text to one-directional fragments.
        while (matcher.find()) {
        	start = matcher.start();
        	end   = matcher.end();
        	if (start > current) {
        		fragments.add(text.substring(current, start));
        	}
        	fragments.add(reverseSubString(text, matcher.start(),matcher.end()));
            current = end;
        }
        
        if (end != text.length()-1) {
        	fragments.add(text.substring(end));
        }
		
        return fragments;
	}
    
    protected String reorgRTLString(ArrayList<String> frags, int max, boolean format_lines){
    	StringBuilder result = new StringBuilder();
    	int first_dir = isRTL(frags.get(0)) ? 0 : 1;
    	String line;
    	
    	for (int i=0; i < frags.size(); i++){
    		if (first_dir % 2 == 0 && format_lines){
        		line = formatLines(frags.get(i), max);    			
    		}else {
    			line = frags.get(i);
    		}
    		first_dir++;
    		result.append(line);    		
    	}
    	return result.toString();
    }

    protected String reverseSubString(String text, int start, int end){
        String str = new StringBuilder(text.substring(start, end)).reverse().toString();
        if (str.charAt(0) == ' ') {
            str = str.substring(1) + " ";
        }
        return str;
    }

    protected String formatLines(String text, int max) {
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
