package io.vertx.up.rs.config;

import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.exception.up.AccessProxyException;
import io.vertx.exception.up.AddressWrongException;
import io.vertx.exception.up.NoArgConstructorException;
import io.vertx.up.annotations.Address;
import io.vertx.up.ce.Receipt;
import io.vertx.up.rs.Extractor;
import io.vertx.zero.web.ZeroAnno;
import org.vie.fun.HBool;
import org.vie.fun.HNull;
import org.vie.util.Instance;
import org.vie.util.StringUtil;
import org.vie.util.log.Annal;
import org.vie.util.mirror.Anno;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

/**
 * Scanned @Queue clazz to build Receipt metadata
 */
public class ReceiptExtractor implements Extractor<Set<Receipt>> {

    private static final Annal LOGGER = Annal.get(ReceiptExtractor.class);

    private static final Set<String> ADDRESS = new ConcurrentHashSet<>();

    static {
        if (ADDRESS.isEmpty()) {
            /** 1. Get all endpoints **/
            final Set<Class<?>> endpoints = ZeroAnno.getEndpoints();

            /** 2. Scan for @Address to matching **/
            for (final Class<?> endpoint : endpoints) {
                // 3. Scan
                final Annotation[] annotations
                        = Anno.query(endpoint, Address.class);
                // 4. Extract address
                for (final Annotation addressAnno : annotations) {
                    final String address = Instance.invoke(addressAnno, "value");
                    if (!StringUtil.isNil(address)) {
                        ADDRESS.add(address);
                    }
                }
            }
        }
        /** 5.Log out address report **/
        LOGGER.info(Info.ADDRESS_IN, ADDRESS.size());
        ADDRESS.forEach(item -> {
            LOGGER.info(Info.ADDRESS_ITEM, item);
        });
    }

    @Override
    public Set<Receipt> extract(final Class<?> clazz) {
        return HNull.get(clazz, () -> {
            // 1. Class verify
            verify(clazz);
            // 2. Scan method to find @Address
            final Set<Receipt> receipts = new ConcurrentHashSet<>();
            final Method[] methods = clazz.getDeclaredMethods();
            for (final Method method : methods) {
                // 3. Only focus on annotated with @Address
                if (method.isAnnotationPresent(Address.class)) {
                    final Receipt receipt = extract(method);
                    if (null != receipt) {
                        receipts.add(receipt);
                    }
                }
            }
            return receipts;
        }, new ConcurrentHashSet<>());
    }

    private Receipt extract(final Method method) {
        // 1. Scan whole Endpoints
        final Class<?> clazz = method.getDeclaringClass();
        final Annotation annotation = method.getDeclaredAnnotation(Address.class);
        final String address = Instance.invoke(annotation, "value");

        // 2. Ensure address incoming.
        HBool.execUp(!ADDRESS.contains(address), LOGGER,
                AddressWrongException.class,
                getClass(), address, clazz, method);

        final Receipt receipt = new Receipt();
        receipt.setMethod(method);
        receipt.setAddress(address);
        // Fix: Instance class for proxy
        final Object proxy = Instance.singleton(clazz);
        receipt.setProxy(proxy);
        return receipt;
    }

    private void verify(final Class<?> clazz) {
        // Check basic specification: No Arg Constructor
        HBool.execUp(!Instance.noarg(clazz), LOGGER,
                NoArgConstructorException.class,
                getClass(), clazz);
        HBool.execUp(!Modifier.isPublic(clazz.getModifiers()), LOGGER,
                AccessProxyException.class,
                getClass(), clazz);
    }
}