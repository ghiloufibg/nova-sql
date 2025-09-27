package com.novasql.ui.util;

import javafx.scene.control.TreeItem;

/**
 * Enhanced TreeItem for schema browser with additional metadata.
 */
public class SchemaTreeItem extends TreeItem<String> {

    public enum ItemType {
        DATABASE, TABLES_FOLDER, VIEWS_FOLDER, INDEXES_FOLDER,
        TABLE, VIEW, INDEX, COLUMN, PRIMARY_KEY, FOREIGN_KEY
    }

    private final ItemType itemType;
    private final String fullName;
    private final String description;
    private int recordCount = -1;

    public SchemaTreeItem(String displayName, ItemType itemType) {
        this(displayName, itemType, displayName, null);
    }

    public SchemaTreeItem(String displayName, ItemType itemType, String fullName, String description) {
        super(displayName);
        this.itemType = itemType;
        this.fullName = fullName;
        this.description = description;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDescription() {
        return description;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
        updateDisplayText();
    }

    private void updateDisplayText() {
        String baseText = getFullName();

        if (itemType == ItemType.TABLE && recordCount >= 0) {
            setValue(baseText + " (" + recordCount + " rows)");
        } else if (description != null && !description.isEmpty()) {
            setValue(baseText + " - " + description);
        } else {
            setValue(baseText);
        }
    }

    public boolean isTable() {
        return itemType == ItemType.TABLE;
    }

    public boolean isColumn() {
        return itemType == ItemType.COLUMN;
    }

    public boolean isIndex() {
        return itemType == ItemType.INDEX;
    }
}