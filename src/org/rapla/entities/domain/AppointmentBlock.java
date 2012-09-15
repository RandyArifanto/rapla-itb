/*--------------------------------------------------------------------------*
 | Copyright (C) 2011 Christopher Kohlhaas                                  |
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
package org.rapla.entities.domain;

import java.text.DateFormat;
import java.util.Date;



/**
 * This class represents a time block of an appointment and is currently only used for performance
 * reasons
 * 
 * @since Rapla 1.4
 */
public class AppointmentBlock implements Comparable<AppointmentBlock>
{
    long start;
    long end;
    boolean isException;
    private Appointment	appointment;
	
	/**
	 * Basic constructor
	 */
	public AppointmentBlock(long start, long end, Appointment appointment, boolean isException)
	{
		this.start = start;
		this.end = end;
		this.appointment = appointment;
		this.isException = isException;
	}
	
	/**
	 * Returns the start date of this block
	 * 
	 * @return Date
	 */
	public long getStart()
	{
		return start;
	}
	
	/**
	 * Returns the end date of this block
	 * 
	 * @return Date
	 */
	public long getEnd()
	{
		return end;
	}
	
	/**
     * Returns if the block is an exception from the appointment rule
     * 
     */
    public boolean isException()
    {
        return isException;
    }
	/**
	 * Returns the appointment to which this block belongs
	 * 
	 * @return Appointment
	 */
	public Appointment getAppointment()
	{
		return appointment;
	}
	
	/**
     * This method is used to compare two appointment blocks by their start dates
     */
	public int compareTo(AppointmentBlock other) 
	{
        if (other.start > start)
            return -1;
        if (other.start < start) 
            return 1;
        if (other.end > end)
            return 1;
        if (other.end < end)
            return -1;
        if ( other == this)
        {
            return 0;
        }
        return appointment.compareTo(other.appointment);
    }
	
	public String toString()
	{
		
		DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		return format.format(new Date(start)) + " - " + format.format(new Date(end));
	}
}
