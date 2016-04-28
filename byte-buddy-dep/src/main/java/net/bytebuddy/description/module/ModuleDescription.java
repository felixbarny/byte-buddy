package net.bytebuddy.description.module;

import net.bytebuddy.description.NamedElement;
import net.bytebuddy.utility.JavaType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Module;

public interface ModuleDescription extends NamedElement.WithRuntimeName {

    ModuleDescription UNDEFINED = null;

    boolean isNamed();

    abstract class AbstractBase implements ModuleDescription {

        @Override
        public String getInternalName() {
            return getName();
        }

        @Override
        public String getSourceCodeName() {
            return getName();
        }
    }

    class ForLoadedModule extends ModuleDescription.AbstractBase {

        private static final Dispatcher DISPATCHER = Dispatcher.Enabled.make();

        private final Object module;

        protected ForLoadedModule(Object module) {
            this.module = module;
        }

        public static ModuleDescription of(Object module) {
            if (!JavaType.MODULE.getTypeStub().isInstance(module)) {
                throw new IllegalArgumentException("Not a module instance: " + module);
            }
            return new ForLoadedModule(module);
        }

        @Override
        public boolean isNamed() {
            return DISPATCHER.isNamed(module);
        }

        @Override
        public String getName() {
            return DISPATCHER.getName(module);
        }

        protected interface Dispatcher {

            boolean isNamed(Object module);

            String getName(Object module);

            enum Disabled implements Dispatcher {

                INSTANCE;

                @Override
                public boolean isNamed(Object module) {
                    throw new IllegalStateException("java.lang.reflect.Module is not available on the current VM");
                }

                @Override
                public String getName(Object module) {
                    throw new IllegalStateException("java.lang.reflect.Module is not available on the current VM");
                }
            }

            class Enabled implements Dispatcher {

                private final Method isNamed;

                private final Method getName;

                protected Enabled(Method isNamed, Method getName) {
                    this.isNamed = isNamed;
                    this.getName = getName;
                }

                protected static Dispatcher make() {
                    try {
                        Class<?> module = Class.forName("java.lang.reflect.Module");
                        return new Enabled(module.getDeclaredMethod("isNamed"), module.getDeclaredMethod("getName"));
                    } catch (RuntimeException exception) {
                        throw exception;
                    } catch (Exception ignored) {
                        return Disabled.INSTANCE;
                    }
                }

                @Override
                public boolean isNamed(Object module) {
                    try {
                        return (Boolean) isNamed.invoke(module);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access " + isNamed, exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error while invoking " + isNamed, exception.getCause());
                    }
                }

                @Override
                public String getName(Object module) {
                    try {
                        return (String) getName.invoke(module);
                    } catch (IllegalAccessException exception) {
                        throw new IllegalStateException("Cannot access " + getName, exception);
                    } catch (InvocationTargetException exception) {
                        throw new IllegalStateException("Error while invoking " + getName, exception.getCause());
                    }
                }
            }
        }
    }
}
