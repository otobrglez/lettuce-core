/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.apigenerator;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.type.Type;

import io.lettuce.core.internal.LettuceSets;

/**
 * Create sync API based on the templates.
 *
 * @author Mark Paluch
 */
public class CreateSyncNodeSelectionClusterApi {

    private Set<String> FILTER_METHODS = LettuceSets.unmodifiableSet("shutdown", "debugOom", "debugSegfault", "digest",
            "close", "isOpen", "BaseRedisCommands.reset", "readOnly", "readWrite", "dispatch", "setAutoFlushCommands", "flushCommands");

    /**
     * Mutate type comment.
     *
     * @return
     */
    Function<String, String> commentMutator() {
        return s -> s.replaceAll("\\$\\{intent\\}", "Synchronous executed commands on a node selection") + "* @generated by "
                + getClass().getName() + "\r\n ";
    }

    /**
     * Mutate type to async result.
     *
     * @return
     */
    Predicate<MethodDeclaration> methodFilter() {

        return method -> {

            ClassOrInterfaceDeclaration classOfMethod = (ClassOrInterfaceDeclaration) method.getParentNode().orElse(null);
            if (FILTER_METHODS.contains(method.getName().getIdentifier())
                    || FILTER_METHODS.contains(classOfMethod.getName().getIdentifier() + "." + method.getName())) {
                return false;
            }

            return true;
        };
    }

    /**
     * Mutate type to async result.
     *
     * @return
     */
    Function<MethodDeclaration, Type> methodTypeMutator() {

        return method -> {
            return CompilationUnitFactory.createParametrizedType("Executions", method.getType().toString());
        };
    }

    /**
     * Supply additional imports.
     *
     * @return
     */
    Supplier<List<String>> importSupplier() {
        return Collections::emptyList;
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void createInterface(String argument) throws Exception {
        createFactory(argument).createInterface();
    }

    static List<String> arguments() {
        return Arrays.asList(Constants.TEMPLATE_NAMES);
    }

    private CompilationUnitFactory createFactory(String templateName) {
        String targetName = templateName.replace("Redis", "NodeSelection");
        File templateFile = new File(Constants.TEMPLATES, "io/lettuce/core/api/" + templateName + ".java");
        String targetPackage = "io.lettuce.core.cluster.api.sync";

        // todo: remove AutoCloseable from BaseNodeSelectionAsyncCommands
        CompilationUnitFactory factory = new CompilationUnitFactory(templateFile, Constants.SOURCES, targetPackage, targetName,
                commentMutator(), methodTypeMutator(), methodFilter(), importSupplier(), null, Function.identity());
        factory.keepMethodSignaturesFor(FILTER_METHODS);
        return factory;
    }
}
