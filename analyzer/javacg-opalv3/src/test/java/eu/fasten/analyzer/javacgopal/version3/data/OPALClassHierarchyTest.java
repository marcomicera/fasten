/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fasten.analyzer.javacgopal.version3.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.fasten.analyzer.javacgopal.version3.ExtendedRevisionCallGraphV3;
import eu.fasten.core.data.FastenURI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opalj.br.BaseType;
import org.opalj.br.ClassFile;
import org.opalj.br.ClassHierarchy;
import org.opalj.br.Code;
import org.opalj.br.DeclaredMethod;
import org.opalj.br.FieldType;
import org.opalj.br.Method;
import org.opalj.br.MethodDescriptor;
import org.opalj.br.ObjectType;
import org.opalj.br.ReferenceType;
import org.opalj.br.instructions.Instruction;
import org.opalj.br.instructions.MethodInvocationInstruction;
import org.opalj.collection.QualifiedCollection;
import org.opalj.collection.immutable.Chain;
import org.opalj.collection.immutable.RefArray;
import org.opalj.collection.immutable.UIDSet;
import org.opalj.collection.immutable.UIDSet1;
import scala.Option;

class OPALClassHierarchyTest {

    @Test
    void asURIHierarchy() {
        var wrapperType = Mockito.mock(ObjectType.class);
        Mockito.when(wrapperType.packageName()).thenReturn("some/package");

        var baseType = Mockito.mock(BaseType.class);
        Mockito.when(baseType.WrapperType()).thenReturn(wrapperType);
        Mockito.when(baseType.toString()).thenReturn("typeName");

        var type = Mockito.mock(ObjectType.class);
        Mockito.when(type.asBaseType()).thenReturn(baseType);
        Mockito.when(type.isBaseType()).thenReturn(true);

        var qualifiedCollection = Mockito.mock(QualifiedCollection.class);
        Mockito.when(qualifiedCollection.s()).thenReturn(null);

        var uidSet = Mockito.mock(UIDSet.class);
        Mockito.when(uidSet.nonEmpty()).thenReturn(false);

        var uidSetInterfaces = new UIDSet1<>(type);

        var classHierarchy = Mockito.mock(ClassHierarchy.class);
        Mockito.when(classHierarchy.supertypes(type)).thenReturn(uidSet);
        Mockito.when(classHierarchy.allSuperclassTypesInInitializationOrder(type))
                .thenReturn(qualifiedCollection);
        Mockito.when(classHierarchy.allSuperinterfacetypes(type, false))
                .thenReturn(uidSetInterfaces);

        var arrayOfParameters = new RefArray<FieldType>(new FieldType[]{type});

        var descriptor = Mockito.mock(MethodDescriptor.class);
        Mockito.when(descriptor.parameterTypes()).thenReturn(arrayOfParameters);
        Mockito.when(descriptor.returnType()).thenReturn(type);

        var declaredMethod = Mockito.mock(DeclaredMethod.class);
        Mockito.when(declaredMethod.descriptor()).thenReturn(descriptor);
        Mockito.when(declaredMethod.name()).thenReturn("methodName");
        Mockito.when(declaredMethod.declaringClassType()).thenReturn(type);

        var method = createMethod();
        Mockito.when(method.isPrivate()).thenReturn(true);

        var methodsInternal = new HashMap<Method, Integer>();
        methodsInternal.put(method, 123);

        var opalTypeInternal = new OPALType(methodsInternal, null, new ArrayList<>(), "source.java");

        var internals = Map.of(type, opalTypeInternal);
        var externals = Map.of(type, Map.of(declaredMethod, 4));

        var opalClassHierarchy = new OPALClassHierarchy(internals, externals, 5);

        var uriHierarchy = opalClassHierarchy.asURIHierarchy(classHierarchy);

        assertNotNull(uriHierarchy);
        assertEquals(1, uriHierarchy.get(ExtendedRevisionCallGraphV3.Scope.internalTypes).size());
        assertEquals(1, uriHierarchy.get(ExtendedRevisionCallGraphV3.Scope.externalTypes).size());

        var internalUri = uriHierarchy
                .get(ExtendedRevisionCallGraphV3.Scope.internalTypes)
                .get(FastenURI.create("/some.package/typeName"));
        var externalUri = uriHierarchy
                .get(ExtendedRevisionCallGraphV3.Scope.externalTypes)
                .get(FastenURI.create("/some.package/typeName"));

        assertEquals("source.java", internalUri.getSourceFileName());
        assertEquals(FastenURI.create("/some.package/typeName.methodName(typeName)typeName"),
                internalUri.getMethods().get(123).getUri());
        assertEquals(0, internalUri.getSuperInterfaces().size());
        assertEquals(0, internalUri.getSuperClasses().size());

        assertEquals("", externalUri.getSourceFileName());
        assertEquals(FastenURI.create("/some.package/typeName.methodName(typeName)typeName"),
                externalUri.getMethods().get(4).getUri());
        assertEquals(1, externalUri.getSuperInterfaces().size());
        assertEquals(FastenURI.create("/some.package/typeName"),
                externalUri.getSuperInterfaces().get(0));
        assertEquals(0, externalUri.getSuperClasses().size());
    }

