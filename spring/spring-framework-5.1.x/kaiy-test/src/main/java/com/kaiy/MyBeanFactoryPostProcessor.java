package com.kaiy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Created by kaiy_baby on 2020/4/29
 *
 * @description:
 * @author: kaiy_baby
 * @date: Created in 2020/4/29 15:00
 * @version:
 */
public class MyBeanFactoryPostProcessor implements BeanFactoryPostProcessor  {
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

	}
}
