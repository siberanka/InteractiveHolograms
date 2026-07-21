package com.siberanka.interactiveholograms.nms;

import com.siberanka.interactiveholograms.api.utils.reflect.Version;
import com.siberanka.interactiveholograms.nms.api.InteractiveHologramsNmsException;
import com.siberanka.interactiveholograms.nms.api.NmsAdapter;
import com.siberanka.interactiveholograms.shared.reflect.ReflectUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * This factory is responsible for creating the {@link NmsAdapter} for the current server version.
 *
 * <p>Create an instance of {@link NmsAdapter} via {@link #createNmsAdapter(Version)} to gain access
 * to all (currently implemented) NMS-related methods.</p>
 *
 * @author d0by
 * @see NmsAdapter
 * @since 2.9.0
 */
public class NmsAdapterFactory {

    /**
     * Initialize the {@link NmsAdapter} instance for the current server version.
     *
     * @param version The version for which the {@link NmsAdapter} should be created.
     * @return The {@link NmsAdapter}.
     * @throws InteractiveHologramsNmsException If the current server version is not supported
     *                                     or something goes wrong during the initialization of the service.
     * @since 2.9.0
     */
    public NmsAdapter createNmsAdapter(Version version) {
        Objects.requireNonNull(version, "version cannot be null");

        String nmsAdapterImplementationClassName = "com.siberanka.interactiveholograms.nms." + version.name() + ".NmsAdapterImpl";
        try {
            Class<?> nmsAdapterImplementationClass = ReflectUtil.getClass(nmsAdapterImplementationClassName);
            if (!NmsAdapter.class.isAssignableFrom(nmsAdapterImplementationClass)) {
                throw new InteractiveHologramsNmsException("Nms adapter " + nmsAdapterImplementationClassName + " does not implement "
                        + NmsAdapter.class.getName());
            }
            Constructor<?> constructor = nmsAdapterImplementationClass.getDeclaredConstructor();
            return (NmsAdapter) constructor.newInstance();
        } catch (InteractiveHologramsNmsException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw new InteractiveHologramsNmsException("Unsupported server version: " + version.name());
        } catch (NoSuchMethodException e) {
            throw new InteractiveHologramsNmsException("NmsAdapter implementation is missing the default constructor: "
                    + nmsAdapterImplementationClassName);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new InteractiveHologramsNmsException("Failed to construct a new instance of NmsAdapter implementation: "
                    + nmsAdapterImplementationClassName, e);
        } catch (Exception e) {
            throw new InteractiveHologramsNmsException("Unknown error occurred while initializing NmsAdapter implementation: "
                    + nmsAdapterImplementationClassName, e);
        }
    }

}