package com.jonateam.breakpointAgent;

import javassist.*;
import javassist.bytecode.CodeIterator;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by yonatan.katz on 12/19/2018.
 */
public class LoggerTransformer implements ClassFileTransformer {

    private Set<String> loggerInterfacesNames = new HashSet<>();
    private Set<String> loggerFunctionsNames = new HashSet<>();
    private Set<String> ignoredPackages = new HashSet<>();

    public LoggerTransformer() {

        loggerInterfacesNames.add("org.slf4j.Logger");
        loggerInterfacesNames.add("ch.qos.logback.classic.Logger");
        loggerInterfacesNames.add("java.util.logging.Logger");
        loggerInterfacesNames.add("org.apache.logging.log4j");

        loggerFunctionsNames.add("debug");
        loggerFunctionsNames.add("error");
        loggerFunctionsNames.add("info");
        loggerFunctionsNames.add("trace");
        loggerFunctionsNames.add("warn");

        ignoredPackages.add("java.");
        ignoredPackages.add("javax.");
        ignoredPackages.add("javassist.");
        ignoredPackages.add("sun.");
        ignoredPackages.add("com.jonateam.breakpointAgent.");

    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            className = className.replace('/', '.');
            if (shouldIgnore(className)) {
                return classfileBuffer;
            }
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(className);

            if (!isInjectableClass(cc)) {
                return classfileBuffer;
            }
            for (CtMethod m : cc.getDeclaredMethods()) {
                if (hasMethodBody(m)) {
                    if (isLoggerFunction(cc, m)) {
                        injectLoggerByteCode(cc, m);
                    }
                    else {
                        injectStackByteCode(cc, m);
                    }
                }
            }
            return ctClassToBytes(cc);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return classfileBuffer;
    }

    private byte[] ctClassToBytes(CtClass cc) throws IOException, CannotCompileException {
        byte[] byteCode = cc.toBytecode();
        cc.detach();
        return byteCode;
    }

    private boolean isLoggerFunction(CtClass cc, CtMethod m) throws NotFoundException {
        return hasLoggerIfc(new HashSet<String>(), new CtClass[]{cc}) && loggerFunctionsNames.contains(m.getName());
    }

    private boolean isInjectableClass(CtClass cc) {
        if (cc.isAnnotation() || cc.isArray() || cc.isPrimitive() ||  cc.isFrozen()) {
            return false;
        }
        return true;
    }

    private void injectLoggerByteCode(CtClass cc, CtMethod m) throws IOException, CannotCompileException {
        try {
            m.insertBefore("com.jonateam.breakpointAgent.Breakpoint.breakIt($args);");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void injectStackByteCode(CtClass cc, CtMethod m) throws IOException, CannotCompileException {
        try {
            m.insertBefore("com.jonateam.breakpointAgent.Breakpoint.storeStack($args);");
            m.insertAfter("com.jonateam.breakpointAgent.Breakpoint.freeStack();");
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println(cc.getName() + " : " + m.getName());
        }
    }


    private boolean hasLoggerFunctionImplementation(CtClass cc) {
        for (CtMethod m : cc.getDeclaredMethods()) {
            if (loggerFunctionsNames.contains(m.getName())) {
                if (hasMethodBody(m)) return true;
            }
        }
        return false;
    }

    private boolean hasMethodBody(CtMethod m) {
        try {
            for (CodeIterator ci = m.getMethodInfo().getCodeAttribute().iterator(); ci.hasNext(); ) {
                return true;
            }
        } catch (Throwable t) {

        }
        return false;
    }

    private boolean shouldIgnore(String className) {
        for (String ignoredPackage : ignoredPackages) {
            if (className.startsWith(ignoredPackage)) {
                return true;
            }
        }
        return false;

    }

    private boolean hasLoggerIfc(Set<String> visitedInterfaces, CtClass[] interfaces) throws NotFoundException {
        for (CtClass ifc : interfaces) {
            if (visitedInterfaces.contains(ifc.getName())) {
                continue;
            }
            if (loggerInterfacesNames.contains(ifc.getName())) {
                return true;
            }
            else {
                visitedInterfaces.add(ifc.getName());
                CtClass[] subIfc = ifc.getInterfaces();
                if (subIfc != null && subIfc.length > 0) {
                    if (hasLoggerIfc(visitedInterfaces, subIfc)) {
                        return true;
                    }
                }
                CtClass superclass = ifc.getSuperclass();
                if (superclass == null || superclass.getName().equals("java.lang.Object")) {
                    continue;
                }
                if (hasLoggerIfc(visitedInterfaces, new CtClass[]{superclass})) {
                    return true;
                }
            }
        }
        return false;
    }


}