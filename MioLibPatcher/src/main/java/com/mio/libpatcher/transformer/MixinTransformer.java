package com.mio.libpatcher.transformer;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;

public class MixinTransformer implements BaseTransformer {
    @Override
    public String getTargetClassName() {
        return "org.spongepowered.asm.mixin.transformer.MixinConfig";
    }

    @Override
    public void transform(CtClass clazz) throws Throwable {
        for (CtMethod method : clazz.getDeclaredMethods()) {
            if (method.getName().equals("prepareMixins")) {
                method.instrument(new ExprEditor() {
                    @Override
                    public void edit(NewExpr e) throws CannotCompileException {
                        if (e.getClassName().equals("org.spongepowered.asm.mixin.transformer.MixinInfo")) {
                            e.replace("$_ = com.mio.libpatcher.transformer.MixinTransformer.createMixinInfo($1, $2, $3, $4, $5);");
                        }
                    }

                    @Override
                    public void edit(MethodCall m) throws CannotCompileException {
                        if (m.getMethodName().equals("shouldPrepare")) {
                            m.replace("$_ = ($0 != null) && $proceed($$);");
                        }
                    }
                });
            }
        }
    }
}
