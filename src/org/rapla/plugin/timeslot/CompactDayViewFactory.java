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
package org.rapla.plugin.timeslot;

import javax.swing.Icon;

import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.SwingCalendarView;
import org.rapla.gui.ViewFactory;
import org.rapla.gui.images.Images;
import org.rapla.servletpages.RaplaPageGenerator;

public class CompactDayViewFactory extends RaplaComponent implements ViewFactory
{
    public final static String DAY_TIMESLOT = "day_timeslot";

    public CompactDayViewFactory( RaplaContext context ) throws RaplaException
    {
        super( context );
    }

    public SwingCalendarView createSwingView(RaplaContext context, CalendarModel model, boolean editable) throws RaplaException
    {
        return new SwingCompactDayCalendar( context, model, editable);
    }

    public RaplaPageGenerator createHTMLView(RaplaContext context, CalendarModel model) throws RaplaException
    {
        return new HTMLCompactDayViewPage( context, model);
    }

    public String getViewId()
    {
        return DAY_TIMESLOT;
    }

    public String getName()
    {
        return getString(DAY_TIMESLOT);
    }

    Icon icon;
    public Icon getIcon()
    {
        if ( icon == null) {
            icon = Images.getIcon("/org/rapla/plugin/compactweekview/images/week_compact.png");
        }
        return icon;
    }

    public String getMenuSortKey() {
        return "B2";
    }


}

