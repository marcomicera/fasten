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

import org.opalj.br.Method;
import scala.Function0;
import scala.collection.Iterable;
import scala.collection.Map;
import scala.runtime.AbstractFunction0;
import scala.runtime.AbstractFunction1;
import scala.runtime.AbstractFunction2;


final class JavaToScalaConverter {

    public static Function0<Iterable<Method>> asScalaFunction0(Iterable<Method> entryPoints) {
        return new AbstractFunction0<>() {

            @Override
            public Iterable<Method> apply() {
                return entryPoints;
            }
        };
    }

    public static AbstractFunction1 asScalaFunction1(ScalaFunction1 findEntryPoints) {
        return new AbstractFunction1() {

            @Override
            public Object apply(Object v1) {
                return findEntryPoints.execute(v1);
            }
        };
    }

    static AbstractFunction2<Method, Map<Object, Iterable<Method>>, Object> asScalaFunction2(ScalaFunction2 javaFunction) {
        return new AbstractFunction2<>() {

            @Override
            public Object apply(Method v1, Map<Object, Iterable<Method>> v2) {
                return javaFunction.execute(v1, v2);
            }
        };
    }

}
