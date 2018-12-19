package com.jonateam.breakpointAgent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.CodeIterator;

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
        ignoredPackages.add("javassist.");
        ignoredPackages.add("sun.");
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
            if (hasLoggerIfc(new HashSet<String>(), cc.getInterfaces()) && hasLoggerFunctionImplementaion(cc))
            {
                System.out.println(className);
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        if (1==1) return classfileBuffer;
        if (loggerInterfacesNames.contains(className)) {
            try {
//                m.addLocalVariable("elapsedTime", CtClass.longType);
//                m.insertBefore("elapsedTime = System.currentTimeMillis();");
//                m.insertAfter("{elapsedTime = System.currentTimeMillis() - elapsedTime;"
//                        + "System.out.println(\"Method Executed in ms: \" + elapsedTime);}");
//                byte[] byteCode = cc.toBytecode();
//                cc.detach();
//                return byteCode;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


        //System.out.println("transformer: " + className);
        return classfileBuffer;
    }

    private boolean hasLoggerFunctionImplementaion(CtClass cc) {
        if (cc.isAnnotation() || cc.isArray() || cc.isInterface()  ||
                cc.isPrimitive() || cc.isEnum() || cc.isFrozen()) {
            return false;
        }
        for (CtMethod m : cc.getDeclaredMethods()) {
            if (loggerFunctionsNames.contains(m.getName())) {
                try {
                    for (CodeIterator ci = m.getMethodInfo().getCodeAttribute().iterator(); ci.hasNext(); ) {
                        return true;
                    }
                } catch (Throwable t) {

                }
            }
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
            }
        }
        return false;
    }


}