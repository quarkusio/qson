package io.quarkus.qson;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on a public member method to denote a transformation to a specific class.
 *
 * This is useful for scenarios where you have a thirdparty class that you cannot modify
 * that cannot be mapped using standard Qson. For example, a class that does not have an
 * empty parameter constructor or does not use standard get/set bean pattern
 *
 * Example:
 * <pre>
 * public class Thirdparty {
 *     public Thirdparty(int x, String y) {
 *
 *     }
 * }
 *
 * public class MyTransformer {
 *     private int x;
 *     private String y;
 *
 *     &#64;QsonTransformer
 *     public Thirdparty createThirdparty() {
 *         return new Thirdparty(x, y);
 *     }
 *
 *
 *     public int getX() { return x; }
 *     public void setX(int x) { this.x = x; }
 *
 *     public String getY() { return y; }
 *     public void setX(String y) { this.y = y; }
 * }
 *
 * QsonMapper mapper = new QsonMapper();
 * mapper.overrideMappingFor(Thirdparty.class).transformer(MyTransformer.class);
 * </pre>
 *
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface QsonTransformer {
}
