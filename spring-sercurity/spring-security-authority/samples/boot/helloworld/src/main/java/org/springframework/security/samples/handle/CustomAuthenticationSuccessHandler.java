package org.springframework.security.samples.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

	private  final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
		logger.info("自定义成功处理器");
		logger.info("自定义成功处理器");
		logger.info("getRedirectStrategy"+getRedirectStrategy());
		logger.info("getDefaultTargetUrl"+getDefaultTargetUrl());
		logger.info("getTargetUrlParameter"+getTargetUrlParameter());
		super.onAuthenticationSuccess(request, response, authentication);
	}
}
