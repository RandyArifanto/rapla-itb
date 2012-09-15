/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender                                     |
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
package org.rapla.entities.storage;
import java.util.Iterator;

import org.rapla.entities.Entity;
import org.rapla.entities.RaplaObject;

/**
The id is the unique key to distinct the entity from all others.
It is needed to safely update the entities and their associations (or aggregations)
with other entities.<br>
<b>Note:</b> Use this interface only in the
storage-backend.
*/
public interface RefEntity<T> extends Entity<T>, RaplaObject, EntityReferencer {
    Object getId();
    void setId(Object id);

    long getVersion();
    void setVersion(long version);

    /**
       returns all entities that are aggregated under the entity.
       This information is usefull to transparently store the
       subentities along with their parent.
       * The difference between subEntities and other references is,
     * that the subEntities are aggregated instead of associated. That
     * means SubEntities should be
     * <li>stored, when the parent is stored</li>
     * <li>deleted, when the parent is deleted or when they are
     * removed from the parent</li>
     */
    Iterator<RefEntity<?>> getSubEntities();

    /** returns if the entity contains the subEntity. */
    boolean isParentEntity(RefEntity<?> subEntity);
    

}







