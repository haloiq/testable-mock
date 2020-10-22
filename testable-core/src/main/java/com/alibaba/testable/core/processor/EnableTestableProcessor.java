package com.alibaba.testable.core.processor;

import com.alibaba.testable.core.annotation.EnableTestable;
import com.alibaba.testable.core.constant.ConstPool;
import com.alibaba.testable.core.model.TestableContext;
import com.alibaba.testable.core.translator.EnableTestableTranslator;
import com.alibaba.testable.core.util.TestableLogger;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import java.util.Set;

/**
 * @author flin
 */
@SupportedAnnotationTypes("com.alibaba.testable.core.annotation.EnableTestable")
public class EnableTestableProcessor extends AbstractProcessor {

    private TestableContext cx;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        Context context = getJavacProcessingContext(processingEnv);
        if (context == null) {
            cx = new TestableContext(new TestableLogger(processingEnv.getMessager()), processingEnv.getFiler());
            cx.logger.info("Skip testable compile time processing");
        } else {
            cx = new TestableContext(new TestableLogger(processingEnv.getMessager()), processingEnv.getFiler(),
                processingEnv.getElementUtils(), processingEnv.getTypeUtils(), JavacTrees.instance(processingEnv),
                TreeMaker.instance(context), Names.instance(context));
        }
        cx.logger.info("Testable processor initialized");
    }

    private Context getJavacProcessingContext(ProcessingEnvironment processingEnv) {
        try {
            return ((JavacProcessingEnvironment)processingEnv).getContext();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (cx.names == null) {
            return true;
        }
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(EnableTestable.class);
        for (Element element : elements) {
            if (element.getKind().isClass() && isTestClass(element.getSimpleName())) {
                processClassElement((Symbol.ClassSymbol)element);
            }
        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // always return the latest version
        return SourceVersion.values()[SourceVersion.values().length - 1];
    }

    private boolean isTestClass(Name name) {
        return name.toString().endsWith(ConstPool.TEST_POSTFIX);
    }

    private void processClassElement(Symbol.ClassSymbol clazz) {
        JCTree tree = cx.trees.getTree(clazz);
        String pkgName = ((Symbol.PackageSymbol)clazz.owner).fullname.toString();
        tree.accept(new EnableTestableTranslator(pkgName, clazz.getSimpleName().toString(), cx));
    }

}