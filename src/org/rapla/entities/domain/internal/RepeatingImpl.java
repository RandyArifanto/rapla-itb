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
package org.rapla.entities.domain.internal;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeSet;

import org.rapla.components.util.Assert;
import org.rapla.components.util.DateTools;
import org.rapla.entities.ReadOnlyException;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.RepeatingType;

class RepeatingImpl implements Repeating,java.io.Serializable {
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 1;
    
    private boolean readOnly = false;

    private int interval = 1;
    private boolean isFixedNumber;
    private int number = -1;
    private Date end;
    private RepeatingType repeatingType;
    private TreeSet<Date> exceptions;
    transient private Date[] exceptionArray;
    transient private boolean arrayUpToDate = false;
    private Appointment appointment;
    private long frequency;
    boolean monthly;
    boolean yearly;

    RepeatingImpl(RepeatingType type,Appointment appointment) {
        setType(type);
        setAppointment(appointment);
        setNumber( 1) ;
    }

    public void setType(RepeatingType repeatingType) {
    	if ( repeatingType == null )
    	{
    		throw new IllegalStateException("Repeating type cannot be null");
    	}
        checkWritable();
        this.repeatingType = repeatingType;
        monthly = false;
        yearly = false;
        if (repeatingType.equals( RepeatingType.WEEKLY ))
        {
            frequency = 7 * DateTools.MILLISECONDS_PER_DAY;
        }
        else if (repeatingType.equals( RepeatingType.MONTHLY))
        {
            frequency = 7 * DateTools.MILLISECONDS_PER_DAY;
            monthly = true;
        } 
        else if (repeatingType.equals( RepeatingType.DAILY))
        {    
            frequency = DateTools.MILLISECONDS_PER_DAY;
        }
        else if (repeatingType.equals( RepeatingType.YEARLY))
        {    
            frequency = DateTools.MILLISECONDS_PER_DAY;
            yearly = true;
        }
        else
        {
            throw new UnsupportedOperationException(" repeatingType " + repeatingType + " not supported");
        }
    }

    public RepeatingType getType() {
        return repeatingType;

    }

