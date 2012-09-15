/**
 * 
 */
package org.rapla.storage.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.rapla.components.util.Assert;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.internal.CategoryImpl;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.RefEntity;
import org.rapla.storage.LocalCache;

public class EntityStore implements EntityResolver {
    HashMap<Object,RefEntity<?>> entities = new HashMap<Object,RefEntity<?>>();
    HashSet<Object> idsToRemove = new HashSet<Object>();
    LocalCache parent;
    HashMap<String,DynamicType> dynamicTypes = new HashMap<String,DynamicType>();
    HashSet<Allocatable> allocatables = new HashSet<Allocatable>();
    
    CategoryImpl superCategory;
    HashMap<Object,String> passwordList = new HashMap<Object,String>();
    long repositoryVersion;
    
    public EntityStore(LocalCache parent,CategoryImpl superCategory) {
        this.parent = parent;
        this.superCategory = superCategory;
       // put( superCategory);
    }
    
    void addAll(Collection<RefEntity<?>> collection) {
        Iterator<RefEntity<?>> it = collection.iterator();
        while (it.hasNext())
        {
            put(it.next());
        }
    }

    public void put(RefEntity<?> entity) {
        Object id = entity.getId();
        Assert.notNull(id);
        if ( entity.getRaplaType().equals( DynamicType.TYPE))
        {
            DynamicType dynamicType = (DynamicType) entity;
            dynamicTypes.put ( dynamicType.getElementKey(), dynamicType);
        }
        if ( entity.getRaplaType().equals( Allocatable.TYPE))
        {
        	Allocatable allocatable = (Allocatable) entity;
            allocatables.add (  allocatable);
        }
        entities.put(id,entity);
    }
    
    public void addRemoveId(Object id)
    {
        idsToRemove.add(id);
    }
    
    public DynamicType getDynamicType(String key)
    {
        // todo super 
        DynamicType type =  dynamicTypes.get( key);
        if ( type == null && parent != null) 
        {
            type = parent.getDynamicType( key);
        }
        return type;
    }
    
    public Collection<RefEntity<?>> getList() {
        return entities.values();
    }
    
    public Collection<?> getRemoveIds() {
        return  idsToRemove;
    }

    // Implementation of EntityResolver
    public RefEntity<?> resolve(Object id) throws EntityNotFoundException {
        RefEntity<?> result = get (id );
        if ( result == null)
        {
            throw new EntityNotFoundException("Object for id " + id.toString() + " not found");
        }
        return result;
    }
    
    public RefEntity<?> resolveEmail(final String emailArg) throws EntityNotFoundException
    {
    	for (Allocatable entity: allocatables)
    	{
    		final Classification classification = entity.getClassification();
    		final Attribute attribute = classification.getAttribute("email");
    		if ( attribute != null)
    		{
    			final String email = (String)classification.getValue(attribute);
    			if ( email != null && email.equals( emailArg))
    			{
    				return (RefEntity<?>)entity;
    			}
    		}
        }
    	throw new EntityNotFoundException("Object for email " + emailArg + " not found");
    }

    public CategoryImpl getSuperCategory()
    {
        return superCategory;
    }

    public void putPassword( Object userid, String password )
    {
        passwordList.put(userid, password);
    }
    
    public String getPassword( Object userid)
    {
        return (String)passwordList.get(userid);
    }

    public RefEntity<?> get( Object id )
    {
        if ( id.equals( superCategory.getId()))
        {
            return superCategory;
        }
        RefEntity<?> entity = entities.get(id);
        if (entity != null)
            return entity;

        if (parent != null)
        {
            return parent.get(id);
            
        }
        return null;
    }

    public Collection<RaplaObject> getCollection( RaplaType raplaType )
    {
        List<RaplaObject> collection = new ArrayList<RaplaObject>();
        Iterator<RefEntity<?>> it = entities.values().iterator();
        while (it.hasNext())
        {
            RaplaObject obj = (RaplaObject)it.next();
            if ( obj.getRaplaType().equals( raplaType))
            {
                collection.add( obj);
            }
        }
        return collection;
    }

    public long getRepositoryVersion()
    {
        return repositoryVersion;
    }

    public void setRepositoryVersion( long repositoryVersion )
    {
        this.repositoryVersion = repositoryVersion;
    }

        
    
}