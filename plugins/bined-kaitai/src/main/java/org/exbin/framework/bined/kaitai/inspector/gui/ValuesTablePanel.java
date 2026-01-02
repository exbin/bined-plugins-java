/*
 * Copyright (C) ExBin Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.exbin.framework.bined.kaitai.inspector.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.bined.capability.CaretCapable;
import org.exbin.bined.swing.CodeAreaCore;
import org.exbin.framework.App;
import org.exbin.framework.bined.kaitai.inspector.api.ValueRowItem;
import org.exbin.framework.bined.objectdata.property.gui.PropertyTableCellEditor;
import org.exbin.framework.bined.objectdata.property.gui.PropertyTableCellRenderer;
import org.exbin.framework.bined.objectdata.property.gui.PropertyTableItem;
import org.exbin.framework.bined.objectdata.property.gui.PropertyTableModel;
import org.exbin.framework.language.api.LanguageModuleApi;

/**
 * Panel for table with values for Kaitai inspection.
 *
 * @author ExBin Project (https://exbin.org)
 */
@ParametersAreNonnullByDefault
public class ValuesTablePanel extends javax.swing.JPanel {

    protected final ResourceBundle resourceBundle = App.getModule(LanguageModuleApi.class).getBundle(ValuesTablePanel.class);

    protected static final int DATA_LIMIT = 250;
    protected final byte[] values = new byte[DATA_LIMIT];

    protected final PropertyTableModel tableModel;
    protected final PropertyTableCellRenderer valueCellRenderer;
    protected final TableCellRenderer nameCellRenderer;
    protected final PropertyTableCellEditor valueCellEditor;

    protected CodeAreaCore codeArea;

    public ValuesTablePanel() {
        tableModel = new PropertyTableModel();

        initComponents();

        TableColumnModel columns = valuesTable.getColumnModel();
        columns.getColumn(0).setPreferredWidth(80);
        columns.getColumn(1).setPreferredWidth(80);
        columns.getColumn(0).setWidth(80);
        columns.getColumn(1).setWidth(80);
        nameCellRenderer = new DefaultTableCellRenderer() {
            @Nonnull
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JComponent component = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                PropertyTableItem tableItem = ((PropertyTableModel) table.getModel()).getRow(row);
                component.setToolTipText("(" + tableItem.getTypeName() + ") " + tableItem.getValueName());
                return component;
            }
        };
        columns.getColumn(0).setCellRenderer(nameCellRenderer);
        valueCellRenderer = new PropertyTableCellRenderer();
        columns.getColumn(1).setCellRenderer(valueCellRenderer);
        valueCellEditor = new PropertyTableCellEditor();
        columns.getColumn(1).setCellEditor(valueCellEditor);

        valuesTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Nonnull
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (component instanceof JComponent) {
                    ((JComponent) component).setBorder(noFocusBorder);
                }

                return component;
            }
        });
    }

    @Nonnull
    public List<ValueRowItem> getValueRows() {
        List<ValueRowItem> rowItems = new ArrayList<>();
        int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            rowItems.add((ValueRowItem) tableModel.getRow(i));
        }
        return rowItems;
    }

    public void setValueRows(List<ValueRowItem> rowItems) {
        tableModel.removeAll();
        for (ValueRowItem rowItem : rowItems) {
            tableModel.addRow(rowItem);
        }
        notifyChanged();
    }

    public void setCodeArea(CodeAreaCore codeArea) {
        this.codeArea = codeArea;
        notifyChanged();
    }

    public void notifyChanged() {
        if (codeArea == null) {
            return;
        }

        BinaryData contentData = codeArea.getContentData();
        long dataSize = codeArea.getDataSize();
        long dataPosition = ((CaretCapable) codeArea).getDataPosition();
        long available = dataSize - dataPosition;

        if (valuesTable.isEditing()) {
            valuesTable.getCellEditor().cancelCellEditing();
        }

        int valuesAvailable = Math.min((int) available, DATA_LIMIT);
        contentData.copyToArray(dataPosition, values, 0, valuesAvailable);
        List<PropertyTableItem> items = tableModel.getItems();
        for (PropertyTableItem item : items) {
            ((ValueRowItem) item).updateRow(values, valuesAvailable);
        }
        tableModel.fireTableRowsUpdated(0, tableModel.getRowCount() - 1);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainScrollPane = new javax.swing.JScrollPane();
        valuesTable = new javax.swing.JTable();

        setName("Form"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        mainScrollPane.setName("mainScrollPane"); // NOI18N

        valuesTable.setModel(tableModel);
        valuesTable.setName("valuesTable"); // NOI18N
        valuesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        mainScrollPane.setViewportView(valuesTable);

        add(mainScrollPane, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane mainScrollPane;
    private javax.swing.JTable valuesTable;
    // End of variables declaration//GEN-END:variables

}
