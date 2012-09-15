/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender, Christopher Kohlhaas               |
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
package org.rapla.entities.storage.internal;

import java.util.ArrayList;
import java.util.Iterator;

import org.rapla.components.util.Assert;
import org.rapla.entities.Entity;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.ReadOnlyException;
import org.rapla.entities.User;
import org.rapla.entities.storage.EntityReferencer;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.Mementable;
import org.rapla.entities.storage.RefEntity;

/** Base-class for all Rapla Entity-Implementations. Provides services
 * for deep cloning and serialization of references. {@link ReferenceHandler}
*/

public abstract class SimpleEntity<T> implements RefEntity<T>, Mementable<T>, Comparable<T>,EntityReferencer , java.io.Serializable
{
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 2;
    private SimpleIdentifier id;
    private long version = 0;

    ReferenceHandler subEntityHandler = new ReferenceHandler();
    ReferenceHandler referenceHandler = new ReferenceHandler();

    transient boolean readOnly = false;

    public SimpleEntity() {

    }

    public void checkWritable() {
        if ( readOnly )
            throw new ReadOnlyException( this );
    }
    
    public boolean isPersistant() {
        return isReadOnly();
    }

    public void resolveEntities( EntityResolver resolver) throws EntityNotFoundException {
        referenceHandler.resolveEntities( resolver);
        subEntityHandler.resolveEntities( resolver );
    }
    
