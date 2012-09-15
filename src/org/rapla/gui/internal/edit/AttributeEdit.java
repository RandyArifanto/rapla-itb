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
package org.rapla.gui.internal.edit;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.components.calendar.DateChangeEvent;
import org.rapla.components.calendar.DateChangeListener;
import org.rapla.components.calendar.RaplaCalendar;
import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.layout.TableLayout;
import org.rapla.entities.Category;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeAnnotations;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.EmptyLineBorder;
import org.rapla.gui.toolkit.RaplaWidget;

/**
 *  @author Christopher Kohlhaas
 */
public class AttributeEdit extends RaplaGUIComponent
    implements
    RaplaWidget
{
    RaplaListEdit listEdit;
    DynamicType dt;
    DefaultConstraints constraintPanel;
    ArrayList<ChangeListener> listenerList = new ArrayList<ChangeListener>();

    Listener listener = new Listener();
    DefaultListModel model = new DefaultListModel();
    boolean editKeys;

    public AttributeEdit(RaplaContext sm) throws RaplaException {
        super( sm);
        constraintPanel = new DefaultConstraints(sm);
        listEdit = new RaplaListEdit( getI18n(), constraintPanel.getComponent(), listener );
        listEdit.setListDimension( new Dimension( 200,220 ) );

        constraintPanel.addChangeListener( listener );

        listEdit.getComponent().setBorder( BorderFactory.createTitledBorder( new EmptyLineBorder(),getString("attributes")) );
        listEdit.getList().setCellRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

                public Component getListCellRendererComponent(JList list,
                                                              Object value,
                                                              int index,
                                                              boolean isSelected,
                                                              boolean cellHasFocus) {
                    Attribute a = (Attribute) value;
                    value = a.getName(getRaplaLocale().getLocale());
                    if (editKeys) {
                        value = "{" + a.getKey() + "} " + value;
                    }
                    value = (index + 1) +") " + value;
                    return super.getListCellRendererComponent(list,value,index,isSelected,cellHasFocus);
               }
            });
        constraintPanel.setEditKeys( false );
    }

    public RaplaWidget getConstraintPanel() {
        return constraintPanel;
    }

    class Listener implements ActionListener,ChangeListener {
        public void actionPerformed(ActionEvent evt) {
            int index = getSelectedIndex();
            try {
                if (evt.getActionCommand().equals("remove")) {
                    removeAttribute();
                } else if (evt.getActionCommand().equals("new")) {
                    createAttribute();
                } else if (evt.getActionCommand().equals("edit")) {
                    constraintPanel.mapFrom( listEdit.getList().getSelectedValue() );
                } else if (evt.getActionCommand().equals("moveUp")) {
                    dt.exchangeAttributes(index, index -1);
                    updateModel();
                } else if (evt.getActionCommand().equals("moveDown")) {
                    dt.exchangeAttributes(index, index + 1);
                    updateModel();
                }

            } catch (RaplaException ex) {
                showException(ex, getComponent());
            }
        }
        public void stateChanged(ChangeEvent e) {
            try {
                confirmEdits();
                fireContentChanged();
            } catch (RaplaException ex) {
                showException(ex, getComponent());
            }
        }
    }

    public JComponent getComponent() {
        return listEdit.getComponent();
    }

    public int getSelectedIndex() {
        return listEdit.getList().getSelectedIndex();
    }

    public void setDynamicType(DynamicType dt) throws RaplaException {
        this.dt = dt;
        updateModel();
    }

    private void updateModel() throws RaplaException {
        Object selectedItem = listEdit.getList().getSelectedValue();
        model.clear();
        Attribute[] attributes = dt.getAttributes();;
        for (int i = 0; i < attributes.length; i++ ) {
            model.addElement( attributes[i] );
        }
        listEdit.getList().setModel(model);
        if ( listEdit.getList().getSelectedValue() != selectedItem )
            listEdit.getList().setSelectedValue(selectedItem, true );
    }

    public void confirmEdits() throws RaplaException {
        if ( getSelectedIndex() < 0 )
            return;
        Object attribute = listEdit.getList().getSelectedValue();
        constraintPanel.mapTo ( attribute );
        model.set( model.indexOf( attribute ), attribute );
    }

    public void setEditKeys(boolean editKeys) {
        constraintPanel.setEditKeys(editKeys);
        this.editKeys = editKeys;
    }

    private String createNewKey() {
        Attribute[] atts = dt.getAttributes();
        int max = 1;
        for (int i=0;i<atts.length;i++) {
            String key = atts[i].getKey();
            if (key.length()>1
                && key.charAt(0) =='a'
                && Character.isDigit(key.charAt(1))
                )
                {
                    try {
                        int value = Integer.valueOf(key.substring(1)).intValue();
                        if (value >= max)
                            max = value + 1;
                    } catch (NumberFormatException ex) {
                    }
                }
        }
        return "a" + (max);
    }

    void removeAttribute() throws RaplaException {
        int index = getSelectedIndex();
        Attribute att = dt.getAttributes() [index];
        dt.removeAttribute(att);
        updateModel();
    }

    void createAttribute() throws RaplaException {
        confirmEdits();
        AttributeType type = AttributeType.STRING;
        Attribute att = (Attribute) getModification().newAttribute(type);
        att.setKey(createNewKey());
        dt.addAttribute(att);
        updateModel();
        listEdit.getList().setSelectedIndex( dt.getAttributes().length -1 );
    }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(listener);
    }

    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[])listenerList.toArray(new ChangeListener[]{});
    }

    protected void fireContentChanged() {
        if (listenerList.size() == 0)
            return;
        ChangeEvent evt = new ChangeEvent(this);
        ChangeListener[] listeners = getChangeListeners();
        for (int i = 0;i<listeners.length; i++) {
            listeners[i].stateChanged(evt);
        }
    }

}

