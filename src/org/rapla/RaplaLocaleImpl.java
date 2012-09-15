/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.DateTools;
import org.rapla.components.xmlbundle.LocaleSelector;
import org.rapla.components.xmlbundle.impl.LocaleSelectorImpl;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
class RaplaLocaleImpl implements RaplaLocale {
   
    TimeZone zone;
    TimeZone systemTimezone;
    LocaleSelector localeSelector = new LocaleSelectorImpl();

    String[] availableLanguages;

    String COUNTRY = "country";
    String LANGUAGES = "languages";
    String LANGUAGE = "language";
    String CHARSET = "charset";
    String charsetForHtml;

    public RaplaLocaleImpl(Configuration config ) throws RaplaException 
    {
        this(config, null);
    }
    
    public RaplaLocaleImpl(Configuration config,Logger logger ) throws RaplaException 
    {
        String selectedCountry = config.getChild( COUNTRY).getValue(Locale.getDefault().getCountry() );
        Configuration languageConfig = config.getChild( LANGUAGES );
        Configuration[] languages = languageConfig.getChildren( LANGUAGE );
        charsetForHtml = config.getChild(CHARSET).getValue("iso-8859-15");
        availableLanguages = new String[languages.length];
        for ( int i=0;i<languages.length;i++ ) {
            availableLanguages[i] = languages[i].getValue("en");
        }
        String selectedLanguage = languageConfig.getAttribute( "default", Locale.getDefault().getLanguage() );
        if (selectedLanguage.trim().length() == 0)
            selectedLanguage = Locale.getDefault().getLanguage();
        localeSelector.setLocale( new Locale(selectedLanguage, selectedCountry) );
        zone = DateTools.getTimeZone();
        //# need to add this for ResultSet.getDate
        // This call is neccessary for the ResultSet.getDate() method. Must be some internal details
        // We also need to store the system timesone. For that, we need to set the default time to null and read it again.
        // We read systemTimezone twice, because SecurityExceptions can prevent the setDefault methods
        systemTimezone = TimeZone.getDefault();
        try
        {
        	TimeZone.setDefault( null );
        	systemTimezone = TimeZone.getDefault();
            TimeZone.setDefault( zone );
        }
        catch (SecurityException ex)
        {
            if ( logger != null)
            {
                logger.error( "Can't set Timezone to gmt", ex );
            }
        }
    }

    public TimeZone getSystemTimezone() {
		return systemTimezone;
	}

	LocaleSelector getLocaleSelector() {
        return localeSelector;
    }

    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#getAvailableLanguages()
     */
    public String[] getAvailableLanguages() {
        return availableLanguages;
    }

    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#createCalendar()
     */
    public Calendar createCalendar() {
        return Calendar.getInstance( zone, getLocale() );
    }


    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#formatTime(java.util.Date)
     */
    public String formatTime( Date date ) {
        DateFormat format = DateFormat.getTimeInstance( DateFormat.SHORT, getLocale() );
        format.setTimeZone( getTimeZone() );
        return format.format( date );
    }


    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#toDate(java.util.Date, boolean)
     */
    public Date toDate( Date date, boolean fillDate ) {
        Calendar cal1 = createCalendar();
        cal1.setTime( date );
        if ( fillDate ) {
            cal1.add( Calendar.DATE, 1);
        }
        cal1.set( Calendar.HOUR_OF_DAY, 0 );
        cal1.set( Calendar.MINUTE, 0 );
        cal1.set( Calendar.SECOND, 0 );
        cal1.set( Calendar.MILLISECOND, 0 );
        return cal1.getTime();
    }

    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#toDate(int, int, int)
     */
    public Date toDate( int year,int month, int date ) {
        Calendar calendar = createCalendar();
        calendar.set( Calendar.YEAR, year );
        calendar.set( Calendar.MONTH, month );
        calendar.set( Calendar.DATE, date );
        calendar.set( Calendar.HOUR_OF_DAY, 0 );
        calendar.set( Calendar.MINUTE, 0 );
        calendar.set( Calendar.SECOND, 0 );
        calendar.set( Calendar.MILLISECOND, 0 );
        return calendar.getTime();
    }

    public Date toTime( int hour,int minute, int second ) {
        Calendar calendar = createCalendar();
        calendar.set( Calendar.HOUR_OF_DAY, hour );
        calendar.set( Calendar.MINUTE, minute );
        calendar.set( Calendar.SECOND, second );
        calendar.set( Calendar.MILLISECOND, 0 );
        return calendar.getTime();
    }

    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#toDate(java.util.Date, java.util.Date)
     */
    public Date toDate( Date date, Date time ) {
        Calendar cal1 = createCalendar();
        Calendar cal2 = createCalendar();
        cal1.setTime( date );
        cal2.setTime( time );
        cal1.set( Calendar.HOUR_OF_DAY, cal2.get(Calendar.HOUR_OF_DAY) );
        cal1.set( Calendar.MINUTE, cal2.get(Calendar.MINUTE) );
        cal1.set( Calendar.SECOND, cal2.get(Calendar.SECOND) );
        cal1.set( Calendar.MILLISECOND, cal2.get(Calendar.MILLISECOND) );
        return cal1.getTime();
    }


    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#formatNumber(long)
     */
    public String formatNumber( long number ) {
        return NumberFormat.getInstance( getLocale()).format(number );
    }

    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#formatDateShort(java.util.Date)
     */
    public String formatDateShort( Date date ) {
        FieldPosition fieldPosition = new FieldPosition( DateFormat.YEAR_FIELD );
        StringBuffer buf = new StringBuffer();
        DateFormat format = DateFormat.getDateInstance( DateFormat.SHORT, getLocale() );
        format.setTimeZone( zone );
        buf=format.format(date,
                           buf,
                           fieldPosition
                           );
        if ( fieldPosition.getEndIndex()<buf.length() ) {
            buf.delete( fieldPosition.getBeginIndex(), fieldPosition.getEndIndex()+1 );
        } else if ( (fieldPosition.getBeginIndex()>=0) ) {
            buf.delete( fieldPosition.getBeginIndex(), fieldPosition.getEndIndex() );
        }
        return buf.toString();
    }

    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#formatDate(java.util.Date)
     */
    public String formatDate( Date date ) {
        DateFormat format = DateFormat.getDateInstance( DateFormat.SHORT, getLocale() );
        format.setTimeZone( zone );
        return format.format( date );
    }


    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#formatDateLong(java.util.Date)
     */
    public String formatDateLong( Date date ) {
        DateFormat format = DateFormat.getDateInstance( DateFormat.MEDIUM, getLocale() );
        format.setTimeZone( zone );
        return format.format( date) + " (" + getWeekday(date) + ")";
    }

    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#getWeekday(java.util.Date)
     */
    public String getWeekday( Date date ) {
        SimpleDateFormat format = new SimpleDateFormat( "EE", getLocale() );
        format.setTimeZone( getTimeZone() );
        return format.format( date );
    }

    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#getMonth(java.util.Date)
     */
    public String getMonth( Date date ) {
        SimpleDateFormat format = new SimpleDateFormat( "MMMMM", getLocale() );
        format.setTimeZone( getTimeZone() );
        return format.format( date );
    }


    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#getTimeZone()
     */
    public TimeZone getTimeZone() {
        return zone;
    }

    /* (non-Javadoc)
     * @see org.rapla.common.IRaplaLocale#getLocale()
     */
    public Locale getLocale() {
        return localeSelector.getLocale();
    }
    
    public String getCharsetNonUtf()
    {
        return charsetForHtml;
    }
    

}


