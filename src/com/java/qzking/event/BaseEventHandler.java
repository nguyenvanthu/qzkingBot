package com.java.qzking.event;

import java.lang.reflect.Method;

import com.smartfoxserver.v2.exceptions.SFSException;

import sfs2x.client.core.BaseEvent;
import sfs2x.client.core.IEventListener;

public class BaseEventHandler implements IEventListener {
	private Object handler;
	private String methodName;
	private Method targetMethod;

	public BaseEventHandler(Object handler, String methodName) {
		this.handler = handler;
		this.methodName = methodName;
	}

	@Override
	public void dispatch(BaseEvent event) throws SFSException {
//		if (targetMethod == null) {
//			try {
//				targetMethod = this.handler.getClass().getMethod(methodName, BaseEvent.class);
//				targetMethod.invoke(handler, event);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//		}
		if (this.handler == null) {
			return ;
		}
		if (targetMethod == null) {
			synchronized (this) {
				if (targetMethod == null) {
					Method[] methods = this.handler.getClass().getMethods();
					for (Method method : methods) {
						if (method.getName().equals(this.methodName) && method.getParameterCount() == 1
								&& BaseEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
							targetMethod = method;
							break;
						}
					}
					if (targetMethod == null) {
						return;
					}
				}
			}
		}
		try {
			targetMethod.invoke(handler, event);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
