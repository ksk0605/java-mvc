package com.interface21.webmvc.servlet.mvc.tobe;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.interface21.context.stereotype.Controller;
import com.interface21.web.bind.annotation.RequestMapping;
import com.interface21.web.bind.annotation.RequestMethod;
import com.interface21.webmvc.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AnnotationHandlerMapping {

	private static final Logger log = LoggerFactory.getLogger(AnnotationHandlerMapping.class);

	private final Object[] basePackage;
	private final Map<HandlerKey, HandlerExecution> handlerExecutions;

	public AnnotationHandlerMapping(final Object... basePackage) {
		this.basePackage = basePackage;
		this.handlerExecutions = new HashMap<>();
	}

	public void initialize() throws Exception {
		Reflections reflections = new Reflections(basePackage);
		Set<Class<?>> controllerClasses = reflections.getTypesAnnotatedWith(Controller.class);

		for (Class<?> controllerClass : controllerClasses) {
			registerHandlerMethods(controllerClass);
		}

		log.info("Initialized AnnotationHandlerMapping with controllers!");
	}

	private void registerHandlerMethods(Class<?> controllerClass) throws Exception {
		for (Method method : controllerClass.getDeclaredMethods()) {
			RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
			if (requestMapping != null) {
				registerHandlerMethod(controllerClass, method, requestMapping);
			}
		}
	}

	private void registerHandlerMethod(Class<?> controllerClass, Method method, RequestMapping requestMapping) throws Exception {
		String url = requestMapping.value();
		RequestMethod[] httpMethods = requestMapping.method();

		for (RequestMethod requestMethod : httpMethods) {
			handlerExecutions.put(new HandlerKey(url, requestMethod), createHandlerExecution(controllerClass, method));
		}
	}

	private HandlerExecution createHandlerExecution(Class<?> controllerClass, Method method) {
		return new HandlerExecution() {
			@Override
			public ModelAndView handle(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
				Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
				return (ModelAndView)method.invoke(controllerInstance, request, response);
			}
		};
	}

	public Object getHandler(final HttpServletRequest request) {
		String url = request.getRequestURI();
		RequestMethod requestMethod = RequestMethod.from(request.getMethod());

		HandlerKey handlerKey = new HandlerKey(url, requestMethod);
		return handlerExecutions.get(handlerKey);
	}
}