    private Method createMethod() {
        var wrapperType = Mockito.mock(ObjectType.class);
        Mockito.when(wrapperType.packageName()).thenReturn("some/package");

        var baseType = Mockito.mock(BaseType.class);
        Mockito.when(baseType.WrapperType()).thenReturn(wrapperType);
        Mockito.when(baseType.toString()).thenReturn("typeName");

        var type = Mockito.mock(ObjectType.class);
        Mockito.when(type.asBaseType()).thenReturn(baseType);
        Mockito.when(type.isBaseType()).thenReturn(true);

        var arrayOfParameters = new RefArray<FieldType>(new FieldType[]{type});

        var descriptor = Mockito.mock(MethodDescriptor.class);
        Mockito.when(descriptor.parameterTypes()).thenReturn(arrayOfParameters);
        Mockito.when(descriptor.returnType()).thenReturn(type);

        var classFile = Mockito.mock(ClassFile.class);
        Mockito.when(classFile.thisType()).thenReturn(type);

        var code = Mockito.mock(Code.class);
        Mockito.when(code.firstLineNumber()).thenReturn(Option.apply(10));
        Mockito.when(code.lineNumber(20)).thenReturn(Option.apply(30));
        Mockito.when(code.codeSize()).thenReturn(20);

        var method = Mockito.mock(Method.class);
        Mockito.when(method.descriptor()).thenReturn(descriptor);
        Mockito.when(method.name()).thenReturn("methodName");
        Mockito.when(method.declaringClassFile()).thenReturn(classFile);
        Mockito.when(method.body()).thenReturn(Option.apply(code));
        Mockito.when(method.instructionsOption()).thenReturn(Option.apply(new Instruction[]{}));

        return method;
    }

    @Test
    void addMethodToExternalsMethodExists() {
        var objectType = Mockito.mock(ObjectType.class);

        var method = Mockito.mock(DeclaredMethod.class);
        Mockito.when(method.declaringClassType()).thenReturn(objectType);

        var externals = new HashMap<ObjectType, Map<DeclaredMethod, Integer>>();
        var externalCall = Map.of(method, 123);
        externals.put(objectType, externalCall);

        var classHierarchy = new OPALClassHierarchy(new HashMap<>(), externals, 5);

        assertEquals(123, classHierarchy.addMethodToExternals(method));
    }

    @Test
    void addMethodToExternalsNoMethod() {
        var objectType = Mockito.mock(ObjectType.class);

        var method = Mockito.mock(DeclaredMethod.class);
        Mockito.when(method.declaringClassType()).thenReturn(objectType);

        var classHierarchy = new OPALClassHierarchy(new HashMap<>(), new HashMap<>(), 5);

        assertEquals(5, classHierarchy.addMethodToExternals(method));
    }

