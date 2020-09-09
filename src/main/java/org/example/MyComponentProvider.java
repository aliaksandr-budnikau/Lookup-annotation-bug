package org.example;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;

@Component
public class MyComponentProvider {

    @Lookup // the class will never be proxied if the bean factory has cacheBeanMetadata = false 
    public MyComponent get() {
        return null;
    }
}
