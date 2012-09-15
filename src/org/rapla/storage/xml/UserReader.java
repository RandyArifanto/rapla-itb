/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
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

package org.rapla.storage.xml;

import org.rapla.entities.Category;
import org.rapla.entities.internal.UserImpl;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class UserReader extends RaplaXMLReader
{
    UserImpl user;
    PreferenceReader preferenceHandler;

    public UserReader( RaplaContext context ) throws RaplaException
    {
        super( context );
        preferenceHandler = new PreferenceReader( context );
        addChildHandler( preferenceHandler );
    }

    public void processElement(
        String namespaceURI,
        String localName,
        String qName,
        Attributes atts ) throws SAXException
    {
        if (!namespaceURI.equals( RAPLA_NS ))
            return;

        if (localName.equals( "user" ))
        {
            user = new UserImpl();
            Object id = setId( user, atts );
            setVersionIfThere( user, atts);
            user.setUsername( getString( atts, "username", "" ) );
            user.setName( getString( atts, "name", "" ) );
            user.setEmail( getString( atts, "email", "" ) );
            user.setAdmin( getString( atts, "isAdmin", "false" ).equals( "true" ) );
            String password = getString( atts, "password", null );
            preferenceHandler.setUser( user );
            if ( password != null)
            {
                putPassword( id, password );
            }
        }

        if (localName.equals( "group" ))
        {
            Category group;
            
            String groupId = atts.getValue( "idref" );
            if (groupId !=null)
            {
                group = (Category) resolve( Category.TYPE, groupId );
            }
            else
            {
                String groupKey = getString( atts, "key" );
                group = getGroup( groupKey);
            }
            if (group != null)
            {
                user.addGroup( group );
            }
        }

        if (localName.equals( "preferences" ))
        {
            delegateElement(
                preferenceHandler,
                namespaceURI,
                localName,
                qName,
                atts );
        }
    }
    
    public void processEnd( String namespaceURI, String localName, String qName )
        throws SAXException
    {
        if (!namespaceURI.equals( RAPLA_NS ))
            return;

        if (localName.equals( "user" ))
        {
            preferenceHandler.setUser( null );
            add( user );
        }
    }

}
