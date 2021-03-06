/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.platform.base.component;

import org.gradle.api.Action;
import org.gradle.api.Incubating;
import org.gradle.internal.Actions;
import org.gradle.internal.BiAction;
import org.gradle.internal.Cast;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.reflect.ObjectInstantiationException;
import org.gradle.language.base.FunctionalSourceSet;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.model.ModelMap;
import org.gradle.model.collection.internal.ModelMapModelProjection;
import org.gradle.model.internal.core.*;
import org.gradle.model.internal.core.rule.describe.NestedModelRuleDescriptor;
import org.gradle.model.internal.core.rule.describe.SimpleModelRuleDescriptor;
import org.gradle.model.internal.type.ModelType;
import org.gradle.model.internal.type.ModelTypes;
import org.gradle.platform.base.*;
import org.gradle.platform.base.internal.BinarySpecFactory;
import org.gradle.platform.base.internal.BinarySpecInternal;
import org.gradle.platform.base.internal.ComponentSpecInternal;

import java.util.Collections;
import java.util.Set;

/**
 * Base class for custom component implementations. A custom implementation of {@link ComponentSpec} must extend this type.
 */
@Incubating
public abstract class BaseComponentSpec implements ComponentSpecInternal {

    public static final BiAction<MutableModelNode, BinarySpec> CREATE_BINARY_SOURCE_SET = new BiAction<MutableModelNode, BinarySpec>() {
        @Override
        public void execute(MutableModelNode modelNode, BinarySpec binarySpec) {
            BinarySpecInternal binarySpecInternal = Cast.uncheckedCast(binarySpec);
            ComponentSpec componentSpec = modelNode.getParent().getParent().getPrivateData(ModelType.of(ComponentSpec.class));
            ComponentSpecInternal componentSpecInternal = Cast.uncheckedCast(componentSpec);
            FunctionalSourceSet componentSources = componentSpecInternal.getSources();
            FunctionalSourceSet binarySources = componentSources.copy(binarySpec.getName());
            binarySpecInternal.setBinarySources(binarySources);
        }
    };

    private static ThreadLocal<ComponentInfo> nextComponentInfo = new ThreadLocal<ComponentInfo>();
    private final FunctionalSourceSet mainSourceSet;
    private final ModelMap<LanguageSourceSet> source;

    private final ComponentSpecIdentifier identifier;
    private final String typeName;
    private final MutableModelNode binaries;

    public static <T extends BaseComponentSpec> T create(Class<T> type, ComponentSpecIdentifier identifier, MutableModelNode modelNode, FunctionalSourceSet mainSourceSet, Instantiator instantiator) {
        if (type.equals(BaseComponentSpec.class)) {
            throw new ModelInstantiationException("Cannot create instance of abstract class BaseComponentSpec.");
        }
        nextComponentInfo.set(new ComponentInfo(identifier, modelNode, type.getSimpleName(), mainSourceSet, instantiator));
        try {
            try {
                return instantiator.newInstance(type);
            } catch (ObjectInstantiationException e) {
                throw new ModelInstantiationException(String.format("Could not create component of type %s", type.getSimpleName()), e.getCause());
            }
        } finally {
            nextComponentInfo.set(null);
        }
    }

    protected BaseComponentSpec() {
        this(nextComponentInfo.get());
    }

    private BaseComponentSpec(ComponentInfo info) {
        if (info == null) {
            throw new ModelInstantiationException("Direct instantiation of a BaseComponentSpec is not permitted. Use a ComponentTypeBuilder instead.");
        }

        this.identifier = info.componentIdentifier;
        this.typeName = info.typeName;
        this.mainSourceSet = info.sourceSets;
        this.source = ModelMapGroovyDecorator.unmanaged(
            NamedDomainObjectSetBackedModelMap.wrap(
                LanguageSourceSet.class,
                mainSourceSet,
                mainSourceSet.getEntityInstantiator(),
                Actions.add(mainSourceSet)
            )
        );

        MutableModelNode modelNode = info.modelNode;
        modelNode.addLink(
            ModelCreators.of(
                modelNode.getPath().child("binaries"), Actions.doNothing())
                .descriptor(modelNode.getDescriptor(), ".binaries")
                .withProjection(
                    ModelMapModelProjection.unmanaged(
                        BinarySpec.class,
                        NodeBackedModelMap.createUsingFactory(ModelReference.of(BinarySpecFactory.class))
                    )
                )
                .build()
        );
        binaries = modelNode.getLink("binaries");
        assert binaries != null;
        binaries.applyToAllLinks(ModelActionRole.Defaults, DirectNodeNoInputsModelAction.of(
            ModelReference.of(BinarySpec.class),
            new NestedModelRuleDescriptor(modelNode.getDescriptor(), ".binaries"),
            CREATE_BINARY_SOURCE_SET
        ));
    }

    public String getName() {
        return identifier.getName();
    }

    public String getProjectPath() {
        return identifier.getProjectPath();
    }

    protected String getTypeName() {
        return typeName;
    }

    public String getDisplayName() {
        return String.format("%s '%s'", getTypeName(), getName());
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public ModelMap<LanguageSourceSet> getSource() {
        return source;
    }

    @Override
    public void sources(Action<? super ModelMap<LanguageSourceSet>> action) {
        action.execute(source);
    }

    @Override
    public ModelMap<BinarySpec> getBinaries() {
        return binaries.asWritable(
            ModelTypes.modelMap(BinarySpec.class),
            new SimpleModelRuleDescriptor(identifier.toString() + ".getBinaries()"),
            Collections.<ModelView<?>>emptyList()
        ).getInstance();
    }

    @Override
    public void binaries(Action<? super ModelMap<BinarySpec>> action) {
        action.execute(getBinaries());
    }

    public FunctionalSourceSet getSources() {
        return mainSourceSet;
    }

    public Set<Class<? extends TransformationFileType>> getInputTypes() {
        return Collections.emptySet();
    }

    private static class ComponentInfo {
        final ComponentSpecIdentifier componentIdentifier;
        private final MutableModelNode modelNode;
        final String typeName;
        final FunctionalSourceSet sourceSets;
        final Instantiator instantiator;

        private ComponentInfo(
            ComponentSpecIdentifier componentIdentifier,
            MutableModelNode modelNode,
            String typeName,
            FunctionalSourceSet sourceSets,
            Instantiator instantiator
        ) {
            this.componentIdentifier = componentIdentifier;
            this.modelNode = modelNode;
            this.typeName = typeName;
            this.sourceSets = sourceSets;
            this.instantiator = instantiator;
        }
    }

}
