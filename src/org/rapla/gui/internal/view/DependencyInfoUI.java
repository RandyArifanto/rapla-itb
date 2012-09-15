
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
package org.rapla.gui.internal.view;

import java.util.Iterator;

import org.rapla.entities.DependencyException;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

class DependencyInfoUI extends HTMLInfo {
    public DependencyInfoUI(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    protected String createHTMLAndFillLinks(Object object,LinkController controller) throws RaplaException{
        DependencyException ex = (DependencyException) object;
        StringBuffer buf = new StringBuffer();
        buf.append(getString("error.dependencies")+":");
        buf.append("<br>");
        Iterator<String> it = ex.getDependencies().iterator();
        int i = 0;
        while (it.hasNext()) {
            Object obj = it.next();
            buf.append((++i));
            buf.append(") ");
            buf.append( obj );
            buf.append("<br>");
            if (i == 30 && it.hasNext()) { //BJO
                buf.append("... " + (ex.getDependencies().size() - 30) + " more"); //BJO
                break;
            }
        }
        return buf.toString();
    }
    
    protected String getTitle(Object object, LinkController controller) {
        return getString("info") + ": " + getString("error.dependencies");
    }
}