    @Test
    void getInternalCallKeys() {
        var objectType = Mockito.mock(ObjectType.class);

        var thisType = Mockito.mock(ObjectType.class);
        Mockito.when(thisType.asObjectType()).thenReturn(objectType);

        var classFile = Mockito.mock(ClassFile.class);
        Mockito.when(classFile.thisType()).thenReturn(thisType);

        var source = Mockito.mock(Method.class);
        Mockito.when(source.declaringClassFile()).thenReturn(classFile);
        var target = Mockito.mock(Method.class);
        Mockito.when(target.declaringClassFile()).thenReturn(classFile);

        var methods = new HashMap<Method, Integer>();
        methods.put(source, 123);
        methods.put(target, 234);

        var type = new OPALType(methods, Chain.empty(), new ArrayList<>(), "source.java");

        var internal = new HashMap<ObjectType, OPALType>();
        internal.put(objectType, type);

        var classHierarchy = new OPALClassHierarchy(internal, new HashMap<>(), 5);

        var internalKeys = classHierarchy.getInternalCallKeys(source, target);

        assertEquals(2, internalKeys.size());
        assertEquals(123, internalKeys.get(0));
        assertEquals(234, internalKeys.get(1));
    }

    @Test
    void getExternalCallKeysSourceMethodTargetDeclaredMethod() {
        var objectType = Mockito.mock(ObjectType.class);

        var thisType = Mockito.mock(ObjectType.class);
        Mockito.when(thisType.asObjectType()).thenReturn(objectType);

        var classFile = Mockito.mock(ClassFile.class);
        Mockito.when(classFile.thisType()).thenReturn(thisType);

        var source = Mockito.mock(Method.class);
        Mockito.when(source.declaringClassFile()).thenReturn(classFile);
        var target = Mockito.mock(DeclaredMethod.class);

        var methods = new HashMap<Method, Integer>();
        methods.put(source, 123);

        var type = new OPALType(methods, Chain.empty(), new ArrayList<>(), "source.java");

        var internal = new HashMap<ObjectType, OPALType>();
        internal.put(objectType, type);

        var classHierarchy = new OPALClassHierarchy(internal, new HashMap<>(), 5);

        var externalKeys = classHierarchy.getExternalCallKeys(source, target);

        assertEquals(2, externalKeys.size());
        assertEquals(123, externalKeys.get(0));
        assertEquals(5, externalKeys.get(1));
    }

    @Test
    void getExternalCallKeysSourceDeclaredMethodTargetMethod() {
        var objectType = Mockito.mock(ObjectType.class);

        var thisType = Mockito.mock(ObjectType.class);
        Mockito.when(thisType.asObjectType()).thenReturn(objectType);

        var classFile = Mockito.mock(ClassFile.class);
        Mockito.when(classFile.thisType()).thenReturn(thisType);

        var source = Mockito.mock(DeclaredMethod.class);
        var target = Mockito.mock(Method.class);
        Mockito.when(target.declaringClassFile()).thenReturn(classFile);

        var methods = new HashMap<Method, Integer>();
        methods.put(target, 123);

        var type = new OPALType(methods, Chain.empty(), new ArrayList<>(), "source.java");

        var internal = new HashMap<ObjectType, OPALType>();
        internal.put(objectType, type);

        var classHierarchy = new OPALClassHierarchy(internal, new HashMap<>(), 5);

        var externalKeys = classHierarchy.getExternalCallKeys(source, target);

        assertEquals(2, externalKeys.size());
        assertEquals(5, externalKeys.get(0));
        assertEquals(123, externalKeys.get(1));
    }

    @Test
    void getExternalCallKeysSourceDeclaredMethodTargetDeclaredMethod() {
        var source = Mockito.mock(DeclaredMethod.class);
        var target = Mockito.mock(DeclaredMethod.class);

        var classHierarchy = new OPALClassHierarchy(new HashMap<>(), new HashMap<>(), 5);

        var externalKeys = classHierarchy.getExternalCallKeys(source, target);

        assertEquals(2, externalKeys.size());
        assertEquals(5, externalKeys.get(0));
        assertEquals(6, externalKeys.get(1));
    }

    @Test
    void getExternalCallKeysWrongTypes() {
        var classHierarchy = new OPALClassHierarchy(new HashMap<>(), new HashMap<>(), 5);

        assertEquals(0, classHierarchy.getExternalCallKeys(new Object(), new Object()).size());
    }

    @Test
    void getExternalCallKeysSourceMethodTargetWrongType() {
        var source = Mockito.mock(Method.class);

        var classHierarchy = new OPALClassHierarchy(new HashMap<>(), new HashMap<>(), 5);

        assertEquals(0, classHierarchy.getExternalCallKeys(source, new Object()).size());
    }

