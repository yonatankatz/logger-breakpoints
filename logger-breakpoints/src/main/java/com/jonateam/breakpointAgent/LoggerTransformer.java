package com.jonateam.breakpointAgent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

/**
 * Created by yonatan.katz on 12/19/2018.
 */
public class LoggerTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className.startsWith("com.")) {
            // Javassist
            try {
                ClassPool cp = ClassPool.getDefault();
                CtClass cc = cp.get(className);
                CtMethod m = cc.getDeclaredMethod("run");
                m.addLocalVariable("elapsedTime", CtClass.longType);
                m.insertBefore("elapsedTime = System.currentTimeMillis();");
                m.insertAfter("{elapsedTime = System.currentTimeMillis() - elapsedTime;"
                        + "System.out.println(\"Method Executed in ms: \" + elapsedTime);}");
                byte[] byteCode = cc.toBytecode();
                cc.detach();
                return byteCode;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }


        //System.out.println("transformer: " + className);
        return classfileBuffer;
    }


}