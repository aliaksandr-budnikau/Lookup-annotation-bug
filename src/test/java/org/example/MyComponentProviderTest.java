package org.example;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class MyComponentProviderTest {

    private AnnotationConfigApplicationContext context;

    @Before
    public void setUp() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.setCacheBeanMetadata(false); // <---------- If it is false @lookup can't create proxy !

        context = new AnnotationConfigApplicationContext(beanFactory);
        context.scan("org.example");
        context.refresh();
    }

    /**
     * 1)
     * public abstract class AbstractBeanFactory extends FactoryBeanRegistrySupport implements ConfigurableBeanFactory {
     *     ...
     *     protected RootBeanDefinition getMergedBeanDefinition(
     *         ...
     *         if (containingBd == null && isCacheBeanMetadata()) {
     * 				this.mergedBeanDefinitions.put(beanName, mbd); <---------- is always empty
     *         }   
     *         ...   
     *     }
     *     ...
     * }
     * 
     * 2) Then here
     * public class AutowiredAnnotationBeanPostProcessor extends InstantiationAwareBeanPostProcessorAdapter
     * 		implements MergedBeanDefinitionPostProcessor, PriorityOrdered, BeanFactoryAware {
     * 	   ...
     * 	   public Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, final String beanName)
     * 			throws BeanCreationException {
     * 		   ...
     * 		   ReflectionUtils.doWithLocalMethods(targetClass, method -> {
     * 							Lookup lookup = method.getAnnotation(Lookup.class);
     * 							if (lookup != null) {
     * 								Assert.state(this.beanFactory != null, "No BeanFactory available");
     * 								LookupOverride override = new LookupOverride(method, lookup.value());
     * 								try {
     * 									RootBeanDefinition mbd = (RootBeanDefinition)
     * 											this.beanFactory.getMergedBeanDefinition(beanName); <---------- always returns a new object even for the same "beanName"
     * 									mbd.getMethodOverrides().addOverride(override); <---------- and we lose this "override"
     *                                                                }
     * 								catch (NoSuchBeanDefinitionException ex) {
     * 									throw new BeanCreationException(beanName,
     * 											"Cannot apply @Lookup to beans without corresponding bean definition");
     *                                }* 							}
     * 						});
     * 		   ...
     *     }
     *     ...
     * }
     * 
     * 3) as a result
     * public class SimpleInstantiationStrategy implements InstantiationStrategy {
     *     ...
     *     public Object instantiate(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
     *         ...
     *         if (!bd.hasMethodOverrides()) { <---------- will be true
     * 		       ...
     *         }
     * 		   else { <---------- will not generate CGLIB subclass
     * 			   // Must generate CGLIB subclass.
     * 			   return instantiateWithMethodInjection(bd, beanName, owner);
     *        }* 	
     *     }
     *     ...
     * }
     */
    @Test
    public void testThatLookupDoesWork() {
        MyComponentProvider provider = context.getBean(MyComponentProvider.class);

        assertNotEquals(MyComponentProvider.class, provider.getClass());
        assertNotNull(provider.get());
    }
}