    @Test
    void putCallsSourceMethod() {
        var objectType = Mockito.mock(ObjectType.class);

        var thisType = Mockito.mock(ObjectType.class);
        Mockito.when(thisType.asObjectType()).thenReturn(objectType);

        var classFile = Mockito.mock(ClassFile.class);
        Mockito.when(classFile.thisType()).thenReturn(thisType);

        var source = Mockito.mock(Method.class);
        Mockito.when(source.declaringClassFile()).thenReturn(classFile);
        var target = Mockito.mock(Method.class);
        Mockito.when(target.declaringClassFile()).thenReturn(classFile);

        var methods = new HashMap<Method, Integer>();
        methods.put(source, 123);
        methods.put(target, 234);

        var type = new OPALType(methods, Chain.empty(), new ArrayList<>(), "source.java");

        var internal = new HashMap<ObjectType, OPALType>();
        internal.put(objectType, type);

        var classHierarchy = new OPALClassHierarchy(internal, new HashMap<>(), 5);

        var internalCalls = new HashMap<List<Integer>, Map<Object, Object>>();
        var internalCallKeys = classHierarchy.getInternalCallKeys(source, target);
        internalCalls.put(internalCallKeys, new HashMap<>());

        var externalCalls = new HashMap<List<Integer>, Map<Object, Object>>();

        var newMetadata = new HashMap<>();
        newMetadata.put(10, "newMetadata");

        assertEquals(0, internalCalls.get(internalCallKeys).size());

        classHierarchy.putCalls(source, internalCalls, externalCalls, null, newMetadata, target);

        assertEquals(1, internalCalls.get(internalCallKeys).size());
        assertEquals("newMetadata", internalCalls.get(internalCallKeys).get(10));
    }

    @Test
    void putCallsSourceDeclaredMethod() {
        var objectType = Mockito.mock(ObjectType.class);

        var thisType = Mockito.mock(ObjectType.class);
        Mockito.when(thisType.asObjectType()).thenReturn(objectType);

        var classFile = Mockito.mock(ClassFile.class);
        Mockito.when(classFile.thisType()).thenReturn(thisType);

        var source = Mockito.mock(DeclaredMethod.class);
        var target = Mockito.mock(Method.class);
        Mockito.when(target.declaringClassFile()).thenReturn(classFile);

        var type = new OPALType(new HashMap<>(), Chain.empty(), new ArrayList<>(), "source.java");

        var internal = new HashMap<ObjectType, OPALType>();
        internal.put(objectType, type);

        var classHierarchy = new OPALClassHierarchy(internal, new HashMap<>(), 5);

        var externalCalls = new HashMap<List<Integer>, Map<Object, Object>>();
        externalCalls.put(List.of(5, 6), new HashMap<>());

        var internalCalls = new HashMap<List<Integer>, Map<Object, Object>>();

        var newMetadata = new HashMap<>();
        newMetadata.put(10, "newMetadata");

        assertEquals(0, externalCalls.get(List.of(5, 6)).size());

        classHierarchy.putCalls(source, internalCalls, externalCalls,
                Mockito.mock(DeclaredMethod.class), newMetadata, target);

        assertEquals(1, externalCalls.get(List.of(5, 6)).size());
        assertEquals("newMetadata", externalCalls.get(List.of(5, 6)).get(10));
    }

    @Test
    void putCallsTargetConstructor() {
        var objectType = Mockito.mock(ObjectType.class);

        var thisType = Mockito.mock(ObjectType.class);
        Mockito.when(thisType.asObjectType()).thenReturn(objectType);

        var classFile = Mockito.mock(ClassFile.class);
        Mockito.when(classFile.thisType()).thenReturn(thisType);

        var source = Mockito.mock(DeclaredMethod.class);
        var target = Mockito.mock(Method.class);
        Mockito.when(target.declaringClassFile()).thenReturn(classFile);
        Mockito.when(target.isConstructor()).thenReturn(true);

        var methods = new HashMap<Method, Integer>();
        methods.put(target, 6);

        var type = new OPALType(methods, Chain.empty(), new ArrayList<>(), "source.java");

        var internal = new HashMap<ObjectType, OPALType>();
        internal.put(objectType, type);

        var classHierarchy = new OPALClassHierarchy(internal, new HashMap<>(), 5);

        var externalCalls = new HashMap<List<Integer>, Map<Object, Object>>();
        externalCalls.put(List.of(5, 6), new HashMap<>());

        var internalCalls = new HashMap<List<Integer>, Map<Object, Object>>();

        var newMetadata = new HashMap<>();
        newMetadata.put(10, "newMetadata");

        assertEquals(0, externalCalls.get(List.of(5, 6)).size());

        classHierarchy.putCalls(source, internalCalls, externalCalls,
                Mockito.mock(DeclaredMethod.class), newMetadata, target);

        assertEquals(2, externalCalls.size());
        assertEquals("newMetadata", externalCalls.get(List.of(5, 6)).get(10));
        assertEquals(0, externalCalls.get(List.of(6, 6)).size());
    }

