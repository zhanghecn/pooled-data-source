package com.github.zhanghecn.sql.datasource;

import com.zhanghe.pool.core.connection.PoolEntity;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.NameMatchMethodPointcutAdvisor;

import java.sql.Connection;

/**
 * 连接代理
 * @author: ZhangHe
 * @since: 2020/10/28 9:26
 */
public abstract class ConnectionProxyDecorate {

    public static Connection proxyConnection(PoolEntity<Connection> poolEntity){
        ProxyFactory proxyFactory = new ProxyFactory(poolEntity.getConnection());
        proxyFactory.addAdvisor(closeInterceptor(poolEntity));
        return (Connection) proxyFactory.getProxy();
    }


    public static PointcutAdvisor closeInterceptor(PoolEntity<Connection> poolEntity){
        //关闭方法改成回收
        MethodInterceptor closeInterceptor = (invocation)->{
            poolEntity.recycle(System.currentTimeMillis());
            return null;
        };

        NameMatchMethodPointcutAdvisor pointcutAdvisor = new NameMatchMethodPointcutAdvisor();
        pointcutAdvisor.setMappedName("close");
        pointcutAdvisor.setAdvice(closeInterceptor);
        return pointcutAdvisor;
    }
}
