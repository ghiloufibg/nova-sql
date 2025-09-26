package com.novasql.index;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BTree {
    private static final Logger logger = LoggerFactory.getLogger(BTree.class);
    private static final int DEFAULT_ORDER = 5;

    private final int order;
    private BTreeNode root;

    public BTree() {
        this(DEFAULT_ORDER);
    }

    public BTree(int order) {
        this.order = order;
        this.root = new BTreeNode(order, true);
    }

    public Integer search(String key) {
        if (key == null) {
            return null;
        }

        Integer result = root.search(key);
        logger.debug("Search for key '{}': {}", key, result != null ? "found" : "not found");
        return result;
    }

    public void insert(String key, Integer value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value cannot be null");
        }

        insertInternal(root, key, value);
        logger.debug("Inserted key '{}' with value {}", key, value);
    }

    private void insertInternal(BTreeNode node, String key, Integer value) {
        if (node.isLeaf()) {
            node.insertKey(key, value);

            if (node.isFull()) {
                splitNode(node);
            }
        } else {
            int childIndex = node.findChildIndex(key);
            BTreeNode child = node.getChild(childIndex);
            insertInternal(child, key, value);

            if (child.isFull()) {
                splitNode(child);
            }
        }
    }

    private void splitNode(BTreeNode node) {
        if (node == root) {
            // Create new root
            BTreeNode newRoot = new BTreeNode(order, false);
            root = newRoot;
            node.setParent(newRoot);
            newRoot.getChildren().add(node);
        }

        BTreeNode newNode = node.split();
        String middleKey = node.getMiddleKey();

        BTreeNode parent = node.getParent();
        parent.insertKey(middleKey, null);

        // Find position to insert new child
        int pos = 0;
        while (pos < parent.getKeys().size() - 1 &&
               parent.getKey(pos).compareTo(middleKey) < 0) {
            pos++;
        }

        parent.insertChild(pos + 1, newNode);

        if (parent.isFull() && parent != root) {
            splitNode(parent);
        }
    }

    public boolean delete(String key) {
        if (key == null) {
            return false;
        }

        boolean deleted = deleteInternal(root, key);
        if (deleted) {
            logger.debug("Deleted key '{}'", key);
        }
        return deleted;
    }

    private boolean deleteInternal(BTreeNode node, String key) {
        int keyIndex = -1;
        for (int i = 0; i < node.getKeys().size(); i++) {
            if (node.getKey(i).equals(key)) {
                keyIndex = i;
                break;
            }
        }

        if (keyIndex >= 0) {
            // Key found in current node
            if (node.isLeaf()) {
                node.getKeys().remove(keyIndex);
                node.getValues().remove(keyIndex);
                return true;
            } else {
                // Internal node deletion (simplified)
                node.getKeys().remove(keyIndex);
                return true;
            }
        } else if (!node.isLeaf()) {
            // Key not found, search in child
            int childIndex = node.findChildIndex(key);
            return deleteInternal(node.getChild(childIndex), key);
        }

        return false;
    }

    public int getHeight() {
        return getHeightInternal(root);
    }

    private int getHeightInternal(BTreeNode node) {
        if (node.isLeaf()) {
            return 1;
        } else {
            return 1 + getHeightInternal(node.getChild(0));
        }
    }

    public int getOrder() {
        return order;
    }

    public BTreeNode getRoot() {
        return root;
    }
}