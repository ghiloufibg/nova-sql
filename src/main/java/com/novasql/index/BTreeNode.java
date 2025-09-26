package com.novasql.index;

import java.util.ArrayList;
import java.util.List;

public class BTreeNode {
    private final int order;
    private final List<String> keys;
    private final List<Integer> values; // Page IDs for leaf nodes
    private final List<BTreeNode> children;
    private boolean isLeaf;
    private BTreeNode parent;

    public BTreeNode(int order, boolean isLeaf) {
        this.order = order;
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.values = new ArrayList<>();
        this.children = new ArrayList<>();
        this.parent = null;
    }

    public boolean isFull() {
        return keys.size() >= order - 1;
    }

    public boolean isUnderflow() {
        return keys.size() < (order - 1) / 2;
    }

    public void insertKey(String key, Integer value) {
        int pos = 0;
        while (pos < keys.size() && keys.get(pos).compareTo(key) < 0) {
            pos++;
        }

        keys.add(pos, key);
        if (isLeaf) {
            values.add(pos, value);
        }
    }

    public void insertChild(int index, BTreeNode child) {
        children.add(index, child);
        child.parent = this;
    }

    public String getKey(int index) {
        return keys.get(index);
    }

    public Integer getValue(int index) {
        return values.get(index);
    }

    public BTreeNode getChild(int index) {
        return children.get(index);
    }

    public int findChildIndex(String key) {
        int index = 0;
        while (index < keys.size() && key.compareTo(keys.get(index)) > 0) {
            index++;
        }
        return index;
    }

    public Integer search(String key) {
        int index = 0;
        while (index < keys.size() && key.compareTo(keys.get(index)) > 0) {
            index++;
        }

        if (index < keys.size() && key.equals(keys.get(index))) {
            return isLeaf ? values.get(index) : null;
        }

        if (isLeaf) {
            return null;
        } else {
            return children.get(index).search(key);
        }
    }

    public BTreeNode split() {
        int mid = keys.size() / 2;
        BTreeNode newNode = new BTreeNode(order, isLeaf);

        // Move half of keys to new node
        for (int i = mid + 1; i < keys.size(); i++) {
            newNode.keys.add(keys.get(i));
            if (isLeaf) {
                newNode.values.add(values.get(i));
            }
        }

        // Move children if not leaf
        if (!isLeaf) {
            for (int i = mid + 1; i <= children.size() - 1; i++) {
                newNode.children.add(children.get(i));
                children.get(i).parent = newNode;
            }
        }

        // Remove moved elements from current node
        keys.subList(mid + (isLeaf ? 0 : 1), keys.size()).clear();
        if (isLeaf) {
            values.subList(mid, values.size()).clear();
        } else {
            children.subList(mid + 1, children.size()).clear();
        }

        return newNode;
    }

    public String getMiddleKey() {
        return keys.get(keys.size() / 2);
    }

    public List<String> getKeys() {
        return keys;
    }

    public List<Integer> getValues() {
        return values;
    }

    public List<BTreeNode> getChildren() {
        return children;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setLeaf(boolean leaf) {
        isLeaf = leaf;
    }

    public BTreeNode getParent() {
        return parent;
    }

    public void setParent(BTreeNode parent) {
        this.parent = parent;
    }
}