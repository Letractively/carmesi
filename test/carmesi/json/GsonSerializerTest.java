/* Licensed under the Apache License, Version 2.0 (http://www.apache.org/licenses/LICENSE-2.0) */

package carmesi.json;

import carmesi.json.GsonSerializer;
import com.google.gson.GsonBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author Victor Hugo Herrera Maldonado
 */
public class GsonSerializerTest {

    @Test
    public void shouldBeSameObjectFromSerialization() {
        GsonSerializer serializer=new GsonSerializer();
        A a=new A("x", 9, new C("y"));
        String jsonString = serializer.serialize(a);
        A a2=new GsonBuilder().create().fromJson(jsonString, A.class);
        assertThat(a2.name, is(a.name));
        assertThat(a2.age, is(a.age));
        System.out.println("json: "+jsonString);
    }
    
    private static class A{
        private String name;
        private int age;
        private C c;

        public A() {
        }

        public A(String name, int age, C c) {
            this.name = name;
            this.age = age;
            this.c = c;
        }
        
    }
    
    private static class C{
        private String name;

        public C() {
        }
        
        public C(String name) {
            this.name = name;
        }
        
    }

}