package com.kaiy;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Created by kaiy_baby on 2019/10/23
 *
 * @description:
 * @author: kaiy_baby
 * @date: Created in 2019/10/23 14:28
 * @version:
 */
public class Test {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext();
		context.register(AppConfig.class);
//		context.addBeanFactoryPostProcessor(new MyBeanFactoryPostProcessor());
//		context.addBeanFactoryPostProcessor(new MyBeanDefinitionRegistryPostProcessor());
//		context.register(TestService.class);
		context.refresh();
		IndexSercvice bean = (IndexSercvice) context.getBean("indexSercvice");
		System.out.println(context.getBean(KaiyService.class));
		System.out.println(context.getBean(AppConfig.class));
		System.out.println(bean.kaiyService);
		System.out.println(bean);
//		System.out.println(bean.select());

//		System.out.println(context.getBean("kaiyMapper"));
//		HashMap<Object, Object> objectObjectHashMap = new HashMap<>();
//		objectObjectHashMap.put("","");
//		System.out.println(bean);
	}
}
