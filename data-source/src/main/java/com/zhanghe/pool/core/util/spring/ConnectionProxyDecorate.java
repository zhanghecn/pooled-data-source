package com.zhanghe.pool.core.util.spring;

import com.zhanghe.pool.core.connection.PoolEntity;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;

/**
 * 连接代理
 *
 * @author: ZhangHe
 * @since: 2020/10/28 9:26
 */
public abstract class ConnectionProxyDecorate {

    public static Object proxyConnection(PoolEntity poolEntity) {
        return proxyConnection(poolEntity, false, "close",null);
    }

    public static Object proxyConnection(PoolEntity poolEntity, boolean isCglib, String closeMethodName,Object returnVal) {
        ProxyFactory proxyFactory = new ProxyFactory(poolEntity.getConnection());
        proxyFactory.addAdvisor(closeInterceptor(poolEntity, closeMethodName,returnVal));
        proxyFactory.setProxyTargetClass(isCglib);
        return proxyFactory.getProxy();
    }

    public static PointcutAdvisor closeInterceptor(PoolEntity poolEntity, String closeMethodName,Object returnVal) {
        //关闭方法改成回收
        MethodInterceptor closeInterceptor = (invocation) -> {
            poolEntity.recycle(System.currentTimeMillis());
            return returnVal;
        };

        NameMatchMethodPointcutAdvisor pointcutAdvisor = new NameMatchMethodPointcutAdvisor();
        pointcutAdvisor.setMappedName(closeMethodName);
        pointcutAdvisor.setAdvice(closeInterceptor);
        return pointcutAdvisor;
    }
}