    public void setReadOnly(boolean enable) {
        this.readOnly = enable;
        Iterator<RefEntity<?>> it = getSubEntityHandler().getReferences();
        while (it.hasNext()) {
            ((SimpleEntity<?>) it.next()).setReadOnly(enable);
        }
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public User getOwner() {
        return (User) referenceHandler.get("owner");
    }

    @SuppressWarnings("unchecked")
	public void setOwner(User owner) {
        referenceHandler.put("owner",(RefEntity<User>)owner);
    }
    
	public User getLastChangedBy() {
		return (User) referenceHandler.get("last_changed_by");
	}

	@SuppressWarnings("unchecked")
	public void setLastChangedBy(User user) {
        referenceHandler.put("last_changed_by",(RefEntity<User>)user);
	}


    protected boolean isSubEntity(RefEntity<?> obj) {
        return subEntityHandler.isRefering(obj);
    }

    protected void addEntity(RefEntity<?> entity) {
        subEntityHandler.add(entity);
    }

    public ReferenceHandler getReferenceHandler() {
        return referenceHandler;
    }

    public ReferenceHandler getSubEntityHandler() {
        return subEntityHandler;
    }

    protected void removeEntity(RefEntity<?> entity) {
        subEntityHandler.isRefering(entity);
        subEntityHandler.remove(entity);
    }


    /** sets the identifier for an object. The identifier should be
     * unique accross all entities (not only accross the entities of a
     * the same type). Once set, the identifier for an object should
     * not change. The identifier is necessary to store the relationsships
     * between enties.
     * @see SimpleIdentifier
     */

    public void setId(Object id)  {
        this.id= (SimpleIdentifier)id;
    }

    /** @return the identifier of the object.
     * @see SimpleIdentifier
     */
    final public Object getId()  {
        return id;
    }

    final public boolean isIdentical(Entity<?> ob2) {
        return equals( ob2);
    }

    /** two Entities are equal if they are identical.
     * @see #isIdentical
     */
    final public boolean equals(Object o) {
        if (!( o instanceof SimpleEntity))
        {
            return false;
        }
        @SuppressWarnings("rawtypes")
		SimpleEntity e2 = (SimpleEntity) o;
        SimpleIdentifier id2 = e2.id; 
        if ( id2== null || id == null)
            return e2 == this;
        return (id2.key == id.key && id2.type == id.type);

    }

    /** The hashcode of the id-object will be returned.
     * @return the hashcode of the id.
     * @throws IllegalStateException if no id is set.
    */
    public int hashCode() {
        if ( id != null) {
            return id.hashCode();
        } else {
            throw new IllegalStateException("Id not set for type '" +  getRaplaType()
                                            + "'. You must set an Id before you can use the hashCode method."
                                            );
        }
    }

    public void setVersion(long version)  {
        this.version= version;
    }

    public long getVersion()  {
        return version;
    }

    public Iterator<RefEntity<?>> getSubEntities() {
        return getSubEntityHandler().getReferences();
    }

    public Iterator<RefEntity<?>> getReferences() {
        return getReferenceHandler().getReferences();
    }

    public boolean isRefering(RefEntity<?> entity) {
        ReferenceHandler referenceHandler = getReferenceHandler();
        return referenceHandler.isRefering(entity);
    }

    public boolean isParentEntity(RefEntity<?> object) {
        return getSubEntityHandler().isRefering(object);
    }
    
	@SuppressWarnings("unchecked")
	static private <T> void copy(SimpleEntity<T> source,SimpleEntity<T> dest,boolean deepCopy) {
        Assert.isTrue(source != dest,"can't copy the same object");

        dest.referenceHandler = (ReferenceHandler) source.referenceHandler.clone();

        ArrayList<RefEntity<?>> newEntities = new ArrayList<RefEntity<?>>();
        Iterator<RefEntity<?>> it = source.getSubEntityHandler().getReferences();
        if (deepCopy){
            while (it.hasNext()) {
                RefEntity<?> entity =  it.next();
                RefEntity<?> oldEntity = dest.findEntity(entity);
                if (oldEntity != null) {
                    ((Mementable<RefEntity<?>>)oldEntity).copy(entity);
                    newEntities.add( oldEntity);
                } else {
                    newEntities.add( ((Mementable<RefEntity<?>>)entity).deepClone());
                }
            }
        } else {
            while (it.hasNext()) {
                RefEntity<?> entity =  it.next();
                RefEntity<?> oldEntity = dest.findEntity((RefEntity<?>)entity);
                if (oldEntity != null) {
                    newEntities.add( oldEntity);
                } else {
                    newEntities.add( entity);
                }
            }
        }
        dest.getSubEntityHandler().clearReferences();
        it = newEntities.iterator();
        while (it.hasNext()) {
            RefEntity<?> entity =  it.next();
            dest.addEntity( entity );
        }
        // In a copy operation the target/destination object should always be writable
        dest.readOnly = false;
        dest.setVersion(source.getVersion());
    }

    /** find the sub-entity that has the same id as the passed copy. Returns null, if the entity was not found. */
	public RefEntity<?> findEntity(RefEntity<?> copy) {
        Iterator<RefEntity<?>> it = getSubEntities();
        while (it.hasNext()) {
            RefEntity<?> entity = it.next();
            if (entity.equals(copy)) {
                return entity;
            }
        }
        return null;
    }

    /** copies the references from the entity to this */
    protected void copy(SimpleEntity<T> entity) {
        copy(entity,this,false);
    }

    protected void deepClone(SimpleEntity<T> clone) {
        clone.setId(id);
        copy(this,clone,true);
    }

    protected void clone(SimpleEntity<T> clone) {
        clone.setId(id);
        copy(this,clone,false);
    }
    
    abstract public T clone();
    
    @SuppressWarnings("unchecked")
	public T cast()
    {
    	return (T) this;
    }

    public String toString() {
        if (id != null)
            return id.toString();
        return "no id for " + super.toString();
    }

    public int compareTo(T o) {
         if ( o == this )
         {
             return 0;
         }
         SimpleIdentifier id1 = id;
         SimpleIdentifier id2 = (SimpleIdentifier)((RefEntity<?>) o).getId();
         if ( equals( o))
             return 0;
         return (id1.getKey() < id2.getKey()) ? -1 : 1;
        
    }
}




