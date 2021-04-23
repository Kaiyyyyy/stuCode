package com.kaiy;

import com.test.TestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by kaiy_baby on 2019/10/23
 *
 * @description:
 * @author: kaiy_baby
 * @date: Created in 2019/10/23 14:33
 * @version:
 */
@Configuration
@ComponentScan("com.kaiy")
//@MapperScan("com.kaiy")
public class AppConfig {

	@Bean
	public TestService testService(){
		return new TestService();
	}

//	@Bean
//	public SqlSessionFactoryBean sqlSessionFactory(DataSource dataSource){
//		SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
//		factoryBean.setDataSource(dataSource);
//		return factoryBean;
//	}

//	@Bean
//	public DataSource dataSource(){
//		DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
//		driverManagerDataSource.setUsername("root");
//		driverManagerDataSource.setPassword("kaiy123456.");
//        driverManagerDataSource.setUrl("jdbc:mysql://server.kaiy.vip:3306/kaiy_sql_test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai");
//        driverManagerDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
//        return driverManagerDataSource;
//	}
}
