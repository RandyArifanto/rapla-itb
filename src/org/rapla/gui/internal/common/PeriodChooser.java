/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas, Bettina Lademann                |
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
package org.rapla.gui.internal.common;

import java.awt.Component;
import java.util.Date;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.domain.Period;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.PeriodModel;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

public class PeriodChooser extends JComboBox
 {
    private static final long serialVersionUID = 1L;
    
    Date selectedDate = null;
    Period selectedPeriod = null;

    public static int START_ONLY = 1;
    public static int START_AND_END = 0;
    public static int END_ONLY = -1;

    int visiblePeriods;
    I18nBundle i18n;
    PeriodModel periodModel;
    private boolean listenersEnabled = true;
    private boolean isWeekOfPeriodVisible = true;

    public PeriodChooser( RaplaContext sm) throws RaplaException {
        this(sm,START_AND_END);
    }

    public PeriodChooser(RaplaContext sm,int visiblePeriods) throws RaplaException {
        //      super(RaplaButton.SMALL);
        this.visiblePeriods = visiblePeriods;
        i18n = (I18nBundle) sm.lookup(I18nBundle.ROLE + "/org.rapla.RaplaResources");
        setPeriodModel(( (ClientFacade) sm.lookup(ClientFacade.ROLE) ).getPeriodModel());
    }


    public void setPeriodModel(PeriodModel model) {
        this.periodModel = model;
        if ( periodModel != null ) {
            try {
                listenersEnabled = false;
                this.setModel(new DefaultComboBoxModel(model.getAllPeriods()));
            } finally {
                listenersEnabled = true;
            }
        }
        this.setRenderer(new PeriodListCellRenderer());
        update();
    }


    private String formatPeriod(Period period) {
        if ( !isWeekOfPeriodVisible)
        {
            return period.getName();
        }

        int lastWeek = period.getWeeks();
        int week = period.weekOf(selectedDate);
        if (week != 1 && week >= lastWeek) {
            return i18n.format(
                               "period.format.end"
                               ,period.getName()
                               );
        } else {
            return i18n.format(
                              "period.format.week"
                              ,String.valueOf(period.weekOf(selectedDate))
                              ,period.getName()
                              );
        }
    }

    private String formatPeriodList(Period period) {
        if (visiblePeriods == START_ONLY) {
            return i18n.format(
                               "period.format.start"
                               ,period.getName()
                               );
        } else if (visiblePeriods == END_ONLY) {
            return i18n.format(
                               "period.format.end"
                               ,period.getName()
                               );
        } else {
              return period.getName();
        }
    }

     public void setDate(Date date, Date endDate) {
        try {
            listenersEnabled = false;
            
            if (date != selectedDate) // Compute period only on date change 
            {
                selectedPeriod = getPeriod(date, endDate);
            }
            
            if ( selectedPeriod != null ) 
            {
                selectedDate = date;
                setSelectedItem(selectedPeriod);
            } 
            else 
            {
                selectedDate = date;
                setSelectedItem(null);
            }
            repaint();
            revalidate();
        } finally {
            listenersEnabled = true;
        }
    }

     public void setDate(Date date) {
	 setDate(date, null);
    }

    private String getSelectionText() {
        Period period = selectedPeriod;
        if ( period != null ) {
            return formatPeriod(period);
        } else {
            return i18n.getString("period.not_set");
        }
    }

    public void setSelectedPeriod(Period period) {
        selectedPeriod = period; // EXCO
        listenersEnabled = false;
        setSelectedItem(period);
        listenersEnabled = true;
        if (visiblePeriods == END_ONLY) {
            selectedDate = period.getEnd();
        } else {
            selectedDate = period.getStart();
        }
    }


    public Period getPeriod() {
        return selectedPeriod; // getPeriod(selectedDate);
    }

    private Period getPeriod(Date date, Date endDate) {
    	if (periodModel == null )
    		return null;
        if ( visiblePeriods == END_ONLY) {
            return periodModel.getNearestPeriodForEndDate(date);
        } else {
            return periodModel.getNearestPeriodForStartDate(date, endDate);
        }
    }

    public Date getDate() {
        return selectedDate;
    }

    private void update() {
        setVisible(periodModel != null && periodModel.getSize() > 0);
        setDate(getDate());
    }

    protected void fireActionEvent() {
        if ( !listenersEnabled )
        {
            return ;
        }
        Period period = (Period) getSelectedItem();
        selectedPeriod = period; // EXCO
        if (period != null) 
        {
            if (visiblePeriods == END_ONLY) {
                selectedDate = period.getEnd();
            } else {
                selectedDate = period.getStart();
            }
        }
        super.fireActionEvent();
    }


    class PeriodListCellRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        public Component getListCellRendererComponent(
                                                      JList list,
                                                      Object value,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus)
        {
            if (index == -1) {
                value = getSelectionText();
            } else {
                Period period = (Period) value;
                value = formatPeriodList(period);
            }
            return super.getListCellRendererComponent(list,
                                                      value,
                                                      index,
                                                      isSelected,
                                                      cellHasFocus);
        }
    }


    public boolean isWeekOfPeriodVisible()
    {
        return isWeekOfPeriodVisible;
    }

    public void setWeekOfPeriodVisible( boolean isWeekOfPeriodVisible )
    {
        this.isWeekOfPeriodVisible = isWeekOfPeriodVisible;
    }

}