class DefaultConstraints extends AbstractEditField
    implements
        ActionListener
        ,ChangeListener
{
    JPanel panel = new JPanel();
    JLabel nameLabel = new JLabel();
    JLabel keyLabel = new JLabel();
    JLabel typeLabel = new JLabel();
    JLabel categoryLabel = new JLabel();
    JLabel tabLabel = new JLabel();
    JLabel expectedColumnsLabel = new JLabel();
    JLabel expectedRowsLabel = new JLabel();
    AttributeType types[] = {
        AttributeType.BOOLEAN
        ,AttributeType.STRING
        ,AttributeType.INT
        ,AttributeType.CATEGORY
        ,AttributeType.DATE
    };

    String tabs[] = {
            AttributeAnnotations.VALUE_MAIN_VIEW
            ,AttributeAnnotations.VALUE_ADDITIONAL_VIEW
            ,AttributeAnnotations.VALUE_NO_VIEW
    };

    boolean mapping = false;
    MultiLanguageField name ;
    TextField key;
    JComboBox classSelect = new JComboBox();
    CategorySelectField categorySelect;
    CategorySelectField defaultSelectCategory;
    TextField defaultSelectText;
    BooleanField defaultSelectBoolean;
    RaplaNumber defaultSelectNumber = new RaplaNumber(new Long(0),null,null, false);
    RaplaCalendar defaultSelectDate ;
     
    RaplaNumber expectedRows = new RaplaNumber(new Long(1),new Long(1),null, false);
    RaplaNumber expectedColumns = new RaplaNumber(new Long(1),new Long(1),null, false);
    JComboBox tabSelect = new JComboBox();

    Category rootCategory;

    DefaultConstraints(RaplaContext sm) throws RaplaException  {
        super( sm );
        key = new TextField(sm,"key");
        name = new MultiLanguageField(sm,"name");
        rootCategory = getQuery().getSuperCategory();

        categorySelect = new CategorySelectField(sm,"choose_root_category"
                                                 ,rootCategory);
        categorySelect.setUseNullCategory(false);
        defaultSelectCategory = new CategorySelectField(sm,"default"
                ,rootCategory);
        defaultSelectText = new TextField(sm,"default");
        defaultSelectBoolean = new BooleanField(sm, "default");
        defaultSelectDate = createRaplaCalendar();
        defaultSelectDate.setNullValuePossible( true);
        defaultSelectDate.setDate( null);
        double fill = TableLayout.FILL;
        double pre = TableLayout.PREFERRED;
        panel.setLayout( new TableLayout( new double[][]
            {{5, pre, 5, fill },  // Columns
             {5, pre ,5, pre, 5, pre, 5, pre, 5, pre, 5, pre, 5,pre, 5}} // Rows
                                          ));
        panel.add("1,1,l,f", nameLabel);
        panel.add("3,1,f,f", name.getComponent() );
        panel.add("1,3,l,f", keyLabel);
        panel.add("3,3,f,f", key.getComponent() );
        panel.add("1,5,l,f", typeLabel);
        panel.add("3,5,l,f", classSelect);
        panel.add("1,7,l,t", categoryLabel);
        panel.add("3,7,l,t", categorySelect.getComponent());
        panel.add("1,7,l,t", expectedRowsLabel);
        panel.add("3,7,l,t", expectedRows);
        panel.add("1,9,l,t", expectedColumnsLabel);
        panel.add("3,9,l,t", expectedColumns);
        panel.add("1,11,l,t", new JLabel(getString("default")));
        panel.add("3,11,l,t", defaultSelectCategory.getComponent());
        panel.add("3,11,l,t", defaultSelectText.getComponent());
        panel.add("3,11,l,t", defaultSelectBoolean.getComponent());
        panel.add("3,11,l,t", defaultSelectDate);
        panel.add("3,11,l,t", defaultSelectNumber);
        panel.add("1,13,l,t", tabLabel);
        panel.add("3,13,l,t", tabSelect);


        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for ( int i = 0; i < types.length; i++ ) {
            model.addElement(getString("type." + types[i]));
        }
        classSelect.setModel( model );

        model = new DefaultComboBoxModel();
        for ( int i = 0; i < tabs.length; i++ ) {
            model.addElement(getString(tabs[i]));
        }
        tabSelect.setModel( model );

        nameLabel.setText(getString("name") + ":");
        keyLabel.setText(getString("key") +" *"+ ":");
        typeLabel.setText(getString("type") + ":");
        categoryLabel.setText(getString("root") + ":");
        expectedRowsLabel.setText(getString("expected_rows") + ":");
        expectedColumnsLabel.setText(getString("expected_columns") + ":");
        tabLabel.setText(getString("edit-view") + ":");

        categorySelect.addChangeListener ( this );
        categorySelect.addChangeListener( new ChangeListener() {
            
            public void stateChanged(ChangeEvent e) 
            {
                final Category rootCategory = (Category)categorySelect.getValue();
                defaultSelectCategory.setRootCategory( rootCategory );
                defaultSelectCategory.setValue( null);
                defaultSelectCategory.getComponent().setEnabled( rootCategory != null);
            }
        }
        
        );
        name.addChangeListener ( this );
        key.addChangeListener ( this );
        classSelect.addActionListener ( this );
        tabSelect.addActionListener( this);
        expectedRows.addChangeListener( this );
        expectedColumns.addChangeListener( this );
        defaultSelectCategory.addChangeListener( this );
        defaultSelectText.addChangeListener( this );
        defaultSelectBoolean.addChangeListener( this );
        defaultSelectNumber.addChangeListener( this );
        defaultSelectDate.addDateChangeListener( new DateChangeListener() {
            
            public void dateChanged(DateChangeEvent evt) 
            {
                stateChanged(null);
            }
        });
    }

    public void setEditKeys(boolean editKeys) {
        keyLabel.setVisible( editKeys );
        key.getComponent().setVisible( editKeys );
    }

    public JComponent getComponent() {
        return panel;
    }

    public Object getValue() {
        return null;
    }

    public void setValue(Object object) {
    }

    public void mapFrom(Object object) throws RaplaException {
        try {
            mapping = true;
            Attribute attribute = (Attribute) object;
            name.mapFrom(attribute);
            key.mapFrom(attribute);
            final AttributeType attributeType = attribute.getType();
            classSelect.setSelectedItem(getString("type." + attributeType));
            if (attributeType.equals(AttributeType.CATEGORY)) {
                final Category rootCategory = (Category)attribute.getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
                categorySelect.setValue( rootCategory );
                defaultSelectCategory.setRootCategory( rootCategory);
                defaultSelectCategory.setValue( attribute.convertValue(attribute.defaultValue()));
                defaultSelectCategory.getComponent().setEnabled( rootCategory != null);
            }
            else if (attributeType.equals(AttributeType.STRING)) 
            {
                defaultSelectText.setValue( attribute.defaultValue());
            }
            else if (attributeType.equals(AttributeType.BOOLEAN)) 
            {
                defaultSelectBoolean.setValue( attribute.defaultValue());
            }
            else if (attributeType.equals(AttributeType.INT)) 
            {
                defaultSelectNumber.setNumber( (Number)attribute.defaultValue());
            }
            else if (attributeType.equals(AttributeType.DATE)) 
            {
                defaultSelectDate.setDate( (Date)attribute.defaultValue());
            }
            Long rows = new Long(attribute.getAnnotation(AttributeAnnotations.KEY_EXPECTED_ROWS, "1"));
            expectedRows.setNumber( rows );
            Long columns = new Long(attribute.getAnnotation(AttributeAnnotations.KEY_EXPECTED_COLUMNS, String.valueOf(TextField.DEFAULT_LENGTH)));
            expectedColumns.setNumber( columns );
            
            String selectedTab = attribute.getAnnotation(AttributeAnnotations.KEY_EDIT_VIEW, AttributeAnnotations.VALUE_MAIN_VIEW);
            tabSelect.setSelectedItem(getString(selectedTab));
            update();
        } finally {
            mapping = false;
        }
    }

    public void mapTo(Object object) throws RaplaException {
        Attribute attribute = (Attribute) object;
        name.mapTo( attribute );
        key.mapTo( attribute );
        AttributeType type = types[classSelect.getSelectedIndex()];
        attribute.setType( type );
        
        if ( type.equals(AttributeType.CATEGORY)) {
            Object defaultValue = defaultSelectCategory.getValue();
            Object rootCategory = categorySelect.getValue();
            if ( rootCategory == null)
            {
                rootCategory = this.rootCategory;
                defaultValue = null;
            }
            attribute.setConstraint(ConstraintIds.KEY_ROOT_CATEGORY, rootCategory );
            attribute.setDefaultValue( defaultValue);
        } else {
            attribute.setConstraint(ConstraintIds.KEY_ROOT_CATEGORY, null);
        }
        
        if ( type.equals(AttributeType.BOOLEAN)) {
            final Object defaultValue = defaultSelectBoolean.getValue();
            attribute.setDefaultValue( defaultValue);
        } 
        
        if ( type.equals(AttributeType.INT)) {
            final Object defaultValue = defaultSelectNumber.getNumber();
            attribute.setDefaultValue( defaultValue);
        }
        
        if ( type.equals(AttributeType.DATE)) {
            final Object defaultValue = defaultSelectDate.getDate();
            attribute.setDefaultValue( defaultValue);
        }
        
        if (type.equals(AttributeType.STRING)) {
            Long size = (Long) expectedRows.getNumber();
            String newRows = null;
            if ( size != null && size.longValue() > 1)
                newRows = size.toString();

            size = (Long) expectedColumns.getNumber();
            String newColumns = null;
            if ( size != null && size.longValue() > 1)
                newColumns = size.toString();
            Object defaultValue = defaultSelectText.getValue();
            if ( defaultValue != null && defaultValue.toString().length() == 0)
            {
            	defaultValue = null;
            }
            attribute.setDefaultValue( defaultValue);
            attribute.setAnnotation(AttributeAnnotations.KEY_EXPECTED_ROWS ,  newRows);
            attribute.setAnnotation(AttributeAnnotations.KEY_EXPECTED_COLUMNS,  newColumns);
        } else {
            attribute.setAnnotation(AttributeAnnotations.KEY_EXPECTED_ROWS,  null);
            attribute.setAnnotation(AttributeAnnotations.KEY_EXPECTED_COLUMNS,  null);
        }

        String selectedTab = tabs[tabSelect.getSelectedIndex()];
        if ( selectedTab != null && !selectedTab.equals(AttributeAnnotations.VALUE_MAIN_VIEW)) {
            attribute.setAnnotation(AttributeAnnotations.KEY_EDIT_VIEW,  selectedTab);
        } else {
            attribute.setAnnotation(AttributeAnnotations.KEY_EDIT_VIEW,  null);
        }
    }

    private void update() {
        AttributeType type = types[classSelect.getSelectedIndex()];
        boolean categoryVisible = type.equals(AttributeType.CATEGORY);
        final boolean textVisible = type.equals(AttributeType.STRING);
        final boolean booleanVisible = type.equals(AttributeType.BOOLEAN);
        final boolean numberVisible = type.equals(AttributeType.INT);
        final boolean dateVisible  = type.equals(AttributeType.DATE);
        boolean expectedRowsVisible = textVisible;
        boolean expectedColumnsVisible = textVisible;
        categoryLabel.setVisible( categoryVisible );
        categorySelect.getComponent().setVisible( categoryVisible );
        expectedRowsLabel.setVisible( expectedRowsVisible );
        expectedRows.setVisible( expectedRowsVisible );
        expectedColumnsLabel.setVisible( expectedColumnsVisible );
        expectedColumns.setVisible( expectedColumnsVisible );
        defaultSelectCategory.getComponent().setVisible( categoryVisible);
        defaultSelectText.getComponent().setVisible( textVisible);
        defaultSelectBoolean.getComponent().setVisible( booleanVisible);
        defaultSelectNumber.setVisible( numberVisible);
        defaultSelectDate.setVisible( dateVisible);
    }

    public void actionPerformed(ActionEvent evt) {
        if (mapping)
            return;
        if ( evt.getSource() == classSelect) {
            AttributeType newType = types[classSelect.getSelectedIndex()];
            if (newType.equals(AttributeType.CATEGORY)) {
                categorySelect.setValue( rootCategory );
            }
        }
        fireContentChanged();
        update();
    }

    public void stateChanged(ChangeEvent e) {
        if (mapping)
            return;

        fireContentChanged();
    }

}

