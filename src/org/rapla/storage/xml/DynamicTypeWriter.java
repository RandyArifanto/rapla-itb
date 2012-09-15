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
package org.rapla.storage.xml;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.internal.DynamicTypeImpl;
import org.rapla.entities.internal.CategoryImpl;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

/** Stores the data from the local cache in XML-format to a print-writer.*/
public class DynamicTypeWriter extends RaplaXMLWriter
{
    public DynamicTypeWriter(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    private void printStartPattern() throws IOException {
        openElement("relax:start");
        openElement("relax:choice");
        Iterator<?> it =  cache.getCollection(DynamicType.TYPE).iterator();
        while (it.hasNext()) {
            DynamicTypeImpl type = (DynamicTypeImpl) it.next();
            openTag("relax:ref");
            att("name",type.getElementKey());
            closeElementTag();
        }
        closeElement("relax:choice");
        closeElement("relax:start");
    }

    public void printDynamicType(DynamicType type) throws IOException,RaplaException {
        openTag("relax:define");
        att("name",type.getElementKey());
        closeTag();

        openTag("relax:element");
        att("name","dynatt:" + type.getElementKey());
        if (isIdOnly())
        {
            att("id",getId(type));
        }
        printVersion( type);

        closeTag();

        printTranslation(type.getName());
        printAnnotations(type);
        
        Attribute att[] =  type.getAttributes();
        for (int i = 0; i< att.length; i ++) {
            printAttribute(att[i]);
        }
        
        closeElement("relax:element");
        closeElement("relax:define");
    }
    
    public void writeObject(RaplaObject type) throws IOException, RaplaException {
    	printDynamicType( (DynamicType) type );
    }
        


    private String getCategoryPath( Category category) throws EntityNotFoundException {
        Category rootCategory = cache.getSuperCategory();
        if ( category != null && rootCategory.equals( category) )
        {
            return "";
        }
        return ((CategoryImpl) rootCategory ).getPathForCategory(category);
    }

    protected void printAttribute(Attribute attribute) throws IOException,RaplaException {
        if (attribute.isOptional())
            openElement("relax:optional");
        openTag("relax:element");
        att("name",attribute.getKey());
        if (isIdOnly())
            att("id",getId(attribute));

        printVersion( attribute);

        AttributeType type = attribute.getType();
        closeTag();
        printTranslation( attribute.getName() );
        printAnnotations( attribute );
        String[] constraintKeys = attribute.getConstraintKeys();
        openTag("relax:data");
        
        att("type", type.toString());
        closeElementTag();
        for (int i = 0; i<constraintKeys.length; i++ ) {
            String key = constraintKeys[i];
            Object constraint = attribute.getConstraint( key );
            // constraint not set
            if (constraint == null)
                continue;
            openTag("rapla:constraint");
            att("name", key );
            closeTagOnLine();
            if ( constraint instanceof Category) {
                Category category = (Category) constraint;
                if (isIdOnly()) {
                    print( getId(category) );
                } else {
                    print( getCategoryPath( category ) );
                }
            }
            else if ( constraint instanceof Date) {
                final String formatDate = dateTimeFormat.formatDate( (Date) constraint);
                print( formatDate);
            } else {
                print( constraint.toString()); 
            }
            closeElementOnLine("rapla:constraint");
            println();
        }
        Object defaultValue = attribute.defaultValue();
        if ( defaultValue != null)
        {
            openTag("rapla:default");
            closeTagOnLine();
            if ( defaultValue instanceof Category) {
                Category category = (Category) defaultValue;
                if (isIdOnly()) {
                    print( getId(category) );
                } else {
                    print( getCategoryPath( category ) );
                }
            }  else if ( defaultValue instanceof Date) {
                final String formatDate = dateTimeFormat.formatDate( (Date) defaultValue);
                print( formatDate);
            } else {
                print( defaultValue.toString()); 
            }
            closeElementOnLine("rapla:default");
            println();
        }
        closeElement("relax:element");
        if (attribute.isOptional())
            closeElement("relax:optional");
    }
    
    
  
    public void printDynamicTypes()  throws IOException,RaplaException {
        openElement("relax:grammar");
        Iterator<?> it =  cache.getCollection(DynamicType.TYPE).iterator();
        while (it.hasNext()) {
            printDynamicType((DynamicType) it.next());
            println();
        }
        printStartPattern();
        closeElement("relax:grammar");
    }
    

    
    

}



