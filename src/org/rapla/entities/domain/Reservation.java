/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender, Christopher Kohlhaas               |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org .       |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/

package org.rapla.entities.domain;

import java.util.Date;
import org.rapla.entities.Entity;
import org.rapla.entities.Named;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.Timestamp;
import org.rapla.entities.dynamictype.Classifiable;

/** The <code>Reservation</code> interface is the central interface of
 *  Rapla.  Objects implementing this interface are the courses or
 *  events to be scheduled.  A <code>Reservation</code> consist
 *  of a group of appointments and a set of allocated
 *  resources (rooms, notebooks, ..) and persons.
 *  By default all resources and persons are allocated on every appointment.
 *  If you want to associate allocatable objects to special appointments
 *  use Restrictions.
 *
 *  @see Classifiable
 *  @see Appointment
 *  @see Allocatable
 */
public interface Reservation extends Entity<Reservation>,RaplaObject,Classifiable,Named,Ownable,Timestamp
{
    final RaplaType TYPE = new RaplaType(Reservation.class,"reservation");

    public final int MAX_RESERVATION_LENGTH = 100;

    void addAppointment(Appointment appointment);
    void removeAppointment(Appointment appointment);
    /** returns all appointments that are part off the reservation.*/
    Appointment[] getAppointments();
   /** Restrict an allocation to one ore more appointments.
    *  By default all objects of a reservation are allocated
    *  on every appointment. Restrictions allow to model
    *  relations between allocatables and appointments.
    *  A resource or person is restricted if its connected to
    *  one or more appointments instead the whole reservation.
    */
    void setRestriction(Allocatable alloc,Appointment[] appointments);
    Appointment[] getRestriction(Allocatable alloc);

    /** returns all appointments for an allocatable. This are either the restrictions, if there are any or all appointments
     * @see #getRestriction
     * @see #getAppointments*/
    Appointment[] getAppointmentsFor(Allocatable alloc);

    /** find an appointment in the reservation that equals the specified appointment. This is usefull if you have the
     * persistant version of an appointment and want to discover the editable appointment in the working copy of a reservation. 
     * This does only work with persistant entities, that have an id, Not with cloned one, because they don't inherit the id of the original*/
    Appointment findAppointment(Appointment appointment);

    void addAllocatable(Allocatable allocatable);
    void removeAllocatable(Allocatable allocatable);
    Allocatable[] getAllocatables();

    Allocatable[] getRestrictedAllocatables(Appointment appointment);

    /** get all allocatables that are allocated on the appointment, restricted and non restricted ones*/
    Allocatable[] getAllocatablesFor(Appointment appointment);
    
    /** returns if an the reservation has allocated the specified object. */
    boolean hasAllocated(Allocatable alloc);

    /** returns if the allocatable is reserved on the specified appointment. */
    boolean hasAllocated(Allocatable alloc,Appointment appointment);

    /** returns all persons that are associated with the reservation.
        Need not necessarily to be users of the System.
    */
    Allocatable[] getPersons();

    /** returns all resources that are associated with the reservation. */
    Allocatable[] getResources();
    
   

    public static final Reservation[] RESERVATION_ARRAY = new Reservation[0];

    /** @deprecated This are only used during the integration period of the occupation plugin. Will be removed in the next version*/ 
	void setSelectedSlotDate(Date start); 
	/** @deprecated This are only used during the integration period of the occupation plugin. Will be removed in the next version*/
	Date getSelectedSlotDate();
	
	/** returns the first (in time) start of all appointments. Returns null when the reservation has no appointments*/
	Date getFirstDate();
}







