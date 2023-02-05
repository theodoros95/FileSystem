package com.jetbrains.filesystem.utils;

import java.io.*;

public class ObjectSerializer {

    public static byte[] serializeObject(final Object object) throws IOException {

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();
             final ObjectOutputStream oos = new ObjectOutputStream(bos)) {

            oos.writeObject(object);
            oos.flush();

            return bos.toByteArray();
        }
    }

    public static <T> T deserializeObject(final byte[] bytes, Class<T> classType) throws Exception {

        try (final ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             final ObjectInputStream ois = new ObjectInputStream(bis)) {

            return classType.cast(ois.readObject());
        }
    }
}
