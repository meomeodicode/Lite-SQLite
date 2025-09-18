package lite.sqlite.server.storage;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Generic doubly linked list with O(1) operations for LRU cache
 */
public class DoublyLinkedList<T> implements Iterable<T> {
    
    /**
     * Node class for doubly linked list
     */
    public static class Node<T> {
        T data;
        Node<T> next;
        Node<T> prev;
        
        public Node(T data) {
            this.data = data;
            this.next = null;
            this.prev = null;
        }
        
        public Node() {
            this(null); // For sentinel nodes
        }
        
        // Getters and setters
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
        public Node<T> getNext() { return next; }
        public void setNext(Node<T> next) { this.next = next; }
        public Node<T> getPrev() { return prev; }
        public void setPrev(Node<T> prev) { this.prev = prev; }
        
        @Override
        public String toString() {
            return "Node{data=" + data + "}";
        }
    }
    
    private final Node<T> head; // Sentinel head (oldest/front)
    private final Node<T> tail; // Sentinel tail (newest/back)
    private int size;
    
    /**
     * Create empty doubly linked list with sentinel nodes
     */
    public DoublyLinkedList() {
        this.head = new Node<>(); // Dummy head
        this.tail = new Node<>(); // Dummy tail
        this.size = 0;
        
        // Connect sentinels
        head.next = tail;
        tail.prev = head;
    }
    
    /**
     * Add element to front (after head) - O(1)
     * @param data Element to add
     * @return The created node for external reference
     */
    public Node<T> addFirst(T data) {
        Node<T> newNode = new Node<>(data);
        addNodeAfter(head, newNode);
        return newNode;
    }
    
    public Node<T> addLast(T data) {
        Node<T> newNode = new Node<>(data);
        addNodeBefore(tail, newNode);
        return newNode;
    }
    
    /**
     * Remove first element (after head) - O(1)
     * @return The removed element, or null if empty
     */
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        Node<T> firstNode = head.next;
        removeNode(firstNode);
        return firstNode.data;
    }
    
    /**
     * Remove last element (before tail) - O(1)
     * @return The removed element, or null if empty
     */
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        Node<T> lastNode = tail.prev;
        removeNode(lastNode);
        return lastNode.data;
    }
    
    /**
     * Remove specific node from list - O(1)
     * @param node Node to remove
     * @return The data from removed node
     */
    public T removeNode(Node<T> node) {
        if (node == null || node == head || node == tail) {
            throw new IllegalArgumentException("Cannot remove null or sentinel node");
        }
        
        Node<T> prevNode = node.prev;
        Node<T> nextNode = node.next;
        
        prevNode.next = nextNode;
        nextNode.prev = prevNode;
        
        // Clear node references
        node.next = null;
        node.prev = null;
        
        size--;
        return node.data;
    }
    
    /**
     * Move existing node to front (after head) - O(1)
     * @param node Node to move
     */
    public void moveToFirst(Node<T> node) {
        if (node == null || node == head || node == tail) {
            return;
        }
        
        // Remove from current position
        removeNode(node);
        // Add after head
        addNodeAfter(head, node);
    }
    
    public void moveToLast(Node<T> node) {
        if (node == null || node == head || node == tail) {
            return;
        }
        
        Node<T> prevNode = node.prev;
        Node<T> nextNode = node.next;
        prevNode.next = nextNode;
        nextNode.prev = prevNode;

        Node<T> curNode = node;
        removeNode(node);
        // Add before tail
        addNodeBefore(tail, curNode);
    }
    
    /**
     * Get first element without removing - O(1)
     * @return First element or null if empty
     */
    public T peekFirst() {
        return isEmpty() ? null : head.next.data;
    }
    
    /**
     * Get last element without removing - O(1)  
     * @return Last element or null if empty
     */
    public T peekLast() {
        return isEmpty() ? null : tail.prev.data;
    }
    
    /**
     * Get first node - O(1)
     * @return First node or null if empty
     */
    public Node<T> getFirstNode() {
        return isEmpty() ? null : head.next;
    }
    
    /**
     * Get last node - O(1)
     * @return Last node or null if empty  
     */
    public Node<T> getLastNode() {
        return isEmpty() ? null : tail.prev;
    }
    
    /**
     * Check if list is empty
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Get size of list
     */
    public int size() {
        return size;
    }
    
    /**
     * Clear all elements
     */
    public void clear() {
        head.next = tail;
        tail.prev = head;
        size = 0;
    }
    
    // ===========================================
    // HELPER METHODS
    // ===========================================
    
    /**
     * Add node after specified node - O(1)
     */
    private void addNodeAfter(Node<T> prevNode, Node<T> newNode) {
        Node<T> nextNode = prevNode.next;
        
        prevNode.next = newNode;
        newNode.prev = prevNode;
        newNode.next = nextNode;
        nextNode.prev = newNode;
        
        size++;
    }
    
    /**
     * Add node before specified node - O(1)  
     */
    private void addNodeBefore(Node<T> nextNode, Node<T> newNode) {
        Node<T> prevNode = nextNode.prev;
        
        prevNode.next = newNode;
        newNode.prev = prevNode;
        newNode.next = nextNode;
        nextNode.prev = newNode;
        
        size++;
    }
    
    // ===========================================
    // ITERATOR SUPPORT
    // ===========================================
    
    @Override
    public Iterator<T> iterator() {
        return new DoublyLinkedListIterator();
    }
    
    private class DoublyLinkedListIterator implements Iterator<T> {
        private Node<T> current = head.next; // Start after head
        
        @Override
        public boolean hasNext() {
            return current != tail;
        }
        
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            T data = current.data;
            current = current.next;
            return data;
        }
    }
    
    // ===========================================
    // DEBUG AND UTILITY METHODS
    // ===========================================
    
    /**
     * Print list from front to back
     */
    public void printForward() {
        System.out.print("Forward: ");
        Node<T> current = head.next;
        while (current != tail) {
            System.out.print(current.data + " <-> ");
            current = current.next;
        }
        System.out.println("null");
    }
    
    public void printBackward() {
        System.out.print("Backward: ");
        Node<T> current = tail.prev;
        while (current != head) {
            System.out.print(current.data + " <-> ");
            current = current.prev;
        }
        System.out.println("null");
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (T item : this) {
            sb.append(item).append(", ");
        }
        if (size > 0) {
            sb.setLength(sb.length() - 2); // Remove last ", "
        }
        sb.append("]");
        return sb.toString();
    }
}