    @Test
    void putExternalCall() {
        var source = Mockito.mock(DeclaredMethod.class);
        var target = Mockito.mock(DeclaredMethod.class);

        var classHierarchy = new OPALClassHierarchy(new HashMap<>(), new HashMap<>(), 5);

        assertEquals(0, classHierarchy.getExternalCHA().size());

        var externalKeys = classHierarchy.getExternalCallKeys(source, target);
        var calls = new HashMap<List<Integer>, Map<Object, Object>>();
        classHierarchy.putExternalCall(source, calls, target, new HashMap<>());

        assertEquals(1, classHierarchy.getExternalCHA().size());
        assertNotNull(calls.get(externalKeys));
    }

    @Test
    void getInternalMetadata() {
        var callKeys = new ArrayList<Integer>();
        callKeys.add(1);
        callKeys.add(2);

        var internalMetadata = new HashMap<>();

        var internalCalls = new HashMap<List<Integer>, Map<Object, Object>>();
        internalCalls.put(callKeys, internalMetadata);

        var metadata = new HashMap<>();
        metadata.put(123, "testMetadata");

        var classHierarchy = new OPALClassHierarchy(new HashMap<>(), new HashMap<>(), 5);

        assertEquals(0, internalMetadata.size());

        var internalMetadataUpdated = classHierarchy.getInternalMetadata(internalCalls, metadata, callKeys);

        assertEquals(1, internalMetadata.size());
        assertEquals(internalMetadata, internalMetadataUpdated);

        assertEquals("testMetadata", internalMetadata.get(123));
    }

    @Test
    void appendGraph() {
    }

    @Test
    void getSubGraph() {
    }

    @Test
    void getCallSite() {
        var wrapperType = Mockito.mock(ObjectType.class);
        Mockito.when(wrapperType.packageName()).thenReturn("some/package");

        var baseType = Mockito.mock(BaseType.class);
        Mockito.when(baseType.WrapperType()).thenReturn(wrapperType);
        Mockito.when(baseType.toString()).thenReturn("typeName");

        var type = Mockito.mock(ReferenceType.class);
        Mockito.when(type.asBaseType()).thenReturn(baseType);
        Mockito.when(type.isBaseType()).thenReturn(true);

        var methodInvocationInstruction = Mockito.mock(MethodInvocationInstruction.class);
        Mockito.when(methodInvocationInstruction.declaringClass()).thenReturn(type);

        var instruction = Mockito.mock(Instruction.class);
        Mockito.when(instruction.mnemonic()).thenReturn("testType");
        Mockito.when(instruction.asMethodInvocationInstruction()).thenReturn(methodInvocationInstruction);

        var code = Mockito.mock(Code.class);
        Mockito.when(code.lineNumber(0)).thenReturn(Option.apply(30));

        var source = Mockito.mock(Method.class);
        Mockito.when(source.instructionsOption())
                .thenReturn(Option.apply(new Instruction[]{instruction}));
        Mockito.when(source.body()).thenReturn(Option.apply(code));

        var classHierarchy = new OPALClassHierarchy(new HashMap<>(), new HashMap<>(), 5);
        var callSite = classHierarchy.getCallSite(source, 0);

        assertNotNull(callSite);

        assertEquals(30, ((OPALCallSite) callSite.get("0")).getLine());
        assertEquals("/some.package/typeName", ((OPALCallSite) callSite.get("0")).getReceiver());
        assertEquals("testType", ((OPALCallSite) callSite.get("0")).getType());
    }
}