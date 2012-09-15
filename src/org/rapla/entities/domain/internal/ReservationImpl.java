/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org .       |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, of which license fullfill the Open Source     |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.entities.domain.internal;
/** The default Implementation of the <code>Reservation</code>
 *  @see ModificationEvent
 *  @see org.rapla.facade.ClientFacade
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import org.rapla.components.util.Assert;
import org.rapla.components.util.iterator.IteratorChain;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaType;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentStartComparator;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.internal.ClassificationImpl;
import org.rapla.entities.internal.ModifiableTimestamp;
import org.rapla.entities.storage.CannotExistWithoutTypeException;
import org.rapla.entities.storage.DynamicTypeDependant;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleEntity;


public class ReservationImpl extends SimpleEntity<Reservation> implements Reservation,java.io.Serializable, ModifiableTimestamp, DynamicTypeDependant
{
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 1;

    private ClassificationImpl classification;
    private Map<Object,Appointment[]> restrictions;
    private Date lastChanged;
    private Date createDate;
    private Date slotDate;
    transient private boolean allocatableArrayUpToDate = false;
    transient private boolean appointmentArrayUpToDate = false;
    // The resolved references
    transient private Allocatable[] allocatables;
    transient private Allocatable[] persons;
    transient private Allocatable[] resources;
    transient private Appointment[] appointments;

    ReservationImpl() {
        this (null, null);
    }

    public ReservationImpl( Date createDate, Date lastChanged ) {
        this.createDate = createDate;
        if (createDate == null)
            this.createDate = new Date();
        this.lastChanged = lastChanged;
        if (lastChanged == null)
            this.lastChanged = this.createDate;
    }

    public void resolveEntities( EntityResolver resolver) throws EntityNotFoundException {
        super.resolveEntities( resolver);
        allocatableArrayUpToDate = false;
        appointmentArrayUpToDate = false;
        classification.resolveEntities( resolver);
    }

    public void setReadOnly(boolean enable) {
        super.setReadOnly( enable );
        classification.setReadOnly( enable );
    }

    final public RaplaType getRaplaType() {return TYPE;}

    // Implementation of interface classifiable
    public Classification getClassification() { return classification; }
    public void setClassification(Classification classification) {
        checkWritable();
        this.classification = (ClassificationImpl) classification;
    }

    public String getName(Locale locale) {
        Classification c = getClassification();
        if (c == null)
            return "";
        return c.getName(locale);
    }

    public Date getLastChangeTime() {
        return lastChanged;
    }

    public Date getCreateTime() {
        return createDate;
    }

    public void setLastChanged(Date date) {
        lastChanged = date;
    }

    public Appointment[] getAppointments()   {
        updateAppointmentArray();
        return appointments;
    }

    public Iterator<RefEntity<?>> getReferences() {
        return new IteratorChain<RefEntity<?>>
            (
             super.getReferences()
             ,classification.getReferences()
            )
            ;
    }

    public boolean isRefering(RefEntity<?> object) {
        if (super.isRefering(object))
            return true;
        return classification.isRefering(object);
    }

    public void addAppointment(Appointment appointment) {
        checkWritable();
        if (super.isSubEntity((RefEntity<?>) appointment))
            return;
        if (appointment.getReservation() != null
            && !this.isIdentical(appointment.getReservation()))
            throw new IllegalStateException("Appointment '" + appointment
                                            + "' belongs to another reservation :"
                                            + appointment.getReservation());
        ((AppointmentImpl) appointment).setParent(this);
        appointmentArrayUpToDate = false;
        super.addEntity((RefEntity<?>)appointment);
    }


    public void removeAppointment(Appointment appointment)   {
        checkWritable();
        if ( findAppointment( appointment ) == null)
            return;
        appointmentArrayUpToDate = false;
        super.removeEntity((RefEntity<?>)appointment);
        clearRestrictions(appointment);
        if (this.equals(appointment.getReservation()))
            ((AppointmentImpl) appointment).setParent(null);
    }

    protected void clearRestrictions(Appointment appointment) {
        if (restrictions == null)
            return;
        ArrayList<Object> list = null;
        for (Object key:restrictions.keySet()) {
            Appointment[] appointments = (Appointment[]) restrictions.get(key);
            for ( int i = 0; i < appointments.length; i++) {
                if (list == null)
                    list = new ArrayList<Object>();
                list.add(key);
            }
        }
        if (list == null)
            return;
        for (Object key: list) {
            Appointment[] appointments = (Appointment[]) restrictions.get(key);
            ArrayList<Appointment> newApps = new ArrayList<Appointment>();
            for ( int i=0; i< appointments.length; i++) {
                if ( !appointments[i].equals( appointment ) ) {
                    newApps.add( appointments[i] );
               }
            }
            setRestrictionForId( key, newApps.toArray( Appointment.EMPTY_ARRAY) );
        }
    }

    public void addAllocatable(Allocatable allocatable) {
        checkWritable();
        if (getReferenceHandler().isRefering((RefEntity<?>)allocatable))
            return;
        allocatableArrayUpToDate = false;
        getReferenceHandler().add((RefEntity<?>)allocatable);
    }

    public void removeAllocatable(Allocatable allocatable)   {
        checkWritable();
        if (!getReferenceHandler().isRefering((RefEntity<?>)allocatable))
            return;
        getReferenceHandler().remove((RefEntity<?>)allocatable);
        allocatableArrayUpToDate = false;
    }

    public Allocatable[] getAllocatables()  {
        updateAllocatableArrays();
        return allocatables;
    }

    public Allocatable[] getResources() {
        updateAllocatableArrays();
        return resources;
    }

    public Allocatable[] getPersons() {
        updateAllocatableArrays();
        return persons;
    }

    private void updateAppointmentArray() {
        if (appointmentArrayUpToDate)
            return;
        Collection<Appointment> appointmentList = new TreeSet<Appointment>(new AppointmentStartComparator());
        Iterator<RefEntity<?>> it = super.getSubEntities();
        while (it.hasNext()) {
            RefEntity<?> o = it.next();
            if (o.getRaplaType().equals( Appointment.TYPE) ) {
                appointmentList.add((Appointment)o);
                //              System.out.println("Appointment " + o + " belongs to reservation " + this);
            }
        }

        appointments = appointmentList.toArray(Appointment.EMPTY_ARRAY);
        //notwendig?
        //Arrays.sort(appointments, new AppointmentStartComparator());
    }

    private void updateAllocatableArrays() {
        if (allocatableArrayUpToDate)
            return;
        Collection<Allocatable> allocatableList = new ArrayList<Allocatable>();
        Collection<Allocatable> resourceList = new ArrayList<Allocatable>();
        Collection<Allocatable> personList = new ArrayList<Allocatable>();
        Iterator<RefEntity<?>> it = super.getReferences();
        while (it.hasNext()) {
            RefEntity<?> o =  it.next();
            if (o.getRaplaType().equals( Allocatable.TYPE)) {
            	Allocatable alloc = (Allocatable) o;
            	if (alloc.isPerson()) {
            		personList.add(alloc);
            	} else {
            		resourceList.add(alloc);
            	}
                allocatableList.add(alloc);
            }
        }
        allocatables = allocatableList.toArray(Allocatable.ALLOCATABLE_ARRAY);
        persons = personList.toArray(Allocatable.ALLOCATABLE_ARRAY);
        resources = resourceList.toArray(Allocatable.ALLOCATABLE_ARRAY);
        allocatableArrayUpToDate = true;
    }

    public boolean hasAllocated(Allocatable allocatable) {
        return getReferenceHandler().isRefering((RefEntity<?>)allocatable);
    }

    public boolean hasAllocated(Allocatable allocatable,Appointment appointment) {
        if (!hasAllocated(allocatable))
            return false;
        Appointment[] restrictions = getRestriction(allocatable);
        for ( int i = 0; i < restrictions.length; i++) {
            if ( restrictions[i].equals( appointment ) )
                 return true;
        }
        return restrictions.length == 0;
    }

    public void setRestriction(Allocatable allocatable,Appointment[] appointments) {
        Object id = ((RefEntity<?>)allocatable).getId();
        Assert.notNull(id,"Allocatable object has no ID");
        setRestrictionForId(id,appointments);
    }

    public void setRestrictionForId(Object id,Appointment[] appointments) {
        if (restrictions == null)
            restrictions = new HashMap<Object,Appointment[]>();
        if (appointments == null || appointments.length == 0)
            restrictions.remove(id);
        else
            restrictions.put(id, appointments);
    }
    
    public void addRestrictionForId(Object id,Appointment appointment) {
        if (restrictions == null)
            restrictions = new HashMap<Object,Appointment[]>();
        Appointment[] appointmentsNew;
        Appointment[] appointments = (Appointment[])restrictions.get( id );
        if ( appointments != null)
        {
            appointmentsNew = new Appointment[appointments.length + 1];
            System.arraycopy( appointments, 0,appointmentsNew, 0, appointments.length );
        }
        else
        {
            appointmentsNew = new Appointment[1];
        }
        appointmentsNew[appointmentsNew.length -1] = appointment;
        restrictions.put(id, appointmentsNew);
    }

    public Appointment[] getRestriction(Allocatable allocatable) {
        Object id = ((RefEntity<?>)allocatable).getId();
        Assert.notNull(id,"Allocatable object has no ID");
        if (restrictions != null) {
            Appointment[] restriction = (Appointment[]) restrictions.get(id);
            if (restriction != null)
                return restriction;
        }
        return Appointment.EMPTY_ARRAY;
    }

    public Appointment[] getAppointmentsFor(Allocatable allocatable) {
        Appointment[] restrictedAppointments = getRestriction( allocatable);
        if ( restrictedAppointments.length == 0)
            return getAppointments();
        else
            return restrictedAppointments;
    }

    public Allocatable[] getRestrictedAllocatables(Appointment appointment) {
        HashSet<Allocatable> set = new HashSet<Allocatable>();
        Allocatable[] allocatables = getAllocatables();
        for (int i=0;i<allocatables.length;i++) {
            Appointment[] restriction = getRestriction( allocatables[i] );
            for (int j = 0; j < restriction.length; j++ ) {
                if ( restriction[j].equals( appointment ) ) {
                    set.add( allocatables[i] );
                }
            }
        }
        return set.toArray( Allocatable.ALLOCATABLE_ARRAY);
    }

    public Allocatable[] getAllocatablesFor(Appointment appointment) {
        HashSet<Allocatable> set = new HashSet<Allocatable>();
        Allocatable[] allocatables = getAllocatables();
        for (int i=0;i<allocatables.length;i++) {
            Appointment[] restriction = getRestriction( allocatables[i] );
            for (int j = 0; j < restriction.length; j++ ) {
                if ( restriction[j].equals( appointment ) ) {
                    set.add( allocatables[i] );
                }
            }
            if ( restriction.length == 0)
            {
            	set.add(allocatables[i]);
            }
        }
        return  set.toArray( Allocatable.ALLOCATABLE_ARRAY);
    }

    public Appointment findAppointment(Appointment copy) {
        return (Appointment) super.findEntity((RefEntity<?>)copy);
    }

    static private void copy(ReservationImpl source,ReservationImpl dest) {
        // First we must invalidate the arrays.
        dest.allocatableArrayUpToDate = false;
        dest.appointmentArrayUpToDate = false;

        dest.classification = (ClassificationImpl) source.classification.clone();
        dest.restrictions = null;

        Allocatable[] allocatables = dest.getAllocatables();
        for (int i=0;i<allocatables.length;i++) {
            RefEntity<?> reference = (RefEntity<?>) allocatables[i];
            if (reference instanceof Allocatable) {
                // We have to copy the restrictions
                Appointment[] sourceRestriction = source.getRestriction( allocatables[i] );
                Appointment[] destRestriction = new Appointment[ sourceRestriction.length ];
                for ( int j = 0; j < sourceRestriction.length; j ++) {
                    destRestriction[j] = dest.findAppointment( sourceRestriction[j] );
                }
                dest.setRestriction( allocatables[i],  destRestriction );
            }
        }
        Iterator<RefEntity<?>> it = dest.getSubEntities();
        while ( it.hasNext()) {
            ((AppointmentImpl)it.next()).setParent(dest);
        }

        dest.createDate = source.createDate;
        dest.lastChanged = source.lastChanged;
        dest.slotDate = source.slotDate;
    }

    public boolean needsChange(DynamicType type) {
        return classification.needsChange( type );
    }

    public void commitChange(DynamicType type) {
        classification.commitChange( type );
    }


    @SuppressWarnings("unchecked")
	public void copy(Reservation obj) {
        super.copy((SimpleEntity<Reservation>)obj);
        copy((ReservationImpl)obj,this);
    }

    public Reservation clone() {
        ReservationImpl clone = new ReservationImpl();
        super.clone(clone);
        copy(this,clone);
        return clone;
    }

    public Reservation deepClone() {
        ReservationImpl clone = new ReservationImpl();
        super.deepClone(clone);
        copy(this,clone);
        return clone;
    }

    public Date getSelectedSlotDate() {
    // get the Date of the GUI selected slot
    	return slotDate;
    }

    public void setSelectedSlotDate(Date d) {
        // set the Date of the GUI selected slot
        slotDate=d;
       return;
       }
    
    public void commitRemove(DynamicType type) throws CannotExistWithoutTypeException 
    {
        classification.commitRemove(type);
    }

	public Date getFirstDate() 
	{
        Appointment[] apps = getAppointments();
        Date minimumDate = null;
        for (int i=0;i< apps.length;i++) {
            Appointment app = apps[i];
            if ( minimumDate == null || app.getStart().before( minimumDate)) {
                minimumDate = app.getStart();
            }
        }
        return minimumDate;
	}
}








