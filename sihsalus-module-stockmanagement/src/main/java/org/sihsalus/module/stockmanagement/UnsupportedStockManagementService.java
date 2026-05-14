package org.sihsalus.module.stockmanagement;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class UnsupportedStockManagementService implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        String methodName = method.getName();
        if ("onStartup".equals(methodName) || "onShutdown".equals(methodName)) {
            return null;
        }
        if ("toString".equals(methodName)) {
            return "Static StockManagementService boundary";
        }
        if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
        }
        if ("equals".equals(methodName)) {
            return proxy == args[0];
        }
        throw new UnsupportedOperationException(
                "Stock Management service operations require the Hibernate 7 DAO migration");
    }
}
