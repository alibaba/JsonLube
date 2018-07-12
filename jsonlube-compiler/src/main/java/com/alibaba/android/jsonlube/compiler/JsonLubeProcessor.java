/*
 * Copyright 2018 Alibaba Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.android.jsonlube.compiler;

import com.alibaba.android.jsonlube.FromJson;
import com.alibaba.android.jsonlube.ToJson;
import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by shangjie on 2018/2/5.
 */
@AutoService(Processor.class)
public class JsonLubeProcessor extends AbstractProcessor {
    private ParserClassGenerator mParserGenerator;
    private JsonSerializerGenerator mSerializerGenerator;
    private ClassFileWriter mClassWriter;
    private Filer mProcessFiler;
    private Elements mElementUtils;
    private Types mTypeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mClassWriter = new ClassFileWriter();
        mProcessFiler = processingEnv.getFiler();
        mElementUtils = processingEnv.getElementUtils();
        mTypeUtils = processingEnv.getTypeUtils();
        mParserGenerator = new ParserClassGenerator();
        mParserGenerator.init(mClassWriter, mElementUtils, mTypeUtils);
        mSerializerGenerator = new JsonSerializerGenerator();
        mSerializerGenerator.init(mClassWriter, mElementUtils, mTypeUtils);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(FromJson.class)) {

            if (element.getKind() != ElementKind.CLASS) {
                return true;
            }

            TypeElement typeElement = (TypeElement) element;
            mParserGenerator.generateClass(typeElement);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(ToJson.class)) {

            if (element.getKind() != ElementKind.CLASS) {
                return true;
            }

            TypeElement typeElement = (TypeElement) element;
            mSerializerGenerator.generateClass(typeElement);
        }

        mClassWriter.flush(mProcessFiler);
        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
        annotations.add(FromJson.class);
        annotations.add(ToJson.class);
        return annotations;
    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
