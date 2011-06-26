package org.piax.trans;
import java.util.HashMap;

public class Literals {
     public static <S, T> MapBuilder<S, T> map(S key, T value) {
         return new MapBuilder<S, T>().map(key, value);
     }
 
     public static class MapBuilder<S, T> extends HashMap<S, T> {
         public MapBuilder() {
         }
 
         public MapBuilder<S, T> map(S key, T value) {
             put(key, value);
             return this;
         }
     }
}