    void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public void setReadOnly(boolean enable) {
        this.readOnly = enable;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void checkWritable() {
        if ( readOnly )
            throw new ReadOnlyException( this );
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setInterval(int interval) {
        checkWritable();
        if (interval<1)
            interval = 1;
        this.interval = interval;
    }

    public int getInterval() {
        return interval;
    }

    public boolean isFixedNumber() {
        return isFixedNumber;
    }
    
    public boolean isWeekly() {
        return RepeatingType.WEEKLY.equals( getType());
    }
    
    public boolean isDaily() {
        return RepeatingType.DAILY.equals( getType());
    }
    
    public boolean isMonthly() {
        return monthly;
    }

    public boolean isYearly() {
        return yearly;
    }

    public void setEnd(Date end) {
        checkWritable();
        isFixedNumber = false;
        number = -1;
        this.end = end;
    }

    transient Date endTime;
    public Date getEnd() {
        if (!isFixedNumber)
            return end;
        if ( this.appointment == null)
            return null;

        if (endTime == null)
            endTime = new Date();

        if ( number < 1 )
        {
            return null;
        }
        
        if ( !isFixedIntervalLength())
        {
            int counts = (int) ((number -1) * interval) ;
            Calendar cal= DateTools.createGMTCalendar();
            cal.setTime( appointment.getStart());
            for ( int i=0;i< counts;i++)
            {
                if ( monthly)
                {
                    gotoNextMonth( cal, cal.getTime());
                }
                else
                {
                    gotoNextYear( cal, cal.getTime());
                }
                
            }
            return cal.getTime();
        }
        else
        {
            long intervalLength = frequency * interval;
            endTime.setTime(DateTools.fillDate( this.appointment.getStart().getTime()
                                           + (this.number -1)* intervalLength
                                           ));
        }
        return endTime;
    }

    /** returns interval-length in milliseconds.
    @see #getInterval
    */
    public long getFixedIntervalLength() {
        return frequency * interval;
    }

    public void setNumber(int number) {
        checkWritable();
        if (number>-1) {
            isFixedNumber = true;
            this.number = Math.max(number,1);
        } else {
            isFixedNumber = false;
            this.number = -1;
            setEnd(null);
        }

    }

    public boolean isException(long time) {
        if (!hasExceptions())
            return false;

        Date[] exceptions = getExceptions();
        if (exceptions.length == 0) {
            //          System.out.println("no exceptions");
            return false;
        }
        for (int i=0;i<exceptions.length;i++) {
            //System.out.println("Comparing exception " + exceptions[i] + " with " + new Date(time));
            if (exceptions[i].getTime()<=time
                && time<exceptions[i].getTime() + DateTools.MILLISECONDS_PER_DAY) {
                //System.out.println("Exception matched " + exceptions[i]);
                return true;
            }
        }
        return false;
    }

    public int getNumber() {
        if (number>-1)
            return number;
        if (end==null)
            return -1;
        //      System.out.println("End " + end.getTime() + " Start " + appointment.getStart().getTime() + " Duration " + duration);

        if ( isFixedIntervalLength() )
        {
            long duration = end.getTime()
            - DateTools.fillDate(appointment.getStart().getTime());
            if (duration<0)
                return 0;
            long intervalLength = getFixedIntervalLength();
            return (int) ((duration/ intervalLength) + 1);
        }
        else
        {
            Calendar cal= DateTools.createGMTCalendar();
            int number = 0;
            do 
            {
                number ++;
                if ( monthly)
                {
                    gotoNextMonth( cal, cal.getTime());
                }
                else
                {
                    gotoNextYear( cal, cal.getTime());
                }
            }
            while ( cal.getTime().before( end));
            return number;
        }            
            
    }

    public void addException(Date date) {
        checkWritable();
        if (exceptions == null)
            exceptions = new TreeSet<Date>();
        exceptions.add(date);
        arrayUpToDate = false;
    }

    public void removeException(Date date) {
        checkWritable();
        if (exceptions == null)
            return;
        exceptions.remove(date);
        if (exceptions.size()==0)
            exceptions = null;
        arrayUpToDate = false;
    }

    public void clearExceptions() {
        if (exceptions == null)
            return;
        exceptions.clear();
        exceptions = null;
        arrayUpToDate = false;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Repeating type=");
        buf.append(repeatingType);
        buf.append(" interval=");
        buf.append(interval);
        if (isFixedNumber()) {
            buf.append(" number=");
            buf.append(number);
        } else {
            if (end != null) {
                buf.append(" end-date=");
                buf.append(AppointmentImpl.fe(end.getTime()));
            }
        }
        return buf.toString();
    }

    @SuppressWarnings("unchecked")
	public Object clone()
    {
        RepeatingImpl dest = new RepeatingImpl(repeatingType,appointment);
        RepeatingImpl source = this; 
        copy(source, dest);
        dest.readOnly = false;// clones are always writable
        return dest;
    }

	private void copy(RepeatingImpl source, RepeatingImpl dest) 
	{
		dest.monthly = source.monthly;
        dest.yearly = source.yearly;
        dest.interval = source.interval;
        dest.isFixedNumber = source.isFixedNumber;
        dest.number = source.number;
        dest.end = source.end;
        dest.interval = source.interval;
        dest.exceptions = (TreeSet<Date>) ((source.exceptions != null) ? source.exceptions.clone(): null);
	}
    
    public void setFrom(Repeating repeating)
    {
    	checkWritable();
    	RepeatingImpl dest = this;
		dest.setType(repeating.getType());
    	RepeatingImpl source = (RepeatingImpl)repeating;
		copy( source, dest);
    }

    private static Date[] DATE_ARRAY = new Date[0];
    public Date[] getExceptions() {
        if (!arrayUpToDate) {
            if (exceptions != null)
                exceptionArray = (Date[])exceptions.toArray(DATE_ARRAY);
            else
                exceptionArray = DATE_ARRAY;
            arrayUpToDate = true;
        }
        return exceptionArray;
    }
    public boolean hasExceptions() {
        return exceptions != null && exceptions.size()>0;
    }

    final public long getIntervalLength( long s )
    {
        if ( isFixedIntervalLength())
        {
            return getFixedIntervalLength();
        }
        
        Date startDate = new Date(s);
        Calendar cal= DateTools.createGMTCalendar();
        if ( monthly)
        {
            gotoNextMonth( cal, startDate);
        }
        else
        {
            gotoNextYear( cal, startDate);
        }
        Date newDate = cal.getTime();
        long newTime = newDate.getTime(); 
        Assert.isTrue( newTime > s );
        return  newTime- s;
        
        // yearly
        
    }

    private void gotoNextMonth( Calendar cal, Date beginDate )
    {
        cal.setTime( appointment.getStart());
        int dayofweekinmonth = cal.get( Calendar.DAY_OF_WEEK_IN_MONTH);
        cal.setTime( beginDate);
        cal.add( Calendar.WEEK_OF_YEAR, 4);
        while ( cal.get( Calendar.DAY_OF_WEEK_IN_MONTH) != dayofweekinmonth )
        {
            //System.out.println( new MonthMapper().getName(month));
            cal.add( Calendar.WEEK_OF_YEAR, 1);
        }
    }

    private void gotoNextYear( Calendar cal, Date beginDate )
    {
        cal.setTime( appointment.getStart());
        int dayOfMonth = cal.get( Calendar.DAY_OF_MONTH);
        int month = cal.get( Calendar.MONTH);
        cal.setTime( beginDate);
        cal.add( Calendar.YEAR,1);
        while ( cal.get( Calendar.DAY_OF_MONTH) != dayOfMonth)
        {
            cal.add( Calendar.YEAR,1);
            cal.set( Calendar.MONTH, month);
            cal.set( Calendar.DAY_OF_MONTH, dayOfMonth);
        }
    }

    final public boolean isFixedIntervalLength()
    {
        return !monthly &&!yearly;
    }

    
}

