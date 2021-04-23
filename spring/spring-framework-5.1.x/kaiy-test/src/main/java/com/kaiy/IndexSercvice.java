package com.kaiy;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by kaiy_baby on 2019/10/23
 *
 * @description:
 * @author: kaiy_baby
 * @date: Created in 2019/10/23 14:33
 * @version:
 */
@org.springframework.stereotype.Service
public class IndexSercvice implements Service{

//	@Autowired
//	KaiyMapper kaiyMapper;

	@Autowired
	KaiyService kaiyService;
//
//	public List<Map> select(){
//		return  kaiyMapper.selectInfo();
//	}

}
