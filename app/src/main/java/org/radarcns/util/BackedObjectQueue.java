package org.radarcns.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A queue-like object queue that is backed by a file storage.
 * @param <T> type of objects to store.
 */
public class BackedObjectQueue<T> implements Closeable {
    private final Converter<T> converter;
    private final QueueFile queueFile;

    /**
     * Creates a new object queue from given file.
     * @param queueFile file to write objects to
     * @param converter way to convert from and to given objects
     */
    public BackedObjectQueue(QueueFile queueFile, Converter<T> converter) {
        this.queueFile = queueFile;
        this.converter = converter;
    }

    /** Number of elements in the queue. */
    public int size() {
        return this.queueFile.size();
    }

    /**
     * Add a new element to the queue.
     * @param entry element to add
     * @throws IOException if the backing file cannot be accessed or the element cannot be converted.
     * @throws IllegalStateException if the queue is full.
     */
    public void add(T entry) throws IOException {
        try (QueueFile.QueueFileOutputStream out = queueFile.elementOutputStream()) {
            converter.serialize(entry, out);
        }
    }

    /**
     * Add a collection of new element to the queue.
     * @param entries elements to add
     * @throws IOException if the backing file cannot be accessed or the element cannot be converted.
     * @throws IllegalStateException if the queue is full.
     */
    public void addAll(Collection<? extends T> entries) throws IOException {
        try (QueueFile.QueueFileOutputStream out = queueFile.elementOutputStream()) {
            for (T entry : entries) {
                converter.serialize(entry, out);
                out.nextElement();
            }
        }
    }

    /**
     * Get the front-most object in the queue. This does not remove the element.
     * @return front-most element or null if none is available
     * @throws IOException if the element could not be read or deserialized
     */
    public T peek() throws IOException {
        try (InputStream in = queueFile.peek()) {
            return converter.deserialize(in);
        }
    }

    /**
     * Get at most {@code n} front-most objects in the queue. This does not remove the elements.
     * @param n number of elements to retrieve
     * @return list of elements, with at most {@code n} elements.
     * @throws IOException if the element could not be read or deserialized
     */
    public List<T> peek(int n) throws IOException {
        Iterator<InputStream> iter = queueFile.iterator();
        List<T> results = new ArrayList<>(n);
        for (int i = 0; i < n && iter.hasNext(); i++) {
            try (InputStream in = iter.next()) {
                results.add(converter.deserialize(in));
            }
        }
        return results;
    }

    /** Remove the first {@code n} elements from the queue. */
    public void remove(int n) throws IOException {
        queueFile.remove(n);
    }

    /** Returns {@code true} if this queue contains no entries. */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Close the queue. This also closes the backing file.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        queueFile.close();
    }

    public interface Converter<T> {
        /**
         * Deserialize an object from given offset of given bytes
         */
        T deserialize(InputStream in) throws IOException;
        /**
         * Serialize an object to given offset of given bytes.
         */
        void serialize(T value, OutputStream out) throws IOException;
    }
}
