package de.skuzzle.polly.sdk;

import java.util.Date;

/**
 * Manages default number and date conversion to Strings to provide a consistent view
 * of these types.
 * 
 * @author Simon
 * @since zero day
 * @version RC 1.0
 */
public interface FormatManager {

    /**
     * Formats the given number using pollys default NumberFormat.
     * 
     * @param number The number to format.
     * @return The formatted number as a String. 
     */
    public String formatNumber(double number);
    
    
    
    /**
     * Formats the given date using pollys default DateFormat.
     * 
     * @param date The date to format.
     * @return The formatted date as a String.
     */
    public String formatDate(Date date);
    
    
    
    /**
     * Formats the given date using pollys default DateFormat.
     * 
     * @param date The date to format.
     * @return The formatted date as a String.
     */
    public String formatDate(long timestamp);
    
    
    
    /**
     * Formats the given int as a timespan.
     * 
     * @param span The timespan in seconds.
     * @return The formatted timespan string.
     */
    public String formatTimeSpan(long span);
    
}